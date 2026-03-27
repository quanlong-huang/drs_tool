package com.csvsql.parser.engine;

import com.csvsql.parser.parser.QueryPlan;
import com.csvsql.parser.parser.QueryPlan.ColumnReference;

import java.util.*;

/**
 * Executes GROUP BY and aggregation operations.
 *
 * <p>GroupByExecutor handles the grouping and aggregation phase of query execution.
 * It supports:</p>
 * <ul>
 *   <li>GROUP BY with one or more columns</li>
 *   <li>Aggregate functions: COUNT, SUM, AVG, MIN, MAX</li>
 *   <li>COUNT(*) and COUNT(1) for counting all rows</li>
 *   <li>Aggregation without GROUP BY (single result row)</li>
 * </ul>
 *
 * <p>Example SQL handled by this executor:</p>
 * <pre>
 * SELECT department, COUNT(*) as count, AVG(salary) as avg_salary
 * FROM employees
 * GROUP BY department
 * </pre>
 *
 * @see QueryExecutor
 * @see com.csvsql.parser.parser.QueryPlan
 */
public class GroupByExecutor {

    /**
     * Executes GROUP BY operation on rows.
     *
     * <p>Rows are grouped by the GROUP BY columns, then aggregate functions
     * are computed for each group. Non-aggregated columns must be in the
     * GROUP BY clause.</p>
     *
     * @param rows the input rows to group
     * @param plan the query plan containing GROUP BY columns and aggregates
     * @return a list of result rows, one per group
     */
    public List<Map<String, Object>> execute(List<Map<String, Object>> rows, QueryPlan plan) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        // Group rows by GROUP BY columns
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String key = buildGroupKey(row, plan.getGroupByColumns());
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        // Apply aggregations to each group
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : groups.entrySet()) {
            List<Map<String, Object>> groupRows = entry.getValue();
            Map<String, Object> resultRow = new LinkedHashMap<>();

            // Add GROUP BY column values
            if (!groupRows.isEmpty()) {
                Map<String, Object> firstRow = groupRows.get(0);
                for (String col : plan.getGroupByColumns()) {
                    resultRow.put(col, getValue(firstRow, col));
                }
            }

            // Calculate aggregate functions
            for (ColumnReference col : plan.getColumns()) {
                if (col.isFunction()) {
                    Object aggValue = calculateAggregate(groupRows, col);
                    resultRow.put(col.getEffectiveName(), aggValue);
                } else if (!plan.getGroupByColumns().contains(col.getName())) {
                    // Non-aggregated column (should be in GROUP BY)
                    // Just take the first value
                    if (!groupRows.isEmpty()) {
                        resultRow.put(col.getEffectiveName(), getValue(groupRows.get(0), col.getName()));
                    }
                }
            }

            result.add(resultRow);
        }

        return result;
    }

    /**
     * Executes aggregation without GROUP BY (produces single result row).
     *
     * <p>Used when aggregate functions are present but no GROUP BY clause.
     * All rows are aggregated into a single result row.</p>
     *
     * <p>Example: SELECT COUNT(*), AVG(salary) FROM employees</p>
     *
     * @param rows the input rows to aggregate
     * @param plan the query plan containing aggregate functions
     * @return a list with a single result row containing aggregates
     */
    public List<Map<String, Object>> executeAggregateAll(List<Map<String, Object>> rows, QueryPlan plan) {
        Map<String, Object> resultRow = new LinkedHashMap<>();

        // Calculate aggregate functions
        for (ColumnReference col : plan.getColumns()) {
            if (col.isFunction()) {
                Object aggValue = calculateAggregate(rows, col);
                resultRow.put(col.getEffectiveName(), aggValue);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        result.add(resultRow);
        return result;
    }

    private String buildGroupKey(Map<String, Object> row, List<String> groupByColumns) {
        StringBuilder key = new StringBuilder();
        for (String col : groupByColumns) {
            Object value = getValue(row, col);
            key.append(value != null ? value.toString() : "NULL");
            key.append("\0"); // Null character as separator
        }
        return key.toString();
    }

    private Object getValue(Map<String, Object> row, String column) {
        if (column == null) return null;

        if (row.containsKey(column)) {
            return row.get(column);
        }

        // Try without table prefix
        if (column.contains(".")) {
            String shortName = column.substring(column.indexOf('.') + 1);
            if (row.containsKey(shortName)) {
                return row.get(shortName);
            }
        }

        return null;
    }

    private Object calculateAggregate(List<Map<String, Object>> rows, ColumnReference col) {
        String funcName = col.getFunctionName();
        if (funcName == null) return null;

        funcName = funcName.toUpperCase();

        switch (funcName) {
            case "COUNT":
                return calculateCount(rows, col.getName());
            case "SUM":
                return calculateSum(rows, col.getName());
            case "AVG":
                return calculateAvg(rows, col.getName());
            case "MIN":
                return calculateMin(rows, col.getName());
            case "MAX":
                return calculateMax(rows, col.getName());
            default:
                return null;
        }
    }

    private long calculateCount(List<Map<String, Object>> rows, String columnName) {
        if (columnName == null || columnName.equals("*")) {
            return rows.size();
        }

        // COUNT(1), COUNT(0), COUNT('constant') are equivalent to COUNT(*)
        // These are literal values, not column names
        if (isLiteralValue(columnName)) {
            return rows.size();
        }

        long count = 0;
        for (Map<String, Object> row : rows) {
            Object value = getValue(row, columnName);
            if (value != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Check if the column name represents a literal value (not a real column).
     * COUNT(1), COUNT(0), COUNT('a') should all count all rows like COUNT(*).
     */
    private boolean isLiteralValue(String columnName) {
        if (columnName == null) return false;
        // Numeric literals: "1", "0", "123", etc.
        try {
            Long.parseLong(columnName);
            return true;
        } catch (NumberFormatException e) {
            // Not a number, continue checking
        }
        // String literals in quotes: "'hello'"
        if (columnName.startsWith("'") && columnName.endsWith("'")) {
            return true;
        }
        // Boolean literals
        if (columnName.equalsIgnoreCase("true") || columnName.equalsIgnoreCase("false")) {
            return true;
        }
        return false;
    }

    private Object calculateSum(List<Map<String, Object>> rows, String columnName) {
        double sum = 0;
        boolean hasValue = false;

        for (Map<String, Object> row : rows) {
            Object value = getValue(row, columnName);
            if (value instanceof Number) {
                sum += ((Number) value).doubleValue();
                hasValue = true;
            }
        }

        return hasValue ? sum : null;
    }

    private Object calculateAvg(List<Map<String, Object>> rows, String columnName) {
        double sum = 0;
        int count = 0;

        for (Map<String, Object> row : rows) {
            Object value = getValue(row, columnName);
            if (value instanceof Number) {
                sum += ((Number) value).doubleValue();
                count++;
            }
        }

        return count > 0 ? sum / count : null;
    }

    @SuppressWarnings("unchecked")
    private Object calculateMin(List<Map<String, Object>> rows, String columnName) {
        Object min = null;

        for (Map<String, Object> row : rows) {
            Object value = getValue(row, columnName);
            if (value != null) {
                if (min == null) {
                    min = value;
                } else if (value instanceof Comparable && min instanceof Comparable) {
                    if (((Comparable<Object>) value).compareTo(min) < 0) {
                        min = value;
                    }
                }
            }
        }

        return min;
    }

    @SuppressWarnings("unchecked")
    private Object calculateMax(List<Map<String, Object>> rows, String columnName) {
        Object max = null;

        for (Map<String, Object> row : rows) {
            Object value = getValue(row, columnName);
            if (value != null) {
                if (max == null) {
                    max = value;
                } else if (value instanceof Comparable && max instanceof Comparable) {
                    if (((Comparable<Object>) value).compareTo(max) > 0) {
                        max = value;
                    }
                }
            }
        }

        return max;
    }
}