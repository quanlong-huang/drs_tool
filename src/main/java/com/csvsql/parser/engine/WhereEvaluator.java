package com.csvsql.parser.engine;

import com.csvsql.parser.parser.QueryPlan.*;

import java.util.*;

/**
 * Evaluates WHERE and HAVING conditions against row data.
 *
 * <p>WhereEvaluator filters rows based on condition expressions. It supports:</p>
 * <ul>
 *   <li>Comparison operators: =, !=, &lt;, &gt;, &lt;=, &gt;=</li>
 *   <li>Logical operators: AND, OR, NOT</li>
 *   <li>Special operators: LIKE, IN, NOT IN, IS NULL, IS NOT NULL</li>
 *   <li>NULL handling in comparisons</li>
 * </ul>
 *
 * <p>The evaluator handles both WHERE clause filtering and HAVING clause
 * filtering after GROUP BY aggregation.</p>
 *
 * @see QueryExecutor
 * @see com.csvsql.parser.parser.QueryPlan.FilterCondition
 */
public class WhereEvaluator {

    /**
     * Evaluates filter conditions against a list of rows.
     *
     * <p>Each row is tested against all conditions (combined with AND logic).
     * Rows that pass all conditions are included in the result.</p>
     *
     * @param rows       the rows to filter
     * @param conditions the filter conditions to evaluate
     * @return a new list containing only rows that pass all conditions
     */
    public List<Map<String, Object>> evaluate(List<Map<String, Object>> rows, List<FilterCondition> conditions) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            if (evaluateConditions(row, conditions)) {
                result.add(row);
            }
        }

        return result;
    }

    private boolean evaluateConditions(Map<String, Object> row, List<FilterCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        // If multiple conditions, treat as AND
        for (FilterCondition condition : conditions) {
            if (!evaluateCondition(row, condition)) {
                return false;
            }
        }

        return true;
    }

    private boolean evaluateCondition(Map<String, Object> row, FilterCondition condition) {
        if (condition.isLogical()) {
            return evaluateLogicalCondition(row, condition);
        }

        Object value = getValue(row, condition.getColumn());
        Object compareValue = condition.getValue();

        switch (condition.getOperator()) {
            case EQ:
                return equals(value, compareValue);
            case NE:
                return !equals(value, compareValue);
            case LT:
                return compare(value, compareValue) < 0;
            case GT:
                return compare(value, compareValue) > 0;
            case LE:
                return compare(value, compareValue) <= 0;
            case GE:
                return compare(value, compareValue) >= 0;
            case LIKE:
                return like(value, compareValue != null ? compareValue.toString() : null);
            case IS_NULL:
                return value == null;
            case IS_NOT_NULL:
                return value != null;
            case IN:
                return in(value, compareValue);
            case NOT_IN:
                return !in(value, compareValue);
            default:
                return true;
        }
    }

    private boolean evaluateLogicalCondition(Map<String, Object> row, FilterCondition condition) {
        List<FilterCondition> subConditions = condition.getSubConditions();

        switch (condition.getOperator()) {
            case AND:
                for (FilterCondition sub : subConditions) {
                    if (!evaluateCondition(row, sub)) {
                        return false;
                    }
                }
                return true;

            case OR:
                for (FilterCondition sub : subConditions) {
                    if (evaluateCondition(row, sub)) {
                        return true;
                    }
                }
                return false;

            case NOT:
                return subConditions != null && !subConditions.isEmpty() &&
                    !evaluateCondition(row, subConditions.get(0));

            default:
                return true;
        }
    }

    private Object getValue(Map<String, Object> row, String column) {
        if (column == null) return null;

        // Try exact match
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

        // Try with any table prefix
        for (String key : row.keySet()) {
            if (key.endsWith("." + column)) {
                return row.get(key);
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private boolean equals(Object val1, Object val2) {
        if (val1 == null && val2 == null) return true;
        if (val1 == null || val2 == null) return false;

        // Handle numeric comparison
        if (val1 instanceof Number && val2 instanceof Number) {
            return ((Number) val1).doubleValue() == ((Number) val2).doubleValue();
        }

        // Handle string comparison (case-insensitive for strings)
        if (val1 instanceof String && val2 instanceof String) {
            return val1.toString().equalsIgnoreCase(val2.toString());
        }

        return val1.equals(val2);
    }

    @SuppressWarnings("unchecked")
    private int compare(Object val1, Object val2) {
        if (val1 == null && val2 == null) return 0;
        if (val1 == null) return 1;
        if (val2 == null) return -1;

        // Handle numeric comparison
        if (val1 instanceof Number && val2 instanceof Number) {
            double diff = ((Number) val1).doubleValue() - ((Number) val2).doubleValue();
            return diff < 0 ? -1 : (diff > 0 ? 1 : 0);
        }

        // Handle string comparison
        if (val1 instanceof Comparable && val2 instanceof Comparable) {
            try {
                return ((Comparable<Object>) val1).compareTo(val2);
            } catch (ClassCastException e) {
                // Fall through to string comparison
            }
        }

        return val1.toString().compareTo(val2.toString());
    }

    private boolean like(Object value, String pattern) {
        if (value == null || pattern == null) return false;

        String str = value.toString().toLowerCase();
        String pat = pattern.toLowerCase();

        // Convert SQL LIKE pattern to regex
        String regex = pat.replace(".", "\\.")
            .replace("%", ".*")
            .replace("_", ".");

        return str.matches(regex);
    }

    private boolean in(Object value, Object list) {
        if (value == null || list == null) return false;

        if (list instanceof Collection) {
            for (Object item : (Collection<?>) list) {
                if (equals(value, item)) {
                    return true;
                }
            }
        }

        return false;
    }
}