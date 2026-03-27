package com.csvsql.parser.error;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when a column is not found in a table.
 *
 * <p>This exception provides intelligent suggestions by finding similar column
 * names using:</p>
 * <ul>
 *   <li>Prefix matching</li>
 *   <li>Levenshtein distance (edit distance)</li>
 * </ul>
 *
 * <p>Example error message:</p>
 * <pre>
 * Error: Column 'emp_name' not found in table 'employees'
 *
 * Available columns:
 *   - id
 *   - name
 *   - salary
 *
 * Did you mean: name?
 * </pre>
 *
 * @see CsvSqlException
 */
public class ColumnNotFoundException extends CsvSqlException {

    private final String columnName;
    private final String tableName;
    private final List<String> availableColumns;
    private final List<String> suggestions;

    public ColumnNotFoundException(String columnName, String tableName) {
        super("Column '" + columnName + "' not found in table '" + tableName + "'");
        this.columnName = columnName;
        this.tableName = tableName;
        this.availableColumns = new ArrayList<>();
        this.suggestions = new ArrayList<>();
    }

    public ColumnNotFoundException(String columnName, String tableName, List<String> availableColumns) {
        super("Column '" + columnName + "' not found in table '" + tableName + "'");
        this.columnName = columnName;
        this.tableName = tableName;
        this.availableColumns = new ArrayList<>(availableColumns);
        this.suggestions = new ArrayList<>();
        findSimilarColumns();
    }

    public String getColumnName() {
        return columnName;
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getAvailableColumns() {
        return availableColumns;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    private void findSimilarColumns() {
        for (String col : availableColumns) {
            if (isSimilar(columnName, col)) {
                suggestions.add(col);
            }
        }
    }

    private boolean isSimilar(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();

        // Check for prefix match
        if (a.startsWith(b) || b.startsWith(a)) {
            return true;
        }

        // Check for Levenshtein distance
        int distance = levenshteinDistance(a, b);
        int maxLength = Math.max(a.length(), b.length());

        // Consider similar if distance is less than 1/3 of max length
        return distance <= maxLength / 3;
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[a.length()][b.length()];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Error: Column '").append(columnName).append("' not found in table '").append(tableName).append("'");

        if (!availableColumns.isEmpty()) {
            sb.append("\n\nAvailable columns:");
            for (String col : availableColumns) {
                sb.append("\n  - ").append(col);
            }
        }

        if (!suggestions.isEmpty()) {
            sb.append("\n\nDid you mean: ").append(String.join(" or ", suggestions)).append("?");
        }

        return sb.toString();
    }
}