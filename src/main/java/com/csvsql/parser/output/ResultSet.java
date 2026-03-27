package com.csvsql.parser.output;

import java.util.*;

/**
 * Represents the result of a SQL query execution.
 *
 * <p>ResultSet contains the query results including column names, row data,
 * and execution metadata such as row count and execution time. It is the
 * primary output of the {@link com.csvsql.parser.engine.QueryExecutor}.</p>
 *
 * <p>ResultSet instances are immutable after creation. The data is copied
 * defensively to prevent external modification.</p>
 *
 * @see com.csvsql.parser.engine.QueryExecutor
 * @see OutputFormatter
 */
public class ResultSet {

    private final List<String> columnNames;
    private final List<Map<String, Object>> rows;
    private final int rowCount;
    private final long executionTimeMs;
    private final Date executedAt;

    /**
     * Creates a new ResultSet with the specified data.
     *
     * @param columnNames     the list of column names in result order
     * @param rows            the list of rows, each row is a map of column names to values
     * @param executionTimeMs the query execution time in milliseconds
     */
    public ResultSet(List<String> columnNames, List<Map<String, Object>> rows, long executionTimeMs) {
        this.columnNames = new ArrayList<>(columnNames);
        this.rows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            this.rows.add(new LinkedHashMap<>(row));
        }
        this.rowCount = rows.size();
        this.executionTimeMs = executionTimeMs;
        this.executedAt = new Date();
    }

    /**
     * Returns an unmodifiable list of column names.
     *
     * @return the column names in result order
     */
    public List<String> getColumnNames() {
        return Collections.unmodifiableList(columnNames);
    }

    /**
     * Returns an unmodifiable list of rows.
     *
     * @return the rows, where each row is a map of column names to values
     */
    public List<Map<String, Object>> getRows() {
        return Collections.unmodifiableList(rows);
    }

    /**
     * Returns the number of rows in the result.
     *
     * @return the row count
     */
    public int getRowCount() {
        return rowCount;
    }

    /**
     * Returns the query execution time.
     *
     * @return the execution time in milliseconds
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    /**
     * Returns the timestamp when the query was executed.
     *
     * @return the execution timestamp
     */
    public Date getExecutedAt() {
        return executedAt;
    }

    /**
     * Returns a specific cell value by row index and column name.
     *
     * @param rowIndex    the zero-based row index
     * @param columnName the column name
     * @return the cell value, may be null
     * @throws IndexOutOfBoundsException if the row index is out of bounds
     */
    public Object getValue(int rowIndex, String columnName) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            throw new IndexOutOfBoundsException("Row index out of bounds: " + rowIndex);
        }
        return rows.get(rowIndex).get(columnName);
    }

    /**
     * Checks if the result set is empty.
     *
     * @return true if there are no rows, false otherwise
     */
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /**
     * Returns the number of columns in the result.
     *
     * @return the column count
     */
    public int getColumnCount() {
        return columnNames.size();
    }

    /**
     * Returns a string representation of the result set.
     *
     * @return a string containing column count, row count, and execution time
     */
    @Override
    public String toString() {
        return String.format("ResultSet[columns=%d, rows=%d, time=%dms]",
            columnNames.size(), rowCount, executionTimeMs);
    }
}