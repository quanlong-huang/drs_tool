package com.csvsql.parser.engine;

import com.csvsql.parser.parser.QueryPlan;
import com.csvsql.parser.parser.QueryPlan.ColumnReference;
import com.csvsql.parser.parser.QueryPlan.SortSpecification;
import net.sf.jsqlparser.expression.Expression;

import java.util.*;

/**
 * Executes SELECT projection and result ordering.
 *
 * <p>SelectExecutor is responsible for:</p>
 * <ul>
 *   <li>Projecting selected columns from result rows</li>
 *   <li>Evaluating expressions in SELECT clause (CASE WHEN, arithmetic, etc.)</li>
 *   <li>Applying ORDER BY sorting</li>
 *   <li>Applying LIMIT and OFFSET</li>
 * </ul>
 *
 * <p>The executor works with pre-processed rows from earlier query stages
 * (WHERE filtering, JOINs, GROUP BY aggregation) and produces the final
 * result rows for output.</p>
 *
 * @see QueryExecutor
 * @see ExpressionEvaluator
 * @see com.csvsql.parser.parser.QueryPlan
 */
public class SelectExecutor {

    private final ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    /**
     * Projects specified columns from rows according to the query plan.
     *
     * <p>For each row, this method extracts the requested columns and handles:
     * <ul>
     *   <li>SELECT * - includes all columns</li>
     *   <li>Aggregate functions - uses pre-computed values</li>
     *   <li>Expressions - evaluates CASE WHEN, arithmetic, etc.</li>
     *   <li>Regular columns - looks up by name</li>
     * </ul>
     *
     * @param rows          the input rows to project
     * @param plan          the query plan containing column specifications
     * @param resultColumns the list of result column names
     * @return a new list of rows with only the projected columns
     */
    public List<Map<String, Object>> project(List<Map<String, Object>> rows, QueryPlan plan, List<String> resultColumns) {
        List<Map<String, Object>> projected = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Map<String, Object> projectedRow = new LinkedHashMap<>();

            if (plan.isSelectAll()) {
                // SELECT * - include all columns
                projectedRow.putAll(row);
            } else if (plan.hasSelectAllTableColumns()) {
                // SELECT table.* or table.*, col - handle table.* selections
                // Use resultColumns which already contains the expanded column names
                for (String colName : resultColumns) {
                    Object value = findValue(row, colName);
                    projectedRow.put(colName, value);
                }
            } else {
                // SELECT specific columns
                for (ColumnReference col : plan.getColumns()) {
                    String targetName = col.getEffectiveName();

                    Object value;
                    if (col.isFunction()) {
                        // For aggregate functions, the value is stored with the effective name (alias)
                        // Check this BEFORE hasExpression because aggregate functions have expression but need pre-computed values
                        value = row.get(targetName);
                    } else if (col.hasExpression()) {
                        // Complex expression (CASE WHEN, arithmetic, etc.)
                        Expression expr = col.getExpression();
                        value = expressionEvaluator.evaluate(expr, row);
                    } else {
                        // For regular columns, look up by source name
                        String sourceName = col.getName();
                        value = findValue(row, sourceName);
                    }
                    projectedRow.put(targetName, value);
                }
            }

            projected.add(projectedRow);
        }

        return projected;
    }

    private Object findValue(Map<String, Object> row, String columnName) {
        // Try exact match first
        if (row.containsKey(columnName)) {
            return row.get(columnName);
        }

        // Try without table prefix
        if (columnName.contains(".")) {
            String shortName = columnName.substring(columnName.indexOf('.') + 1);
            if (row.containsKey(shortName)) {
                return row.get(shortName);
            }
        }

        // Try with table prefix
        for (String key : row.keySet()) {
            if (key.endsWith("." + columnName)) {
                return row.get(key);
            }
        }

        return null;
    }

    /**
     * Sorts rows according to ORDER BY specifications.
     *
     * <p>Supports multiple sort columns with ASC/DESC ordering. NULL values
     * are sorted last regardless of sort direction.</p>
     *
     * @param rows      the rows to sort
     * @param sortSpecs the sort specifications (column, direction)
     * @return a new sorted list of rows
     */
    public List<Map<String, Object>> orderBy(List<Map<String, Object>> rows, List<SortSpecification> sortSpecs) {
        if (sortSpecs == null || sortSpecs.isEmpty()) {
            return rows;
        }

        List<Map<String, Object>> sorted = new ArrayList<>(rows);

        sorted.sort((row1, row2) -> {
            for (SortSpecification spec : sortSpecs) {
                String col = spec.getColumn();
                Object val1 = findValue(row1, col);
                Object val2 = findValue(row2, col);

                int cmp = compareValues(val1, val2);

                if (cmp != 0) {
                    return spec.isAscending() ? cmp : -cmp;
                }
            }
            return 0;
        });

        return sorted;
    }

    @SuppressWarnings("unchecked")
    private int compareValues(Object val1, Object val2) {
        // Handle nulls
        if (val1 == null && val2 == null) return 0;
        if (val1 == null) return 1;  // NULLs last
        if (val2 == null) return -1;

        // Handle comparable values
        if (val1 instanceof Comparable && val2 instanceof Comparable) {
            try {
                return ((Comparable<Object>) val1).compareTo(val2);
            } catch (ClassCastException e) {
                // Different types, compare as strings
            }
        }

        // Fallback to string comparison
        return val1.toString().compareTo(val2.toString());
    }

    /**
     * Applies LIMIT and OFFSET to the result rows.
     *
     * <p>If OFFSET is specified, that many rows are skipped from the beginning.
     * If LIMIT is specified, at most that many rows are returned.</p>
     *
     * @param rows   the rows to limit
     * @param limit  the maximum number of rows to return, or null for no limit
     * @param offset the number of rows to skip, or null for no offset
     * @return a new list containing the limited rows
     */
    public List<Map<String, Object>> applyLimitOffset(List<Map<String, Object>> rows, Integer limit, Integer offset) {
        int start = offset != null ? offset : 0;
        int end = limit != null ? start + limit : rows.size();

        if (start >= rows.size()) {
            return Collections.emptyList();
        }

        end = Math.min(end, rows.size());

        return new ArrayList<>(rows.subList(start, end));
    }
}