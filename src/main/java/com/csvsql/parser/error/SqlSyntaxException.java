package com.csvsql.parser.error;

/**
 * Exception thrown when SQL syntax is invalid.
 *
 * <p>This exception provides detailed information about syntax errors including:</p>
 * <ul>
 *   <li>Line and column number of the error</li>
 *   <li>The offending token</li>
 *   <li>The original SQL statement</li>
 *   <li>A suggestion for fixing the error</li>
 * </ul>
 *
 * <p>Example error message:</p>
 * <pre>
 * SQL Syntax Error: Unexpected token 'SELEC' at line 1, column 1
 * Unexpected token: 'SELEC'
 * Suggestion: Did you mean 'SELECT'?
 * </pre>
 *
 * @see CsvSqlException
 * @see com.csvsql.parser.parser.SqlParser
 */
public class SqlSyntaxException extends CsvSqlException {

    private final int line;
    private final int column;
    private final String sql;
    private final String token;

    public SqlSyntaxException(String message, String sql) {
        super(message);
        this.line = -1;
        this.column = -1;
        this.sql = sql;
        this.token = null;
    }

    public SqlSyntaxException(String message, String sql, int line, int column) {
        super(message);
        this.line = line;
        this.column = column;
        this.sql = sql;
        this.token = null;
    }

    public SqlSyntaxException(String message, String sql, int line, int column, String token, String suggestion) {
        super(message, suggestion);
        this.line = line;
        this.column = column;
        this.sql = sql;
        this.token = token;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getSql() {
        return sql;
    }

    public String getToken() {
        return token;
    }

    public boolean hasLocation() {
        return line > 0 && column > 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SQL Syntax Error: ").append(getMessage());

        if (hasLocation()) {
            sb.append(" at line ").append(line).append(", column ").append(column);
        }

        if (token != null) {
            sb.append("\nUnexpected token: '").append(token).append("'");
        }

        if (hasSuggestion()) {
            sb.append("\nSuggestion: ").append(getSuggestion());
        }

        return sb.toString();
    }
}