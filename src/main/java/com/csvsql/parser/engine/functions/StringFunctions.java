package com.csvsql.parser.engine.functions;

/**
 * Implementation of SQL string functions.
 *
 * <p>StringFunctions provides static methods for string manipulation operations
 * commonly used in SQL queries:</p>
 * <ul>
 *   <li>Case conversion: UPPER, LOWER</li>
 *   <li>Length and position: LENGTH, INSTR</li>
 *   <li>Concatenation and substring: CONCAT, SUBSTRING</li>
 *   <li>Trimming: TRIM, LTRIM, RTRIM</li>
 *   <li>Padding: LPAD, RPAD</li>
 *   <li>Replacement: REPLACE</li>
 *   <li>Reversal: REVERSE</li>
 * </ul>
 *
 * <p>Note: SQL string functions use 1-based indexing for position parameters.</p>
 *
 * @see com.csvsql.parser.engine.ExpressionEvaluator
 */
public class StringFunctions {

    /**
     * Convert to uppercase.
     */
    public static String upper(String str) {
        return str != null ? str.toUpperCase() : null;
    }

    /**
     * Convert to lowercase.
     */
    public static String lower(String str) {
        return str != null ? str.toLowerCase() : null;
    }

    /**
     * Get string length.
     */
    public static Integer length(String str) {
        return str != null ? str.length() : null;
    }

    /**
     * Concatenate strings.
     */
    public static String concat(String... strings) {
        if (strings == null) return null;

        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            if (s != null) {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    /**
     * Extract substring.
     * Note: SQL uses 1-based indexing.
     */
    public static String substring(String str, int start) {
        if (str == null) return null;
        // SQL uses 1-based indexing
        int idx = start - 1;
        if (idx < 0) idx = 0;
        if (idx >= str.length()) return "";
        return str.substring(idx);
    }

    /**
     * Extract substring with length.
     * Note: SQL uses 1-based indexing.
     */
    public static String substring(String str, int start, int length) {
        if (str == null) return null;
        // SQL uses 1-based indexing
        int idx = start - 1;
        if (idx < 0) idx = 0;
        if (idx >= str.length()) return "";
        int end = Math.min(idx + length, str.length());
        return str.substring(idx, end);
    }

    /**
     * Trim whitespace from both ends.
     */
    public static String trim(String str) {
        return str != null ? str.trim() : null;
    }

    /**
     * Trim whitespace from left.
     */
    public static String ltrim(String str) {
        if (str == null) return null;
        int start = 0;
        while (start < str.length() && Character.isWhitespace(str.charAt(start))) {
            start++;
        }
        return str.substring(start);
    }

    /**
     * Trim whitespace from right.
     */
    public static String rtrim(String str) {
        if (str == null) return null;
        int end = str.length();
        while (end > 0 && Character.isWhitespace(str.charAt(end - 1))) {
            end--;
        }
        return str.substring(0, end);
    }

    /**
     * Replace substring.
     */
    public static String replace(String str, String search, String replace) {
        if (str == null) return null;
        if (search == null || search.isEmpty()) return str;
        return str.replace(search, replace != null ? replace : "");
    }

    /**
     * Find position of substring.
     * Note: SQL uses 1-based indexing, returns 0 if not found.
     */
    public static Integer instr(String str, String search) {
        if (str == null || search == null) return null;
        int idx = str.indexOf(search);
        return idx >= 0 ? idx + 1 : 0;
    }

    /**
     * Reverse string.
     */
    public static String reverse(String str) {
        if (str == null) return null;
        return new StringBuilder(str).reverse().toString();
    }

    /**
     * Left pad string.
     */
    public static String lpad(String str, int length, String pad) {
        if (str == null) return null;
        if (str.length() >= length) return str.substring(0, length);
        String padStr = pad != null ? pad : " ";
        StringBuilder sb = new StringBuilder();
        int padLength = length - str.length();
        while (sb.length() < padLength) {
            sb.append(padStr);
        }
        return sb.substring(0, padLength) + str;
    }

    /**
     * Right pad string.
     */
    public static String rpad(String str, int length, String pad) {
        if (str == null) return null;
        if (str.length() >= length) return str.substring(0, length);
        String padStr = pad != null ? pad : " ";
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(padStr);
        }
        return sb.substring(0, length);
    }
}