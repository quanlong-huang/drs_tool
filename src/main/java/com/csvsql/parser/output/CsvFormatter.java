package com.csvsql.parser.output;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * Formatter for CSV (Comma-Separated Values) output.
 *
 * <p>CsvFormatter produces standard CSV output that can be imported into
 * spreadsheet applications or other tools. Features include:</p>
 * <ul>
 *   <li>Configurable delimiter character</li>
 *   <li>Optional header row</li>
 *   <li>Proper quoting and escaping of special characters</li>
 *   <li>NULL values represented as empty strings</li>
 * </ul>
 *
 * <p>Example output:</p>
 * <pre>
 * id,name,salary
 * 1,Alice,50000
 * 2,Bob,60000
 * </pre>
 *
 * @see OutputFormatter
 * @see ResultSet
 */
public class CsvFormatter implements OutputFormatter {

    private char delimiter = ',';
    private boolean includeHeader = true;

    public CsvFormatter() {
    }

    public CsvFormatter setDelimiter(char delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    public CsvFormatter setIncludeHeader(boolean includeHeader) {
        this.includeHeader = includeHeader;
        return this;
    }

    @Override
    public String format(ResultSet resultSet) {
        StringWriter writer = new StringWriter();

        try {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader(resultSet.getColumnNames().toArray(new String[0]))
                .setSkipHeaderRecord(!includeHeader)
                .build();

            CSVPrinter printer = new CSVPrinter(writer, format);

            for (Map<String, Object> row : resultSet.getRows()) {
                List<String> values = new java.util.ArrayList<>();
                for (String col : resultSet.getColumnNames()) {
                    Object value = row.get(col);
                    values.add(formatValue(value));
                }
                printer.printRecord(values);
            }

            printer.flush();
            printer.close();

        } catch (IOException e) {
            return "Error formatting CSV: " + e.getMessage();
        }

        return writer.toString();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    @Override
    public String getName() {
        return "csv";
    }
}