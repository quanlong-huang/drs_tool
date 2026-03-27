package com.csvsql.parser.engine;

import com.csvsql.parser.data.CsvTable;
import com.csvsql.parser.parser.QueryPlan;
import com.csvsql.parser.parser.QueryPlan.JoinClause;

import java.util.*;

/**
 * Executes JOIN operations between tables.
 *
 * <p>JoinExecutor supports the following join types:</p>
 * <ul>
 *   <li>INNER JOIN - returns rows when there is a match in both tables</li>
 *   <li>LEFT JOIN - returns all rows from the left table, with matched right table rows</li>
 *   <li>RIGHT JOIN - returns all rows from the right table, with matched left table rows</li>
 * </ul>
 *
 * <p>The executor uses hash-based indexing for efficient join operations.
 * An index is built on the join column of the right table, enabling O(1)
 * lookups during the join process.</p>
 *
 * <p>Example SQL handled by this executor:</p>
 * <pre>
 * SELECT * FROM orders o
 * INNER JOIN customers c ON o.customer_id = c.id
 * </pre>
 *
 * @see QueryExecutor
 * @see com.csvsql.parser.parser.QueryPlan.JoinClause
 */
public class JoinExecutor {

    /**
     * Executes a JOIN operation between left rows and a right table.
     *
     * <p>The method builds an index on the right table's join column for
     * efficient lookups, then iterates through left rows to find matches.</p>
     *
     * @param leftRows   the rows from the left table
     * @param rightTable the right table to join with
     * @param join       the join clause specifying join type and columns
     * @return a new list of rows containing the join result
     */
    public List<Map<String, Object>> executeJoin(List<Map<String, Object>> leftRows, CsvTable rightTable, JoinClause join) {
        List<Map<String, Object>> result = new ArrayList<>();

        // Build index on right table for faster lookup
        String rightColumn = extractColumnName(join.getRightColumn());
        Map<Object, List<Map<String, Object>>> rightIndex = buildIndex(rightTable.getRows(), rightColumn);

        String leftColumn = extractColumnName(join.getLeftColumn());

        for (Map<String, Object> leftRow : leftRows) {
            Object leftValue = getValue(leftRow, leftColumn);
            List<Map<String, Object>> rightMatches = rightIndex.getOrDefault(leftValue, Collections.emptyList());

            if (rightMatches.isEmpty()) {
                // No match
                if (join.getType() == JoinClause.JoinType.LEFT) {
                    // Include left row with NULL right columns
                    Map<String, Object> combinedRow = new LinkedHashMap<>(leftRow);
                    for (String col : rightTable.getColumnNames()) {
                        combinedRow.put(col, null);
                    }
                    result.add(combinedRow);
                }
            } else {
                // Match found
                for (Map<String, Object> rightRow : rightMatches) {
                    Map<String, Object> combinedRow = new LinkedHashMap<>(leftRow);
                    combinedRow.putAll(rightRow);
                    result.add(combinedRow);
                }
            }
        }

        // Handle RIGHT JOIN
        if (join.getType() == JoinClause.JoinType.RIGHT) {
            result = handleRightJoin(leftRows, rightTable, join, rightIndex);
        }

        return result;
    }

    private List<Map<String, Object>> handleRightJoin(
        List<Map<String, Object>> leftRows,
        CsvTable rightTable,
        JoinClause join,
        Map<Object, List<Map<String, Object>>> rightIndex
    ) {
        List<Map<String, Object>> result = new ArrayList<>();

        // Build index on left table
        String leftColumn = extractColumnName(join.getLeftColumn());
        Map<Object, List<Map<String, Object>>> leftIndex = buildIndex(leftRows, leftColumn);

        // Process all right rows
        for (Map<String, Object> rightRow : rightTable.getRows()) {
            String rightColumn = extractColumnName(join.getRightColumn());
            Object rightValue = rightRow.get(rightColumn);
            List<Map<String, Object>> leftMatches = leftIndex.getOrDefault(rightValue, Collections.emptyList());

            if (leftMatches.isEmpty()) {
                // No match - include right row with NULL left columns
                Map<String, Object> combinedRow = new LinkedHashMap<>();
                for (String col : getColumns(leftRows)) {
                    combinedRow.put(col, null);
                }
                combinedRow.putAll(rightRow);
                result.add(combinedRow);
            } else {
                // Match found
                for (Map<String, Object> leftRow : leftMatches) {
                    Map<String, Object> combinedRow = new LinkedHashMap<>(leftRow);
                    combinedRow.putAll(rightRow);
                    result.add(combinedRow);
                }
            }
        }

        return result;
    }

    private Map<Object, List<Map<String, Object>>> buildIndex(List<Map<String, Object>> rows, String columnName) {
        Map<Object, List<Map<String, Object>>> index = new HashMap<>();

        for (Map<String, Object> row : rows) {
            Object key = getValue(row, columnName);
            if (key != null) {
                index.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            }
        }

        return index;
    }

    private String extractColumnName(String qualifiedName) {
        if (qualifiedName == null) return null;
        if (qualifiedName.contains(".")) {
            return qualifiedName.substring(qualifiedName.indexOf('.') + 1);
        }
        return qualifiedName;
    }

    private Object getValue(Map<String, Object> row, String column) {
        if (column == null) return null;

        if (row.containsKey(column)) {
            return row.get(column);
        }

        // Try with table prefix
        for (String key : row.keySet()) {
            if (key.endsWith("." + column)) {
                return row.get(key);
            }
        }

        return null;
    }

    private Set<String> getColumns(List<Map<String, Object>> rows) {
        Set<String> columns = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            columns.addAll(row.keySet());
        }
        return columns;
    }
}