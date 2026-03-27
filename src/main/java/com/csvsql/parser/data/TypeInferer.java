package com.csvsql.parser.data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for inferring and parsing column data types from string values.
 *
 * <p>TypeInferer analyzes string values from CSV columns to determine the most
 * appropriate data type. It supports the following types in order of specificity:</p>
 * <ol>
 *   <li>INTEGER - for whole numbers within int range</li>
 *   <li>LONG - for whole numbers outside int range</li>
 *   <li>DOUBLE - for decimal numbers</li>
 *   <li>DATE - for date values in various formats</li>
 *   <li>BOOLEAN - for true/false values</li>
 *   <li>STRING - default type for non-matching values</li>
 * </ol>
 *
 * <p>The class also handles NULL values represented as "null", "NULL", "NA", "N/A", "None", or "-".</p>
 *
 * @see ColumnType
 * @see CsvLoader
 */
public class TypeInferer {

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM-dd-yyyy")
    };

    /**
     * Infers the column type from a list of string values.
     *
     * <p>The inference process filters out null and empty values, then tries
     * to match all remaining values against each type in order of specificity.
     * Returns STRING type if no other type matches all values.</p>
     *
     * @param values the list of string values to analyze (may contain nulls)
     * @return the inferred column type, never null
     */
    public ColumnType inferType(List<String> values) {
        if (values == null || values.isEmpty()) {
            return ColumnType.STRING;
        }

        // Filter out null/empty values
        List<String> nonEmptyValues = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty() && !isNullValue(value)) {
                nonEmptyValues.add(value.trim());
            }
        }

        if (nonEmptyValues.isEmpty()) {
            return ColumnType.STRING;
        }

        // Try each type in order of specificity
        if (isAllIntegers(nonEmptyValues)) {
            return ColumnType.INTEGER;
        }
        if (isAllLongs(nonEmptyValues)) {
            return ColumnType.LONG;
        }
        if (isAllDoubles(nonEmptyValues)) {
            return ColumnType.DOUBLE;
        }
        if (isAllDates(nonEmptyValues)) {
            return ColumnType.DATE;
        }
        if (isAllBooleans(nonEmptyValues)) {
            return ColumnType.BOOLEAN;
        }

        return ColumnType.STRING;
    }

    /**
     * Checks if a value represents a NULL value.
     *
     * @param value the value to check
     * @return true if the value represents NULL (null, empty, "NA", "N/A", "None", "-")
     */
    private boolean isNullValue(String value) {
        if (value == null) return true;
        String trimmed = value.trim().toUpperCase();
        return trimmed.isEmpty() ||
               trimmed.equals("NULL") ||
               trimmed.equals("NA") ||
               trimmed.equals("N/A") ||
               trimmed.equals("NONE") ||
               trimmed.equals("-");
    }

    /**
     * Checks if all values can be parsed as integers within int range.
     */
    private boolean isAllIntegers(List<String> values) {
        for (String value : values) {
            try {
                long longValue = Long.parseLong(value);
                if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if all values can be parsed as long integers.
     */
    private boolean isAllLongs(List<String> values) {
        for (String value : values) {
            try {
                Long.parseLong(value);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if all values can be parsed as doubles.
     */
    private boolean isAllDoubles(List<String> values) {
        for (String value : values) {
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if all values can be parsed as dates.
     */
    private boolean isAllDates(List<String> values) {
        for (String value : values) {
            if (!isDate(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Attempts to parse a value as a date using various formats.
     */
    private boolean isDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate.parse(value, formatter);
                return true;
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        return false;
    }

    /**
     * Checks if all values can be parsed as booleans.
     */
    private boolean isAllBooleans(List<String> values) {
        for (String value : values) {
            String lower = value.toLowerCase();
            if (!lower.equals("true") && !lower.equals("false") &&
                !lower.equals("yes") && !lower.equals("no") &&
                !lower.equals("1") && !lower.equals("0")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses a string value to the specified type.
     *
     * <p>If parsing fails for the target type, the original string value
     * (trimmed) is returned. NULL values are converted to Java null.</p>
     *
     * @param value the string value to parse (may be null)
     * @param type  the target column type
     * @return the parsed value of the appropriate Java type, or null for NULL values
     */
    public Object parseValue(String value, ColumnType type) {
        if (value == null || value.trim().isEmpty() || isNullValue(value)) {
            return null;
        }

        String trimmed = value.trim();

        try {
            switch (type) {
                case INTEGER:
                    return Integer.parseInt(trimmed);
                case LONG:
                    return Long.parseLong(trimmed);
                case DOUBLE:
                    return Double.parseDouble(trimmed);
                case DATE:
                    return parseDate(trimmed);
                case BOOLEAN:
                    return parseBoolean(trimmed);
                case STRING:
                default:
                    return trimmed;
            }
        } catch (Exception e) {
            // Return as string if parsing fails
            return trimmed;
        }
    }

    /**
     * Parses a date string using the configured date formats.
     *
     * @param value the date string to parse
     * @return the parsed LocalDate
     * @throws IllegalArgumentException if the value cannot be parsed as any date format
     */
    private LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        throw new IllegalArgumentException("Cannot parse date: " + value);
    }

    /**
     * Parses a boolean string.
     *
     * @param value the boolean string to parse
     * @return the parsed Boolean
     * @throws IllegalArgumentException if the value cannot be parsed as a boolean
     */
    private Boolean parseBoolean(String value) {
        String lower = value.toLowerCase();
        if (lower.equals("true") || lower.equals("yes") || lower.equals("1")) {
            return true;
        }
        if (lower.equals("false") || lower.equals("no") || lower.equals("0")) {
            return false;
        }
        throw new IllegalArgumentException("Cannot parse boolean: " + value);
    }
}