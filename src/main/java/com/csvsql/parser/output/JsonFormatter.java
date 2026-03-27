package com.csvsql.parser.output;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.*;

/**
 * Formatter for JSON (JavaScript Object Notation) output.
 *
 * <p>JsonFormatter produces structured JSON output that includes:</p>
 * <ul>
 *   <li>Column names array</li>
 *   <li>Row data as array of objects</li>
 *   <li>Row count metadata</li>
 *   <li>Execution time in milliseconds</li>
 * </ul>
 *
 * <p>Example output:</p>
 * <pre>
 * {
 *   "columns" : [ "id", "name", "salary" ],
 *   "rows" : [ {
 *     "id" : 1,
 *     "name" : "Alice",
 *     "salary" : 50000
 *   }, {
 *     "id" : 2,
 *     "name" : "Bob",
 *     "salary" : 60000
 *   } ],
 *   "rowCount" : 2,
 *   "executionTimeMs" : 15
 * }
 * </pre>
 *
 * @see OutputFormatter
 * @see ResultSet
 */
public class JsonFormatter implements OutputFormatter {

    private final ObjectMapper objectMapper;
    private boolean prettyPrint = true;

    public JsonFormatter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public JsonFormatter setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
        if (prettyPrint) {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        } else {
            objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        }
        return this;
    }

    @Override
    public String format(ResultSet resultSet) {
        Map<String, Object> output = new LinkedHashMap<>();

        output.put("columns", resultSet.getColumnNames());

        List<Map<String, Object>> formattedRows = new ArrayList<>();
        for (Map<String, Object> row : resultSet.getRows()) {
            Map<String, Object> formattedRow = new LinkedHashMap<>();
            for (String col : resultSet.getColumnNames()) {
                Object value = row.get(col);
                formattedRow.put(col, formatValue(value));
            }
            formattedRows.add(formattedRow);
        }
        output.put("rows", formattedRows);

        output.put("rowCount", resultSet.getRowCount());
        output.put("executionTimeMs", resultSet.getExecutionTimeMs());

        try {
            return objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to format JSON: " + e.getMessage() + "\"}";
        }
    }

    private Object formatValue(Object value) {
        if (value == null) {
            return null;
        }
        // Let Jackson handle the serialization
        return value;
    }

    @Override
    public String getName() {
        return "json";
    }
}