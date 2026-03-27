package com.csvsql.parser.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Loader for CSV files with automatic type inference, chunked loading, and encoding detection.
 *
 * <p>CsvLoader is responsible for reading CSV files and converting them into
 * {@link CsvTable} objects. It provides the following features:</p>
 * <ul>
 *   <li>Automatic encoding detection (UTF-8, GBK, GB2312, ISO-8859-1)</li>
 *   <li>Automatic delimiter detection (comma, semicolon, tab, pipe)</li>
 *   <li>Automatic type inference for columns (INTEGER, LONG, DOUBLE, DATE, BOOLEAN, STRING)</li>
 *   <li>Chunked loading for large files (&gt;50MB)</li>
 *   <li>Configurable sample size for type inference</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * CsvLoader loader = new CsvLoader()
 *     .setDelimiter(';')
 *     .setHasHeader(true)
 *     .setAutoDetectEncoding(true);
 *
 * CsvTable table = loader.load("data.csv");
 * </pre>
 *
 * @see CsvTable
 * @see TypeInferer
 * @see TableRegistry
 */
public class CsvLoader {

    private static final Logger logger = LoggerFactory.getLogger(CsvLoader.class);

    // Common encodings to try for detection
    private static final Charset[] DETECTABLE_ENCODINGS = {
        StandardCharsets.UTF_8,
        Charset.forName("GBK"),
        Charset.forName("GB2312"),
        StandardCharsets.ISO_8859_1,
        StandardCharsets.US_ASCII
    };

    private final TypeInferer typeInferer;
    private char delimiter = ',';
    private Charset encoding = StandardCharsets.UTF_8;
    private boolean hasHeader = true;
    private int sampleSize = 1000; // Number of rows to sample for type inference
    private int chunkSize = 10000; // Number of rows per chunk for large files
    private boolean autoDetectEncoding = true;
    private boolean useChunkedLoading = false;

    /**
     * Creates a new CsvLoader with default settings.
     *
     * <p>Default settings:</p>
     * <ul>
     *   <li>Delimiter: comma (',')</li>
     *   <li>Encoding: UTF-8 (with auto-detection enabled)</li>
     *   <li>Has header: true</li>
     *   <li>Sample size: 1000 rows</li>
     *   <li>Chunk size: 10000 rows</li>
     * </ul>
     */
    public CsvLoader() {
        this.typeInferer = new TypeInferer();
    }

    /**
     * Sets the delimiter character for CSV parsing.
     *
     * @param delimiter the delimiter character (e.g., ',', ';', '\t')
     * @return this loader instance for method chaining
     */
    public CsvLoader setDelimiter(char delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    /**
     * Sets the character encoding for reading the CSV file.
     *
     * <p>Calling this method disables automatic encoding detection.</p>
     *
     * @param encoding the character encoding to use
     * @return this loader instance for method chaining
     */
    public CsvLoader setEncoding(Charset encoding) {
        this.encoding = encoding;
        this.autoDetectEncoding = false;
        return this;
    }

    /**
     * Sets whether the CSV file has a header row.
     *
     * @param hasHeader true if the first row contains column names
     * @return this loader instance for method chaining
     */
    public CsvLoader setHasHeader(boolean hasHeader) {
        this.hasHeader = hasHeader;
        return this;
    }

    /**
     * Sets the number of rows to sample for type inference.
     *
     * <p>A larger sample size improves type inference accuracy but increases
     * initial loading time. Default is 1000 rows.</p>
     *
     * @param sampleSize the number of rows to sample
     * @return this loader instance for method chaining
     */
    public CsvLoader setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
        return this;
    }

    /**
     * Sets the chunk size for loading large files.
     *
     * <p>When chunked loading is enabled, the file is read in chunks of this
     * size to reduce memory usage. Default is 10000 rows per chunk.</p>
     *
     * @param chunkSize the number of rows per chunk
     * @return this loader instance for method chaining
     */
    public CsvLoader setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    /**
     * Enables or disables automatic encoding detection.
     *
     * <p>When enabled, the loader will attempt to detect the file's encoding
     * by checking for BOM markers and testing various encodings.</p>
     *
     * @param autoDetect true to enable automatic detection
     * @return this loader instance for method chaining
     */
    public CsvLoader setAutoDetectEncoding(boolean autoDetect) {
        this.autoDetectEncoding = autoDetect;
        return this;
    }

    /**
     * Enables or disables chunked loading for large files.
     *
     * <p>When enabled, files are loaded in chunks to reduce memory usage.
     * Chunked loading is automatically enabled for files larger than 50MB.</p>
     *
     * @param useChunked true to enable chunked loading
     * @return this loader instance for method chaining
     */
    public CsvLoader setUseChunkedLoading(boolean useChunked) {
        this.useChunkedLoading = useChunked;
        return this;
    }

