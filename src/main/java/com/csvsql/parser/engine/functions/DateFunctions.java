package com.csvsql.parser.engine.functions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Implementation of SQL date functions.
 *
 * <p>DateFunctions provides static methods for date manipulation operations
 * commonly used in SQL queries:</p>
 * <ul>
 *   <li>Extraction: YEAR, MONTH, DAY, DAYOFWEEK, DAYOFYEAR, WEEK, QUARTER</li>
 *   <li>Formatting: DATE_FORMAT</li>
 *   <li>Arithmetic: DATE_ADD, DATE_SUB, DATEDIFF</li>
 *   <li>Current: CURRENT_DATE</li>
 * </ul>
 *
 * <p>The class supports multiple date input formats including:</p>
 * <ul>
 *   <li>java.time.LocalDate</li>
 *   <li>java.sql.Date</li>
 *   <li>java.util.Date</li>
 *   <li>String in various formats (yyyy-MM-dd, MM/dd/yyyy, etc.)</li>
 * </ul>
 *
 * @see com.csvsql.parser.engine.ExpressionEvaluator
 */
public class DateFunctions {

    private static final DateTimeFormatter DEFAULT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Extract year from date.
     */
    public static Integer year(Object date) {
        LocalDate localDate = toDate(date);
        return localDate != null ? localDate.getYear() : null;
    }

    /**
     * Extract month from date.
     */
    public static Integer month(Object date) {
        LocalDate localDate = toDate(date);
        return localDate != null ? localDate.getMonthValue() : null;
    }

    /**
     * Extract day from date.
     */
    public static Integer day(Object date) {
        LocalDate localDate = toDate(date);
        return localDate != null ? localDate.getDayOfMonth() : null;
    }

    /**
     * Extract day of week (1=Sunday, 7=Saturday in MySQL).
     */
    public static Integer dayOfWeek(Object date) {
        LocalDate localDate = toDate(date);
        if (localDate == null) return null;
        // Java uses 1=Monday, 7=Sunday, MySQL uses 1=Sunday, 7=Saturday
        int javaDay = localDate.getDayOfWeek().getValue();
        return javaDay == 7 ? 1 : javaDay + 1;
    }

    /**
     * Extract day of year.
     */
    public static Integer dayOfYear(Object date) {
        LocalDate localDate = toDate(date);
        return localDate != null ? localDate.getDayOfYear() : null;
    }

    /**
     * Extract week of year.
     */
    public static Integer week(Object date) {
        LocalDate localDate = toDate(date);
        if (localDate == null) return null;
        return localDate.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
    }

    /**
     * Extract quarter.
     */
    public static Integer quarter(Object date) {
        LocalDate localDate = toDate(date);
        if (localDate == null) return null;
        return (localDate.getMonthValue() - 1) / 3 + 1;
    }

    /**
     * Format date as string.
     */
    public static String dateFormat(Object date, String format) {
        LocalDate localDate = toDate(date);
        if (localDate == null) return null;

        if (format == null || format.isEmpty()) {
            return localDate.format(DEFAULT_FORMAT);
        }

        // Convert MySQL format to Java format
        String javaFormat = convertMySqlFormatToJava(format);
        try {
            return localDate.format(DateTimeFormatter.ofPattern(javaFormat));
        } catch (IllegalArgumentException e) {
            return localDate.format(DEFAULT_FORMAT);
        }
    }

    /**
     * Convert MySQL date format to Java DateTimeFormatter format.
     */
    private static String convertMySqlFormatToJava(String mysqlFormat) {
        return mysqlFormat
            .replace("%Y", "yyyy")
            .replace("%y", "yy")
            .replace("%m", "MM")
            .replace("%c", "M")
            .replace("%d", "dd")
            .replace("%e", "d")
            .replace("%H", "HH")
            .replace("%h", "hh")
            .replace("%i", "mm")
            .replace("%s", "ss")
            .replace("%W", "EEEE")
            .replace("%a", "EEE")
            .replace("%M", "MMMM")
            .replace("%b", "MMM");
    }

    /**
     * Parse object to LocalDate.
     */
    private static LocalDate toDate(Object obj) {
        if (obj == null) return null;

        if (obj instanceof LocalDate) {
            return (LocalDate) obj;
        }

        if (obj instanceof java.sql.Date) {
            return ((java.sql.Date) obj).toLocalDate();
        }

        if (obj instanceof java.util.Date) {
            return ((java.util.Date) obj).toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
        }

        if (obj instanceof String) {
            String str = ((String) obj).trim();
            try {
                return LocalDate.parse(str, DEFAULT_FORMAT);
            } catch (DateTimeParseException e) {
                // Try other common formats
                String[] formats = {
                    "MM/dd/yyyy", "dd-MM-yyyy", "yyyy/MM/dd",
                    "MM-dd-yyyy", "dd/MM/yyyy"
                };
                for (String fmt : formats) {
                    try {
                        return LocalDate.parse(str, DateTimeFormatter.ofPattern(fmt));
                    } catch (DateTimeParseException ignored) {
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get current date.
     */
    public static LocalDate currentDate() {
        return LocalDate.now();
    }

    /**
     * Add days to date.
     */
    public static LocalDate dateAdd(Object date, long days) {
        LocalDate localDate = toDate(date);
        return localDate != null ? localDate.plusDays(days) : null;
    }

    /**
     * Subtract days from date.
     */
    public static LocalDate dateSub(Object date, long days) {
        return dateAdd(date, -days);
    }

    /**
     * Calculate days between two dates.
     */
    public static Long datediff(Object date1, Object date2) {
        LocalDate d1 = toDate(date1);
        LocalDate d2 = toDate(date2);
        if (d1 == null || d2 == null) return null;
        return (long) java.time.temporal.ChronoUnit.DAYS.between(d2, d1);
    }
}