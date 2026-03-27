package com.csvsql.parser.error;

/**
 * Base exception for CSV SQL Parser errors.
 *
 * <p>This is the base class for all exceptions thrown by the CSV SQL Parser.
 * It provides:</p>
 * <ul>
 *   <li>A clear error message</li>
 *   <li>An optional suggestion for how to fix the error</li>
 *   <li>An optional context string for additional details</li>
 * </ul>
 *
 * <p>All specific exception types extend this class to provide additional
 * context-specific information.</p>
 *
 * @see SqlSyntaxException
 * @see UnsupportedFeatureException
 * @see ColumnNotFoundException
 * @see FileNotFoundException
 */
public class CsvSqlException extends RuntimeException {

    private final String message;
    private final String suggestion;
    private final String context;

    public CsvSqlException(String message) {
        super(message);
        this.message = message;
        this.suggestion = null;
        this.context = null;
    }

    public CsvSqlException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.suggestion = null;
        this.context = null;
    }

    public CsvSqlException(String message, String suggestion) {
        super(message);
        this.message = message;
        this.suggestion = suggestion;
        this.context = null;
    }

    public CsvSqlException(String message, String suggestion, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.suggestion = suggestion;
        this.context = null;
    }

    public CsvSqlException(String message, String suggestion, String context) {
        super(message);
        this.message = message;
        this.suggestion = suggestion;
        this.context = context;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public String getContext() {
        return context;
    }

    public boolean hasSuggestion() {
        return suggestion != null && !suggestion.isEmpty();
    }
}