    /**
     * Load a CSV file into a CsvTable.
     *
     * @param filePath the path to the CSV file
     * @return the loaded table
     * @throws IOException if the file cannot be read
     */
    public CsvTable load(String filePath) throws IOException {
        return load(filePath, null);
    }

    /**
     * Load a CSV file into a CsvTable with an optional alias.
     *
     * @param filePath the path to the CSV file
     * @param alias    an optional alias for the table
     * @return the loaded table
     * @throws IOException if the file cannot be read
     */
    public CsvTable load(String filePath, String alias) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        String tableName = alias != null ? alias : file.getName();
        logger.info("Loading CSV file: {} as {}", filePath, tableName);

        // Detect encoding if enabled
        Charset detectedEncoding = encoding;
        if (autoDetectEncoding) {
            detectedEncoding = detectEncoding(filePath);
            logger.info("Detected encoding: {} for file: {}", detectedEncoding.name(), filePath);
        }

        // Detect delimiter
        char detectedDelimiter = detectDelimiter(filePath, detectedEncoding);
        if (detectedDelimiter != ',') {
            logger.info("Detected delimiter: '{}' for file: {}", detectedDelimiter, filePath);
            delimiter = detectedDelimiter;
        }

        // Check file size for chunked loading decision
        long fileSize = file.length();
        boolean shouldUseChunks = useChunkedLoading || fileSize > 50 * 1024 * 1024; // 50MB threshold

