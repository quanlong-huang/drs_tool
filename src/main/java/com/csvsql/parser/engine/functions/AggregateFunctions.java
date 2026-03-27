package com.csvsql.parser.engine.functions;

import java.util.List;

/**
 * Implementation of SQL aggregate functions: COUNT, SUM, AVG, MIN, MAX.
 *
 * <p>AggregateFunctions provides static methods for computing aggregate values
 * over a list of values. These functions are used by {@link com.csvsql.parser.engine.GroupByExecutor}
 * during query execution.</p>
 *
 * <p>All functions handle NULL values correctly:</p>
 * <ul>
 *   <li>COUNT - counts non-NULL values</li>
 *   <li>SUM/AVG - ignores NULL values, returns NULL if no numeric values</li>
 *   <li>MIN/MAX - ignores NULL values, returns NULL if no values</li>
 * </ul>
 *
 * @see com.csvsql.parser.engine.GroupByExecutor
 */
public class AggregateFunctions {

    /**
     * Calculate COUNT.
     */
    public static long count(List<Object> values) {
        if (values == null) return 0;
        return values.stream().filter(v -> v != null).count();
    }

    /**
     * Calculate SUM.
     */
    public static Double sum(List<Object> values) {
        if (values == null || values.isEmpty()) return null;

        double sum = 0;
        boolean hasValue = false;

        for (Object value : values) {
            if (value instanceof Number) {
                sum += ((Number) value).doubleValue();
                hasValue = true;
            }
        }

        return hasValue ? sum : null;
    }

    /**
     * Calculate AVG.
     */
    public static Double avg(List<Object> values) {
        if (values == null || values.isEmpty()) return null;

        double sum = 0;
        int count = 0;

        for (Object value : values) {
            if (value instanceof Number) {
                sum += ((Number) value).doubleValue();
                count++;
            }
        }

        return count > 0 ? sum / count : null;
    }

    /**
     * Calculate MIN.
     */
    @SuppressWarnings("unchecked")
    public static Object min(List<Object> values) {
        if (values == null || values.isEmpty()) return null;

        Object min = null;

        for (Object value : values) {
            if (value != null) {
                if (min == null) {
                    min = value;
                } else if (value instanceof Comparable && min instanceof Comparable) {
                    try {
                        if (((Comparable<Object>) value).compareTo(min) < 0) {
                            min = value;
                        }
                    } catch (ClassCastException e) {
                        // Different types, compare as strings
                        if (value.toString().compareTo(min.toString()) < 0) {
                            min = value;
                        }
                    }
                }
            }
        }

        return min;
    }

    /**
     * Calculate MAX.
     */
    @SuppressWarnings("unchecked")
    public static Object max(List<Object> values) {
        if (values == null || values.isEmpty()) return null;

        Object max = null;

        for (Object value : values) {
            if (value != null) {
                if (max == null) {
                    max = value;
                } else if (value instanceof Comparable && max instanceof Comparable) {
                    try {
                        if (((Comparable<Object>) value).compareTo(max) > 0) {
                            max = value;
                        }
                    } catch (ClassCastException e) {
                        // Different types, compare as strings
                        if (value.toString().compareTo(max.toString()) > 0) {
                            max = value;
                        }
                    }
                }
            }
        }

        return max;
    }
}