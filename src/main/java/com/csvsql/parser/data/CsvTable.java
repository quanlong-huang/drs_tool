package com.csvsql.parser.data;

import java.util.*;

/**
 * Represents a CSV table loaded into memory with typed columns.
 *
 * <p>CsvTable is the core data structure that holds CSV data after loading.
 * It maintains column names, column types (auto-inferred), and row data
 * as a list of key-value maps.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * CsvLoader loader = new CsvLoader();
 * CsvTable table = loader.load("employees.csv");
 * List&lt;Map&lt;String, Object&gt;&gt; rows = table.getRows();
 * </pre>
 *
 * @see CsvLoader
 * @see ColumnType
 * @see TableRegistry
 */
public class CsvTable {

    private final String name;
    private final String filePath;
    private final List<String> columnNames;
    private final Map<String, ColumnType> columnTypes;
    private final List<Map<String, Object>> rows;

    /**
     * Creates an empty CsvTable with the specified name.
     *
     * @param name     the table name
     * @param filePath the source file path
     */
    public CsvTable(String name, String filePath) {
        this.name = name;
        this.filePath = filePath;
        this.columnNames = new ArrayList<>();
        this.columnTypes = new LinkedHashMap<>();
        this.rows = new ArrayList<>();
    }

    /**
     * Creates a CsvTable with predefined columns and types.
     *
     * @param name        the table name
     * @param filePath    the source file path
     * @param columnNames the list of column names
     * @param columnTypes the map of column names to types
     */
    public CsvTable(String name, String filePath, List<String> columnNames, Map<String, ColumnType> columnTypes) {
        this.name = name;
        this.filePath = filePath;
        this.columnNames = new ArrayList<>(columnNames);
        this.columnTypes = new LinkedHashMap<>(columnTypes);
        this.rows = new ArrayList<>();
    }

    /**
     * Returns the table name.
     *
     * @return the table name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the source file path.
     *
     * @return the file path, or null if not loaded from a file
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Returns an unmodifiable list of column names.
     *
     * @return the list of column names in order
     */
    public List<String> getColumnNames() {
        return Collections.unmodifiableList(columnNames);
    }

    /**
     * Returns an unmodifiable map of column names to types.
     *
     * @return the map of column types
     */
    public Map<String, ColumnType> getColumnTypes() {
        return Collections.unmodifiableMap(columnTypes);
    }

    /**
     * Returns the type of a specific column.
     *
     * @param columnName the column name
     * @return the column type, or null if the column doesn't exist
     */
    public ColumnType getColumnType(String columnName) {
        return columnTypes.get(columnName);
    }

    /**
     * Returns an unmodifiable list of all rows.
     *
     * @return the list of rows, where each row is a map of column names to values
     */
    public List<Map<String, Object>> getRows() {
        return Collections.unmodifiableList(rows);
    }

    /**
     * Returns the number of rows in the table.
     *
     * @return the row count
     */
    public int getRowCount() {
        return rows.size();
    }

    /**
     * Returns the number of columns in the table.
     *
     * @return the column count
     */
    public int getColumnCount() {
        return columnNames.size();
    }

    /**
     * Adds a new column to the table.
     *
     * @param name the column name
     * @param type the column type
     */
    public void addColumn(String name, ColumnType type) {
        if (!columnNames.contains(name)) {
            columnNames.add(name);
            columnTypes.put(name, type);
        }
    }

    /**
     * Adds a row to the table.
     *
     * @param row the row data as a map of column names to values
     */
    public void addRow(Map<String, Object> row) {
        rows.add(new LinkedHashMap<>(row));
    }

    /**
     * Sets the column types for all columns.
     *
     * @param types the map of column names to types
     */
    public void setColumnTypes(Map<String, ColumnType> types) {
        columnTypes.clear();
        columnTypes.putAll(types);
    }

    /**
     * Checks if a column exists in the table.
     *
     * @param columnName the column name to check
     * @return true if the column exists, false otherwise
     */
    public boolean hasColumn(String columnName) {
        return columnTypes.containsKey(columnName);
    }

    /**
     * Returns a specific cell value by row index and column name.
     *
     * @param rowIndex    the zero-based row index
     * @param columnName the column name
     * @return the cell value
     * @throws IndexOutOfBoundsException if the row index is out of bounds
     */
    public Object getValue(int rowIndex, String columnName) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            throw new IndexOutOfBoundsException("Row index out of bounds: " + rowIndex);
        }
        return rows.get(rowIndex).get(columnName);
    }

    /**
     * Returns all values from a specific column.
     *
     * @param columnName the column name
     * @return a list of all values in the column
     */
    public List<Object> getColumnValues(String columnName) {
        List<Object> values = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            values.add(row.get(columnName));
        }
        return values;
    }

    /**
     * Returns a string representation of the table.
     *
     * @return a string containing table name, column count, and row count
     */
    @Override
    public String toString() {
        return String.format("CsvTable[name=%s, columns=%d, rows=%d]",
            name, columnNames.size(), rows.size());
    }
}