        if (shouldUseChunks) {
            return loadChunked(file, tableName, detectedEncoding);
        } else {
            return loadStandard(file, tableName, detectedEncoding);
        }
    }

    /**
     * Standard loading for smaller files.
     */
    private CsvTable loadStandard(File file, String tableName, Charset encoding) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setDelimiter(delimiter)
            .setHeader()
            .setSkipHeaderRecord(hasHeader)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();

        try (Reader reader = new InputStreamReader(new FileInputStream(file), encoding);
             CSVParser parser = CSVParser.parse(reader, format)) {

            List<String> columnNames = new ArrayList<>(parser.getHeaderNames());
            if (!hasHeader) {
                columnNames.clear();
                int colCount = parser.getHeaderNames().size();
                for (int i = 0; i < colCount; i++) {
                    columnNames.add("col" + (i + 1));
                }
            }

            List<CSVRecord> records = parser.getRecords();
            logger.debug("Read {} records from {}", records.size(), file.getPath());

            // Collect values for type inference
            Map<String, List<String>> columnValues = new LinkedHashMap<>();
            for (String col : columnNames) {
                columnValues.put(col, new ArrayList<>());
            }

            int sampleCount = Math.min(records.size(), sampleSize);
            for (int i = 0; i < sampleCount; i++) {
                CSVRecord record = records.get(i);
                for (int j = 0; j < columnNames.size(); j++) {
                    String colName = columnNames.get(j);
                    String value = j < record.size() ? record.get(j) : null;
                    columnValues.get(colName).add(value);
                }
            }

            // Infer column types
            Map<String, ColumnType> columnTypes = new LinkedHashMap<>();
            for (String col : columnNames) {
                ColumnType type = typeInferer.inferType(columnValues.get(col));
                columnTypes.put(col, type);
                logger.debug("Column '{}' inferred as {}", col, type);
            }

            // Create table
            CsvTable table = new CsvTable(tableName, file.getPath(), columnNames, columnTypes);

            // Parse and add rows
            for (CSVRecord record : records) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int j = 0; j < columnNames.size(); j++) {
                    String colName = columnNames.get(j);
                    String value = j < record.size() ? record.get(j) : null;
                    ColumnType type = columnTypes.get(colName);
                    Object parsedValue = typeInferer.parseValue(value, type);
                    row.put(colName, parsedValue);
                }
                table.addRow(row);
            }

            logger.info("Loaded {} rows from {}", table.getRowCount(), file.getPath());
            return table;
        }
    }

    /**
     * Chunked loading for large files.
     */
    private CsvTable loadChunked(File file, String tableName, Charset encoding) throws IOException {
        logger.info("Using chunked loading for large file: {}", file.getPath());

        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setDelimiter(delimiter)
            .setHeader()
            .setSkipHeaderRecord(hasHeader)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();

        try (Reader reader = new InputStreamReader(new FileInputStream(file), encoding);
             CSVParser parser = CSVParser.parse(reader, format)) {

            List<String> columnNames = new ArrayList<>(parser.getHeaderNames());
            if (!hasHeader) {
                columnNames.clear();
                int colCount = parser.getHeaderNames().size();
                for (int i = 0; i < colCount; i++) {
                    columnNames.add("col" + (i + 1));
                }
            }

            // First pass: sample for type inference
            Map<String, List<String>> columnValues = new LinkedHashMap<>();
            for (String col : columnNames) {
                columnValues.put(col, new ArrayList<>());
            }

            Iterator<CSVRecord> iterator = parser.iterator();
            int sampleCount = 0;
            while (iterator.hasNext() && sampleCount < sampleSize) {
                CSVRecord record = iterator.next();
                for (int j = 0; j < columnNames.size(); j++) {
                    String colName = columnNames.get(j);
                    String value = j < record.size() ? record.get(j) : null;
                    columnValues.get(colName).add(value);
                }
                sampleCount++;
            }

            // Infer column types
            Map<String, ColumnType> columnTypes = new LinkedHashMap<>();
            for (String col : columnNames) {
                ColumnType type = typeInferer.inferType(columnValues.get(col));
                columnTypes.put(col, type);
            }

            // Create table
            CsvTable table = new CsvTable(tableName, file.getPath(), columnNames, columnTypes);

            // Reset and read all data in chunks
            try (Reader reader2 = new InputStreamReader(new FileInputStream(file), encoding);
                 CSVParser parser2 = CSVParser.parse(reader2, format)) {

                int chunkRowCount = 0;
                int totalRows = 0;

                for (CSVRecord record : parser2) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int j = 0; j < columnNames.size(); j++) {
                        String colName = columnNames.get(j);
                        String value = j < record.size() ? record.get(j) : null;
                        ColumnType type = columnTypes.get(colName);
                        Object parsedValue = typeInferer.parseValue(value, type);
                        row.put(colName, parsedValue);
                    }
                    table.addRow(row);
                    chunkRowCount++;
                    totalRows++;

                    if (chunkRowCount >= chunkSize) {
                        logger.debug("Loaded chunk: {} rows (total: {})", chunkRowCount, totalRows);
                        chunkRowCount = 0;
                    }
                }

                logger.info("Loaded {} rows from {} using chunked loading", table.getRowCount(), file.getPath());
            }

            return table;
        }
    }

    /**
     * Detect the encoding of a file.
     *
     * @param filePath the path to the file
     * @return the detected charset
     * @throws IOException if the file cannot be read
     */
    public Charset detectEncoding(String filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));

        // Check for BOM markers
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (bytes.length >= 2) {
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
                return StandardCharsets.UTF_16LE;
            }
            if ((bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
                return StandardCharsets.UTF_16BE;
            }
        }

        // Try each encoding and check for validity
        for (Charset charset : DETECTABLE_ENCODINGS) {
            try {
                String content = new String(bytes, charset);
                // Check if the content looks valid (no replacement characters)
                if (!content.contains("\uFFFD")) {
                    // For GBK/GB2312, check for Chinese characters
                    if (charset.name().startsWith("GB")) {
                        if (containsChinese(content)) {
                            return charset;
                        }
                        continue;
                    }
                    return charset;
                }
            } catch (Exception e) {
                // Try next encoding
            }
        }

        // Default to UTF-8
        return StandardCharsets.UTF_8;
    }

    /**
     * Check if a string contains Chinese characters.
     */
    private boolean containsChinese(String str) {
        for (char c : str.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detect the delimiter character from file content.
     *
     * @param filePath the path to the CSV file
     * @return the detected delimiter
     * @throws IOException if the file cannot be read
     */
    public char detectDelimiter(String filePath) throws IOException {
        return detectDelimiter(filePath, encoding);
    }

    /**
     * Detect the delimiter character from file content with specified encoding.
     */
    public char detectDelimiter(String filePath, Charset charset) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        String content = new String(bytes, charset);
        if (content.isEmpty()) {
            return ',';
        }

        int firstLineEnd = content.indexOf('\n');
        String firstLine = firstLineEnd > 0 ? content.substring(0, firstLineEnd) : content;

        char[] delimiters = {',', ';', '\t', '|'};
        int[] counts = new int[delimiters.length];

        for (int i = 0; i < delimiters.length; i++) {
            counts[i] = countChar(firstLine, delimiters[i]);
        }

        int maxIndex = 0;
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] > counts[maxIndex]) {
                maxIndex = i;
            }
        }

        return counts[maxIndex] > 0 ? delimiters[maxIndex] : ',';
    }

    private int countChar(String str, char c) {
        int count = 0;
        for (char ch : str.toCharArray()) {
            if (ch == c) {
                count++;
            }
        }
        return count;
    }
}