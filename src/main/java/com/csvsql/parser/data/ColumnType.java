package com.csvsql.parser.data;

/**
 * Enumeration of supported column data types for CSV data.
 *
 * <p>The type system supports the following types, automatically inferred
 * from CSV data during loading:</p>
 * <ul>
 *   <li>{@link #INTEGER} - 32-bit integer values</li>
 *   <li>{@link #LONG} - 64-bit integer values</li>
 *   <li>{@link #DOUBLE} - Floating-point values</li>
 *   <li>{@link #STRING} - Text values (default for non-numeric data)</li>
 *   <li>{@link #DATE} - Date values in ISO format (yyyy-MM-dd)</li>
 *   <li>{@link #BOOLEAN} - Boolean values (true/false)</li>
 * </ul>
 *
 * @see TypeInferer
 * @see CsvTable
 */
public enum ColumnType {
    /** 32-bit integer type for whole numbers within Integer range. */
    INTEGER("INTEGER"),

    /** 64-bit integer type for large whole numbers. */
    LONG("LONG"),

    /** Double-precision floating-point type for decimal values. */
    DOUBLE("DOUBLE"),

    /** String type for text data (default type). */
    STRING("STRING"),

    /** Date type for date values in ISO-8601 format. */
    DATE("DATE"),

    /** Boolean type for true/false values. */
    BOOLEAN("BOOLEAN");

    private final String name;

    ColumnType(String name) {
        this.name = name;
    }

    /**
     * Returns the display name of this type.
     *
     * @return the type name as displayed in SQL contexts
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the display name of this type.
     *
     * @return the type name
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Checks if this type represents numeric data.
     *
     * @return true if this is INTEGER, LONG, or DOUBLE; false otherwise
     */
    public boolean isNumeric() {
        return this == INTEGER || this == LONG || this == DOUBLE;
    }

    /**
     * Returns the corresponding Java class for this column type.
     *
     * @return the Java class used to store values of this type
     */
    public Class<?> getJavaClass() {
        switch (this) {
            case INTEGER:
                return Integer.class;
            case LONG:
                return Long.class;
            case DOUBLE:
                return Double.class;
            case DATE:
                return java.time.LocalDate.class;
            case BOOLEAN:
                return Boolean.class;
            case STRING:
            default:
                return String.class;
        }
    }
}