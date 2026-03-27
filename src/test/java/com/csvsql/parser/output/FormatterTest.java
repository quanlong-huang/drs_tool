package com.csvsql.parser.output;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for output formatters.
 */
class FormatterTest {

    private ResultSet resultSet;

    @BeforeEach
    void setUp() {
        List<String> columns = Arrays.asList("name", "age", "salary");

        List<Map<String, Object>> rows = new ArrayList<>();

        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("name", "Alice");
        row1.put("age", 30);
        row1.put("salary", 80000);
        rows.add(row1);

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("name", "Bob");
        row2.put("age", 25);
        row2.put("salary", 60000);
        rows.add(row2);

        resultSet = new ResultSet(columns, rows, 10);
    }

    @Test
    void testTableFormatter() {
        TableFormatter formatter = new TableFormatter();
        String output = formatter.format(resultSet);

        assertNotNull(output);
        assertTrue(output.contains("Alice"));
        assertTrue(output.contains("Bob"));
        assertTrue(output.contains("name"));
        assertTrue(output.contains("age"));
        assertTrue(output.contains("salary"));
        assertEquals("table", formatter.getName());
    }

    @Test
    void testCsvFormatter() {
        CsvFormatter formatter = new CsvFormatter();
        String output = formatter.format(resultSet);

        assertNotNull(output);
        assertTrue(output.contains("name,age,salary"));
        assertTrue(output.contains("Alice,30,80000"));
        assertTrue(output.contains("Bob,25,60000"));
        assertEquals("csv", formatter.getName());
    }

    @Test
    void testJsonFormatter() {
        JsonFormatter formatter = new JsonFormatter();
        String output = formatter.format(resultSet);

        assertNotNull(output);
        assertTrue(output.contains("\"name\" : \"Alice\""));
        assertTrue(output.contains("\"age\" : 30"));
        assertTrue(output.contains("\"rowCount\" : 2"));
        assertTrue(output.contains("\"executionTimeMs\" : 10"));
        assertEquals("json", formatter.getName());
    }

    @Test
    void testEmptyResultSet() {
        ResultSet emptyResult = new ResultSet(
            Arrays.asList("col1", "col2"),
            Collections.emptyList(),
            5
        );

        TableFormatter tableFormatter = new TableFormatter();
        String tableOutput = tableFormatter.format(emptyResult);
        assertNotNull(tableOutput);

        CsvFormatter csvFormatter = new CsvFormatter();
        String csvOutput = csvFormatter.format(emptyResult);
        assertTrue(csvOutput.contains("col1,col2"));

        JsonFormatter jsonFormatter = new JsonFormatter();
        String jsonOutput = jsonFormatter.format(emptyResult);
        assertTrue(jsonOutput.contains("\"rowCount\" : 0"));
    }

    @Test
    void testResultSetWithNulls() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", "Test");
        row.put("value", null);
        rows.add(row);

        ResultSet resultWithNull = new ResultSet(
            Arrays.asList("name", "value"),
            rows,
            1
        );

        CsvFormatter csvFormatter = new CsvFormatter();
        String csvOutput = csvFormatter.format(resultWithNull);
        assertTrue(csvOutput.contains("Test"));

        JsonFormatter jsonFormatter = new JsonFormatter();
        String jsonOutput = jsonFormatter.format(resultWithNull);
        assertTrue(jsonOutput.contains("Test"));
    }

    @Test
    void testResultSetGetters() {
        assertEquals(2, resultSet.getRowCount());
        assertEquals(3, resultSet.getColumnNames().size());
        assertEquals(10, resultSet.getExecutionTimeMs());
        assertEquals(2, resultSet.getRows().size());
    }

    @Test
    void testOutputFormatterInterface() {
        OutputFormatter tableFormatter = new TableFormatter();
        OutputFormatter csvFormatter = new CsvFormatter();
        OutputFormatter jsonFormatter = new JsonFormatter();

        assertNotNull(tableFormatter.format(resultSet));
        assertNotNull(csvFormatter.format(resultSet));
        assertNotNull(jsonFormatter.format(resultSet));
    }
}