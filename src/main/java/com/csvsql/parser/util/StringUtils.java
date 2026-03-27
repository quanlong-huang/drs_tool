package com.csvsql.parser.util;

import java.util.Collection;

/**
 * Utility class for string operations.
 *
 * <p>StringUtils provides static methods for common string operations
 * including:</p>
 * <ul>
 *   <li>Null-safe emptiness checks</li>
 *   <li>Trimming and padding</li>
 *   <li>Joining and repeating</li>
 *   <li>Levenshtein distance calculation for similarity matching</li>
 *   <li>CSV escaping and unescaping</li>
 * </ul>
 *
 * <p>All methods are null-safe and handle edge cases gracefully.</p>
 */
public class StringUtils {

    /**
     * Check if a string is null or empty.
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Check if a string is null or blank (empty or whitespace only).
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Trim a string, returning null if the result is empty.
     */
    public static String trimToNull(String str) {
        if (str == null) return null;
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Trim a string, returning empty string if null.
     */
    public static String trimToEmpty(String str) {
        return str == null ? "" : str.trim();
    }

    /**
     * Join strings with a separator.
     */
    public static String join(Collection<String> strings, String separator) {
        if (strings == null || strings.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : strings) {
            if (!first) {
                sb.append(separator);
            }
            sb.append(s != null ? s : "");
            first = false;
        }
        return sb.toString();
    }

    /**
     * Join strings with a separator.
     */
    public static String join(String[] strings, String separator) {
        if (strings == null || strings.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(strings[i] != null ? strings[i] : "");
        }
        return sb.toString();
    }

    /**
     * Repeat a string n times.
     */
    public static String repeat(String str, int times) {
        if (str == null || times <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * Pad a string on the right.
     */
    public static String padRight(String str, int length) {
        if (str == null) str = "";
        if (str.length() >= length) return str.substring(0, length);
        return str + repeat(" ", length - str.length());
    }

    /**
     * Pad a string on the left.
     */
    public static String padLeft(String str, int length) {
        if (str == null) str = "";
        if (str.length() >= length) return str.substring(0, length);
        return repeat(" ", length - str.length()) + str;
    }

    /**
     * Truncate a string with ellipsis if too long.
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        if (maxLength <= 3) return str.substring(0, maxLength);
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Calculate Levenshtein distance between two strings.
     */
    public static int levenshteinDistance(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";

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

    /**
     * Check if two strings are similar (within threshold).
     */
    public static boolean isSimilar(String a, String b, int threshold) {
        return levenshteinDistance(a, b) <= threshold;
    }

    /**
     * Escape special characters for CSV.
     */
    public static String escapeCsv(String str) {
        if (str == null) return "";
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    /**
     * Unescape CSV string.
     */
    public static String unescapeCsv(String str) {
        if (str == null) return "";
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1).replace("\"\"", "\"");
        }
        return str;
    }
}