package com.csvsql.parser.output;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.util.List;
import java.util.Map;

/**
 * Formatter for table-style console output with borders and alignment.
 *
 * <p>TableFormatter produces human-readable ASCII table output with:</p>
 * <ul>
 *   <li>Unicode box-drawing characters for borders</li>
 *   <li>Automatic column width calculation</li>
 *   <li>Right-alignment for numeric values</li>
 *   <li>Optional ANSI color support</li>
 *   <li>Configurable maximum column width</li>
 * </ul>
 *
 * <p>Example output:</p>
 * <pre>
 * ┌─────────┬────────┬────────┐
 * │ id      │ name   │ salary │
 * ├─────────┼────────┼────────┤
 * │       1 │ Alice  │  50000 │
 * │       2 │ Bob    │  60000 │
 * └─────────┴────────┴────────┘
 * (2 rows, 15ms)
 * </pre>
 *
 * @see OutputFormatter
 * @see ResultSet
 */
public class TableFormatter implements OutputFormatter {

    private boolean useColors = true;
    private int maxColumnWidth = 50;

    public TableFormatter() {
        // Enable ANSI support on Windows
        AnsiConsole.systemInstall();
    }

    public TableFormatter setUseColors(boolean useColors) {
        this.useColors = useColors;
        return this;
    }

    public TableFormatter setMaxColumnWidth(int maxColumnWidth) {
        this.maxColumnWidth = maxColumnWidth;
        return this;
    }

    /**
     * Format a result set as a table string.
     *
     * @param resultSet the result set to format
     * @return the formatted table string
     */
    public String format(ResultSet resultSet) {
        if (resultSet.isEmpty()) {
            return "No results.\n";
        }

        List<String> columns = resultSet.getColumnNames();
        List<Map<String, Object>> rows = resultSet.getRows();

        // Calculate column widths
        int[] widths = calculateColumnWidths(columns, rows);

        StringBuilder sb = new StringBuilder();

        // Top border
        sb.append(buildBorder(widths, '┌', '┬', '┐')).append('\n');

        // Header row
        sb.append('│');
        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i);
            sb.append(' ');
            if (useColors) {
                sb.append(Ansi.ansi().fg(Ansi.Color.CYAN).a(padRight(col, widths[i])).reset());
            } else {
                sb.append(padRight(col, widths[i]));
            }
            sb.append(' ');
            sb.append('│');
        }
        sb.append('\n');

        // Header separator
        sb.append(buildBorder(widths, '├', '┼', '┤')).append('\n');

        // Data rows
        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            Map<String, Object> row = rows.get(rowIdx);
            sb.append('│');
            for (int i = 0; i < columns.size(); i++) {
                String col = columns.get(i);
                Object value = row.get(col);
                String valueStr = formatValue(value);

                sb.append(' ');
                // Right-align numbers, left-align others
                if (value instanceof Number) {
                    sb.append(padLeft(valueStr, widths[i]));
                } else {
                    sb.append(padRight(valueStr, widths[i]));
                }
                sb.append(' ');
                sb.append('│');
            }
            sb.append('\n');
        }

        // Bottom border
        sb.append(buildBorder(widths, '└', '┴', '┘')).append('\n');

        // Summary
        sb.append(String.format("(%d rows, %dms)\n", resultSet.getRowCount(), resultSet.getExecutionTimeMs()));

        return sb.toString();
    }

    private int[] calculateColumnWidths(List<String> columns, List<Map<String, Object>> rows) {
        int[] widths = new int[columns.size()];

        // Start with column name widths
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = Math.min(columns.get(i).length(), maxColumnWidth);
        }

        // Check data widths
        for (Map<String, Object> row : rows) {
            for (int i = 0; i < columns.size(); i++) {
                Object value = row.get(columns.get(i));
                int len = formatValue(value).length();
                widths[i] = Math.min(Math.max(widths[i], len), maxColumnWidth);
            }
        }

        return widths;
    }

    private String buildBorder(int[] widths, char left, char middle, char right) {
        StringBuilder sb = new StringBuilder();
        sb.append(left);
        for (int i = 0; i < widths.length; i++) {
            sb.append(repeat('─', widths[i] + 2));
            if (i < widths.length - 1) {
                sb.append(middle);
            }
        }
        sb.append(right);
        return sb.toString();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        String str = value.toString();
        if (str.length() > maxColumnWidth) {
            return str.substring(0, maxColumnWidth - 3) + "...";
        }
        return str;
    }

    private String padRight(String s, int width) {
        if (s.length() >= width) {
            return s.substring(0, width);
        }
        return s + repeat(' ', width - s.length());
    }

    private String padLeft(String s, int width) {
        if (s.length() >= width) {
            return s.substring(0, width);
        }
        return repeat(' ', width - s.length()) + s;
    }

    private String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Print the formatted result to console.
     */
    public void print(ResultSet resultSet) {
        System.out.print(format(resultSet));
    }

    @Override
    public String getName() {
        return "table";
    }
}