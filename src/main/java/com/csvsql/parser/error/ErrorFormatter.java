package com.csvsql.parser.error;

import org.fusesource.jansi.Ansi;

/**
 * Formatter for detailed error messages with suggestions and context.
 *
 * <p>ErrorFormatter creates user-friendly error messages with:</p>
 * <ul>
 *   <li>Color-coded output (when supported by terminal)</li>
 *   <li>Clear error type headers</li>
 *   <li>Detailed context information</li>
 *   <li>Helpful suggestions for fixing errors</li>
 *   <li>SQL context with error position markers</li>
 * </ul>
 *
 * <p>Example formatted output:</p>
 * <pre>
 * ✗ SQL Syntax Error
 * Unexpected token 'SELEC'
 *
 *   1 | SELEC * FROM data.csv
 *     | ^
 *
 *   💡 Did you mean 'SELECT'?
 * </pre>
 *
 * @see CsvSqlException
 * @see SqlSyntaxException
 */
public class ErrorFormatter {

    private boolean useColors = true;

    public ErrorFormatter() {
        this.useColors = true;
    }

    public ErrorFormatter(boolean useColors) {
        this.useColors = useColors;
    }

    /**
     * Format a CSV SQL exception with detailed information.
     */
    public String format(CsvSqlException e) {
        StringBuilder sb = new StringBuilder();

        // Error type header
        sb.append(formatErrorType(e));
        sb.append("\n");

        // Main message
        sb.append(formatMessage(e.getMessage()));
        sb.append("\n");

        // Context if available
        if (e.getContext() != null) {
            sb.append("\n");
            sb.append(formatContext(e.getContext()));
        }

        // Suggestion if available
        if (e.hasSuggestion()) {
            sb.append("\n");
            sb.append(formatSuggestion(e.getSuggestion()));
        }

        return sb.toString();
    }

    /**
     * Format a SQL syntax error with line/column information.
     */
    public String formatSqlError(SqlSyntaxException e) {
        StringBuilder sb = new StringBuilder();

        // Error type header
        sb.append(formatErrorHeader("SQL Syntax Error"));
        sb.append("\n");

        // Main message
        sb.append(formatMessage(e.getMessage()));
        sb.append("\n");

        // SQL context with error position
        if (e.getSql() != null) {
            sb.append("\n");
            sb.append(formatSqlContext(e.getSql(), e.getLine(), e.getColumn()));
        }

        // Suggestion if available
        if (e.hasSuggestion()) {
            sb.append("\n");
            sb.append(formatSuggestion(e.getSuggestion()));
        }

        return sb.toString();
    }

    /**
     * Format a column not found error with suggestions.
     */
    public String formatColumnError(ColumnNotFoundException e) {
        StringBuilder sb = new StringBuilder();

        // Error type header
        sb.append(formatErrorHeader("Column Not Found"));
        sb.append("\n");

        // Main message
        sb.append(formatMessage("Column '" + e.getColumnName() + "' does not exist"));
        sb.append("\n");

        // Available columns
        if (e.getAvailableColumns() != null && !e.getAvailableColumns().isEmpty()) {
            sb.append("\n");
            sb.append(formatInfo("Available columns: " + String.join(", ", e.getAvailableColumns())));
        }

        // Suggestion
        if (e.hasSuggestion()) {
            sb.append("\n");
            sb.append(formatSuggestion("Did you mean: " + e.getSuggestion() + "?"));
        }

        return sb.toString();
    }

    /**
     * Format a file not found error.
     */
    public String formatFileError(FileNotFoundException e) {
        StringBuilder sb = new StringBuilder();

        sb.append(formatErrorHeader("File Not Found"));
        sb.append("\n");

        sb.append(formatMessage("File '" + e.getFilePath() + "' does not exist or cannot be read"));
        sb.append("\n");

        if (e.hasSuggestion()) {
            sb.append("\n");
            sb.append(formatSuggestion(e.getSuggestion()));
        }

        return sb.toString();
    }

    /**
     * Format an unsupported feature error.
     */
    public String formatUnsupportedError(UnsupportedFeatureException e) {
        StringBuilder sb = new StringBuilder();

        sb.append(formatErrorHeader("Unsupported Feature"));
        sb.append("\n");

        sb.append(formatMessage(e.getMessage()));
        sb.append("\n");

        if (e.getAlternatives() != null && !e.getAlternatives().isEmpty()) {
            sb.append("\n");
            sb.append(formatInfo("Alternatives: " + e.getAlternatives()));
        }

        return sb.toString();
    }

    private String formatErrorType(CsvSqlException e) {
        String typeName = e.getClass().getSimpleName();
        if (typeName.endsWith("Exception")) {
            typeName = typeName.substring(0, typeName.length() - 9);
        }
        return formatErrorHeader(typeName + " Error");
    }

    private String formatErrorHeader(String text) {
        if (useColors) {
            return Ansi.ansi().fg(Ansi.Color.RED).bold().a("✗ ").a(text).reset().toString();
        }
        return "✗ " + text;
    }

    private String formatMessage(String message) {
        if (useColors) {
            return Ansi.ansi().fg(Ansi.Color.WHITE).a(message).reset().toString();
        }
        return message;
    }

    private String formatContext(String context) {
        if (useColors) {
            return Ansi.ansi().fg(Ansi.Color.YELLOW).a("  Context: ").a(context).reset().toString();
        }
        return "  Context: " + context;
    }

    private String formatSuggestion(String suggestion) {
        if (useColors) {
            return Ansi.ansi().fg(Ansi.Color.CYAN).a("  💡 ").a(suggestion).reset().toString();
        }
        return "  Suggestion: " + suggestion;
    }

    private String formatInfo(String info) {
        if (useColors) {
            return Ansi.ansi().fg(Ansi.Color.BLUE).a("  ").a(info).reset().toString();
        }
        return "  " + info;
    }

    private String formatSqlContext(String sql, Integer line, Integer column) {
        StringBuilder sb = new StringBuilder();

        // Split SQL into lines
        String[] lines = sql.split("\n");
        int lineNumber = line != null ? line : 1;
        int colNumber = column != null ? column : 1;

        // Show context (max 3 lines around error)
        int startLine = Math.max(1, lineNumber - 1);
        int endLine = Math.min(lines.length, lineNumber + 1);

        for (int i = startLine; i <= endLine; i++) {
            String lineText = i <= lines.length ? lines[i - 1] : "";
            String linePrefix = String.format("%4d | ", i);

            if (useColors) {
                sb.append(Ansi.ansi().fg(Ansi.Color.BLUE).a(linePrefix).reset());
            } else {
                sb.append(linePrefix);
            }

            sb.append(lineText);
            sb.append("\n");

            // Add error pointer on the error line
            if (i == lineNumber) {
                sb.append("     | ");
                for (int j = 1; j < colNumber && j <= lineText.length(); j++) {
                    sb.append(" ");
                }
                if (useColors) {
                    sb.append(Ansi.ansi().fg(Ansi.Color.RED).bold().a("^").reset());
                } else {
                    sb.append("^");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public boolean isUseColors() {
        return useColors;
    }

    public void setUseColors(boolean useColors) {
        this.useColors = useColors;
    }
}