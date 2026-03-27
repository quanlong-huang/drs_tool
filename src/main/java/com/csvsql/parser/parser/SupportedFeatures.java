package com.csvsql.parser.parser;

import net.sf.jsqlparser.statement.select.*;
import com.csvsql.parser.error.UnsupportedFeatureException;

/**
 * Validates that only supported SQL features are used.
 *
 * <p>SupportedFeatures checks a parsed SELECT statement against the list of
 * supported SQL features and throws an exception if unsupported features are
 * detected. This provides clear error messages for users attempting to use
 * advanced SQL features.</p>
 *
 * <p><b>Supported features:</b></p>
 * <ul>
 *   <li>SELECT with column list, *, and aliases</li>
 *   <li>FROM with table (CSV file) references</li>
 *   <li>WHERE with comparison and logical operators</li>
 *   <li>JOIN (INNER, LEFT, RIGHT) with ON clause</li>
 *   <li>GROUP BY with HAVING</li>
 *   <li>ORDER BY with ASC/DESC</li>
 *   <li>LIMIT and OFFSET</li>
 *   <li>Aggregate, string, date, and math functions</li>
 * </ul>
 *
 * <p><b>Unsupported features:</b></p>
 * <ul>
 *   <li>INSERT, UPDATE, DELETE statements</li>
 *   <li>Subqueries in FROM or WHERE</li>
 *   <li>UNION, INTERSECT, EXCEPT</li>
 *   <li>Window functions</li>
 *   <li>Common Table Expressions (WITH clause)</li>
 * </ul>
 *
 * @see SqlParser
 * @see UnsupportedFeatureException
 */
public class SupportedFeatures {

    /**
     * Validate a SELECT statement for supported features.
     *
     * @param select the SELECT statement to validate
     * @throws UnsupportedFeatureException if an unsupported feature is used
     */
    public void validate(Select select) {
        SelectBody selectBody = select.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            throw new UnsupportedFeatureException(
                selectBody.getClass().getSimpleName(),
                "SELECT type",
                "Only simple SELECT statements are supported. UNION, INTERSECT, and subqueries are not supported."
            );
        }

        PlainSelect plainSelect = (PlainSelect) selectBody;

        // Check for subqueries in FROM
        if (plainSelect.getFromItem() instanceof SubSelect) {
            throw new UnsupportedFeatureException(
                "Subquery in FROM clause",
                "query structure",
                "Subqueries are not supported. Use JOIN instead."
            );
        }

        // Check for subqueries in JOINs
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                if (join.getRightItem() instanceof SubSelect) {
                    throw new UnsupportedFeatureException(
                        "Subquery in JOIN",
                        "query structure",
                        "Subqueries are not supported in JOINs."
                    );
                }
            }
        }

        // Check for WITH clause (CTE)
        if (select.getWithItemsList() != null && !select.getWithItemsList().isEmpty()) {
            throw new UnsupportedFeatureException(
                "WITH clause (CTE)",
                "query structure",
                "Common Table Expressions (CTE) are not supported."
            );
        }
    }

    /**
     * Get a description of all supported SQL features.
     */
    public String getSupportedFeaturesDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Supported SQL Features:\n");
        sb.append("  - SELECT with column list, *, and aliases\n");
        sb.append("  - FROM with table (CSV file) references\n");
        sb.append("  - WHERE with comparison operators (=, !=, <, >, <=, >=)\n");
        sb.append("  - WHERE with logical operators (AND, OR, NOT)\n");
        sb.append("  - ORDER BY with ASC/DESC\n");
        sb.append("  - GROUP BY with HAVING\n");
        sb.append("  - LIMIT and OFFSET\n");
        sb.append("  - JOIN (INNER, LEFT, RIGHT) with ON clause\n");
        sb.append("  - Column and table aliases (AS keyword)\n");
        sb.append("  - Aggregate functions: COUNT, SUM, AVG, MIN, MAX\n");
        sb.append("  - String functions: CONCAT, UPPER, LOWER, LENGTH, SUBSTRING, TRIM\n");
        sb.append("  - Date functions: YEAR, MONTH, DAY, DATE_FORMAT\n");
        sb.append("  - Math functions: ROUND, CEILING, FLOOR, ABS\n");
        return sb.toString();
    }

    /**
     * Get a description of unsupported SQL features.
     */
    public String getUnsupportedFeaturesDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Unsupported SQL Features:\n");
        sb.append("  - INSERT, UPDATE, DELETE statements\n");
        sb.append("  - Subqueries\n");
        sb.append("  - UNION, INTERSECT, EXCEPT\n");
        sb.append("  - Window functions\n");
        sb.append("  - Common Table Expressions (WITH clause)\n");
        sb.append("  - Stored procedures and triggers\n");
        sb.append("  - Views\n");
        return sb.toString();
    }
}