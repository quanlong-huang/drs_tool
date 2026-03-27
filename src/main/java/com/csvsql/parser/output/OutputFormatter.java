package com.csvsql.parser.output;

import java.util.List;
import java.util.Map;

/**
 * Interface for formatting query results into different output formats.
 *
 * <p>Implementations of this interface transform a {@link ResultSet} into
 * a formatted string representation. Built-in implementations include:</p>
 * <ul>
 *   <li>{@link TableFormatter} - ASCII table format with borders</li>
 *   <li>{@link CsvFormatter} - CSV format for data export</li>
 *   <li>{@link JsonFormatter} - JSON format with metadata</li>
 * </ul>
 *
 * <p>Custom formatters can be created by implementing this interface and
 * registering them with the CLI or using them programmatically.</p>
 *
 * @see ResultSet
 * @see TableFormatter
 * @see CsvFormatter
 * @see JsonFormatter
 */
public interface OutputFormatter {

    /**
     * Formats a result set into a string representation.
     *
     * @param resultSet the result set to format, must not be null
     * @return the formatted string representation
     */
    String format(ResultSet resultSet);

    /**
     * Returns the unique name identifying this format.
     *
     * <p>The name is used to select the formatter via command-line options
     * and REPL commands.</p>
     *
     * @return the format name (e.g., "table", "csv", "json")
     */
    String getName();
}