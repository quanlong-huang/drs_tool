package com.csvsql.parser.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CsvLoader.
 */
class CsvLoaderTest {

    private CsvLoader loader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new CsvLoader();
    }

    @Test
    void testLoadSimpleCsv() throws IOException {
        File csvFile = createCsvFile("test.csv",
            "name,age,salary\n" +
            "Alice,30,80000\n" +
            "Bob,25,60000\n");

        CsvTable table = loader.load(csvFile.getAbsolutePath());

        assertEquals("test.csv", table.getName());
        assertEquals(3, table.getColumnCount());
        assertEquals(2, table.getRowCount());

        List<String> columns = table.getColumnNames();
        assertTrue(columns.contains("name"));
        assertTrue(columns.contains("age"));
        assertTrue(columns.contains("salary"));
    }

    @Test
    void testLoadWithDifferentDelimiter() throws IOException {
        File csvFile = createCsvFile("test.csv",
            "name;age;salary\n" +
            "Alice;30;80000\n");

        loader.setDelimiter(';');
        CsvTable table = loader.load(csvFile.getAbsolutePath());

        assertEquals(3, table.getColumnCount());
        assertEquals(1, table.getRowCount());
    }

    @Test
    void testTypeInference() throws IOException {
        File csvFile = createCsvFile("test.csv",
            "name,age,salary,active\n" +
            "Alice,30,80000.50,true\n" +
            "Bob,25,60000.00,false\n");

        CsvTable table = loader.load(csvFile.getAbsolutePath());

        assertEquals(ColumnType.STRING, table.getColumnType("name"));
        assertEquals(ColumnType.INTEGER, table.getColumnType("age"));
        assertEquals(ColumnType.DOUBLE, table.getColumnType("salary"));
        assertEquals(ColumnType.BOOLEAN, table.getColumnType("active"));
    }

    @Test
    void testNullValues() throws IOException {
        File csvFile = createCsvFile("test.csv",
            "name,age\n" +
            "Alice,30\n" +
            "Bob,\n" +
            ",25\n");

        CsvTable table = loader.load(csvFile.getAbsolutePath());

        List<Map<String, Object>> rows = table.getRows();
        assertEquals(3, rows.size());

        // Second row: Bob with null age
        assertNull(rows.get(1).get("age"));

        // Third row: null name with age 25
        assertNull(rows.get(2).get("name"));
        assertEquals(25, rows.get(2).get("age"));
    }

    @Test
    void testNoHeader() throws IOException {
        File csvFile = createCsvFile("test.csv",
            "Alice,30,80000\n" +
            "Bob,25,60000\n");

        loader.setHasHeader(false);
        CsvTable table = loader.load(csvFile.getAbsolutePath());

        List<String> columns = table.getColumnNames();
        assertEquals("col1", columns.get(0));
        assertEquals("col2", columns.get(1));
        assertEquals("col3", columns.get(2));
    }

    @Test
    void testDetectDelimiter() throws IOException {
        File tabFile = createCsvFile("tab.txt",
            "name\tage\tsalary\n" +
            "Alice\t30\t80000\n");

        char delimiter = loader.detectDelimiter(tabFile.getAbsolutePath());
        assertEquals('\t', delimiter);

        File semicolonFile = createCsvFile("semi.txt",
            "name;age;salary\n" +
            "Alice;30;80000\n");

        delimiter = loader.detectDelimiter(semicolonFile.getAbsolutePath());
        assertEquals(';', delimiter);
    }

    @Test
    void testFileNotFound() {
        assertThrows(IOException.class, () -> {
            loader.load("/nonexistent/path/file.csv");
        });
    }

    @Test
    void testChunkedLoading() throws IOException {
        // Create a large CSV file
        StringBuilder sb = new StringBuilder("id,value\n");
        for (int i = 0; i < 1000; i++) {
            sb.append(i).append(",value").append(i).append("\n");
        }

        File csvFile = createCsvFile("large.csv", sb.toString());

        loader.setUseChunkedLoading(true).setChunkSize(100);
        CsvTable table = loader.load(csvFile.getAbsolutePath());

        assertEquals(1000, table.getRowCount());
    }

    private File createCsvFile(String name, String content) throws IOException {
        File file = tempDir.resolve(name).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }
}