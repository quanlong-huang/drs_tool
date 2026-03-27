package com.csvsql.parser.parser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csvsql.parser.error.SqlSyntaxException;
import com.csvsql.parser.error.UnsupportedFeatureException;

/**
 * SQL parser that converts SQL strings into executable query plans.
 *
 * <p>SqlParser uses JSqlParser to parse MySQL-compatible SELECT statements
 * and produces a {@link QueryPlan} that can be executed by the
 * {@link com.csvsql.parser.engine.QueryExecutor}.</p>
 *
 * <p>Supported SQL features:</p>
 * <ul>
 *   <li>SELECT with column lists, aliases, and expressions</li>
 *   <li>FROM clause with file paths as table names</li>
 *   <li>WHERE clause with comparison and logical operators</li>
 *   <li>JOIN (INNER, LEFT, RIGHT)</li>
 *   <li>GROUP BY with aggregate functions</li>
 *   <li>ORDER BY with ASC/DESC</li>
 *   <li>LIMIT and OFFSET</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * SqlParser parser = new SqlParser();
 * QueryPlan plan = parser.parse("SELECT * FROM employees WHERE salary > 50000");
 * </pre>
 *
 * @see QueryPlan
 * @see QueryPlanBuilder
 * @see com.csvsql.parser.engine.QueryExecutor
 */
public class SqlParser {

    private static final Logger logger = LoggerFactory.getLogger(SqlParser.class);

    private final SupportedFeatures supportedFeatures;
    private final QueryPlanBuilder queryPlanBuilder;

    /**
     * Creates a new SqlParser instance.
     */
    public SqlParser() {
        this.supportedFeatures = new SupportedFeatures();
        this.queryPlanBuilder = new QueryPlanBuilder();
    }

    /**
     * Parses a SQL string and returns a query plan.
     *
     * <p>The parser validates SQL syntax and supported features before
     * building the execution plan.</p>
     *
     * @param sql the SQL string to parse
     * @return the parsed query plan
     * @throws SqlSyntaxException         if the SQL syntax is invalid
     * @throws UnsupportedFeatureException if an unsupported SQL feature is used
     */
    public QueryPlan parse(String sql) throws SqlSyntaxException, UnsupportedFeatureException {
        logger.debug("Parsing SQL: {}", sql);

        try {
            // Preprocess SQL to handle file paths with special characters
            String preprocessedSql = preprocessFilePaths(sql);
            logger.debug("Preprocessed SQL: {}", preprocessedSql);

            Statement statement = CCJSqlParserUtil.parse(preprocessedSql);

            if (!(statement instanceof Select)) {
                throw new UnsupportedFeatureException(
                    statement.getClass().getSimpleName(),
                    "statement type",
                    "Only SELECT statements are supported. INSERT, UPDATE, DELETE are not allowed."
                );
            }

            Select select = (Select) statement;

            // Validate supported features
            supportedFeatures.validate(select);

            // Build query plan
            QueryPlan plan = queryPlanBuilder.build(select);

            logger.debug("Successfully parsed query plan: {}", plan);
            return plan;

        } catch (JSQLParserException e) {
            Throwable cause = e.getCause();
            String message = cause != null ? cause.getMessage() : e.getMessage();

            logger.error("SQL syntax error: {}", message);
            throw new SqlSyntaxException(message, sql);
        }
    }

    /**
     * Preprocesses SQL to handle file paths that need to be quoted.
     *
     * <p>JSqlParser interprets '/' as division operator, so file paths like
     * '/tmp/data.csv' need to be wrapped in backticks for proper parsing.
     * This method automatically quotes:</p>
     * <ul>
     *   <li>File paths starting with '/' (absolute paths)</li>
     *   <li>File paths starting with './' (relative paths)</li>
     *   <li>File paths containing '/' after FROM or JOIN</li>
     * </ul>
     *
     * @param sql the original SQL string
     * @return the preprocessed SQL with quoted file paths
     */
    private String preprocessFilePaths(String sql) {
        // Pattern to match file paths after FROM or JOIN keywords
        // Matches: FROM /path/to/file.csv or JOIN /path/to/file.csv
        // Also matches paths followed by AS or alias

        StringBuilder result = new StringBuilder();
        String upperSql = sql.toUpperCase();

        int i = 0;
        while (i < sql.length()) {
            // Check for FROM or JOIN keyword
            int fromIdx = indexOfKeyword(upperSql, "FROM", i);
            int joinIdx = indexOfKeyword(upperSql, "JOIN", i);

            int nextKeywordIdx = -1;
            String keyword = null;

            if (fromIdx != -1 && (joinIdx == -1 || fromIdx < joinIdx)) {
                nextKeywordIdx = fromIdx;
                keyword = "FROM";
            } else if (joinIdx != -1) {
                nextKeywordIdx = joinIdx;
                keyword = "JOIN";
            }

            if (nextKeywordIdx == -1) {
                // No more FROM or JOIN keywords, append rest of SQL
                result.append(sql.substring(i));
                break;
            }

            // Append everything before the keyword
            result.append(sql.substring(i, nextKeywordIdx));
            result.append(keyword);

            int afterKeyword = nextKeywordIdx + keyword.length();

            // Skip whitespace
            int pathStart = afterKeyword;
            while (pathStart < sql.length() && Character.isWhitespace(sql.charAt(pathStart))) {
                result.append(sql.charAt(pathStart));
                pathStart++;
            }

            // Check if already quoted
            if (pathStart < sql.length()) {
                char firstChar = sql.charAt(pathStart);
                if (firstChar == '"' || firstChar == '`' || firstChar == '\'') {
                    // Already quoted, find end and copy as-is
                    int pathEnd = pathStart + 1;
                    while (pathEnd < sql.length() && sql.charAt(pathEnd) != firstChar) {
                        pathEnd++;
                    }
                    if (pathEnd < sql.length()) {
                        pathEnd++; // Include closing quote
                    }
                    result.append(sql.substring(pathStart, pathEnd));
                    i = pathEnd;
                    continue;
                }
            }

            // Find the end of the table/file name
            int pathEnd = pathStart;
            while (pathEnd < sql.length()) {
                char c = sql.charAt(pathEnd);
                if (Character.isWhitespace(c) || c == ',' || c == ';' ||
                    c == ')' || c == '(' || c == '+' || c == '-' ||
                    c == '*' || c == '/') {
                    // Check if this is a path separator '/' or an operator
                    if (c == '/' && pathEnd > pathStart) {
                        // This could be part of a path, continue
                        // But need to check if it's actually a path character
                        // For paths like /tmp/file.csv, the first char is '/'
                        // For paths like folder/file.csv, we're in the middle
                        pathEnd++;
                        continue;
                    }
                    break;
                }
                pathEnd++;
            }

            String tableName = sql.substring(pathStart, pathEnd);

            // Check if this looks like a file path (contains / or starts with /)
            if (tableName.contains("/") || tableName.startsWith("./") ||
                (pathStart < sql.length() && sql.charAt(pathStart) == '/')) {
                // Need to re-parse from pathStart because '/' is an operator
                // Re-scan to get the actual path
                pathEnd = pathStart;
                boolean inPath = false;

                // Handle paths starting with /
                if (pathStart < sql.length() && sql.charAt(pathStart) == '/') {
                    inPath = true;
                    pathEnd++;
                }

                // Continue reading path
                while (pathEnd < sql.length()) {
                    char c = sql.charAt(pathEnd);
                    if (Character.isWhitespace(c) || c == ',' || c == ';' ||
                        c == ')' || c == '(') {
                        break;
                    }
                    // Stop at ON, WHERE, ORDER, GROUP, LIMIT, etc.
                    if (Character.isWhitespace(c)) {
                        // Check if next word is a keyword
                        int tempEnd = pathEnd + 1;
                        while (tempEnd < sql.length() && Character.isWhitespace(sql.charAt(tempEnd))) {
                            tempEnd++;
                        }
                        String rest = sql.substring(tempEnd).toUpperCase();
                        if (rest.startsWith("ON ") || rest.startsWith("WHERE ") ||
                            rest.startsWith("ORDER ") || rest.startsWith("GROUP ") ||
                            rest.startsWith("LIMIT ") || rest.startsWith("OFFSET ") ||
                            rest.startsWith("HAVING ") || rest.startsWith("JOIN ") ||
                            rest.startsWith("INNER ") || rest.startsWith("LEFT ") ||
                            rest.startsWith("RIGHT ")) {
                            break;
                        }
                    }
                    pathEnd++;
                }

                // Check for optional alias (AS alias or just alias)
                // We need to be careful not to include alias in the quoted path
                int actualPathEnd = pathEnd;
                String pathStr = sql.substring(pathStart, pathEnd);

                // Check if path ends with a known file extension followed by alias
                // e.g., /tmp/orders.csv o or /tmp/orders.csv AS o
                String[] parts = pathStr.split("\\s+");
                if (parts.length > 1) {
                    // Check if last part is likely an alias
                    String lastPart = parts[parts.length - 1];
                    String pathOnly = pathStr.substring(0, pathStr.length() - lastPart.length()).trim();

                    // If pathOnly looks like a file path, quote it and add alias separately
                    if (pathOnly.contains("/") || pathOnly.endsWith(".csv") ||
                        pathOnly.endsWith(".tsv") || pathOnly.endsWith(".txt")) {
                        // Check if second to last is AS
                        if (parts.length > 2 && parts[parts.length - 2].equalsIgnoreCase("AS")) {
                            // path AS alias
                            pathOnly = pathStr.substring(0, pathStr.length() - lastPart.length() - 3).trim();
                            result.append("`").append(pathOnly).append("` AS ").append(lastPart);
                            i = pathEnd;
                            continue;
                        } else {
                            // path alias (without AS)
                            result.append("`").append(pathOnly).append("` ").append(lastPart);
                            i = pathEnd;
                            continue;
                        }
                    }
                }

                // Quote the path
                result.append("`").append(pathStr).append("`");
                i = pathEnd;
            } else {
                // Regular table name, copy as-is
                result.append(tableName);
                i = pathEnd;
            }
        }

        return result.toString();
    }

    /**
     * Finds the index of a keyword in the SQL string, starting from a given position.
     * The keyword must be preceded and followed by whitespace or special characters.
     */
    private int indexOfKeyword(String upperSql, String keyword, int startPos) {
        int idx = upperSql.indexOf(keyword, startPos);
        while (idx != -1) {
            // Check if it's a standalone keyword (not part of another word)
            boolean validPrefix = (idx == 0 || !Character.isLetterOrDigit(upperSql.charAt(idx - 1)));
            boolean validSuffix = (idx + keyword.length() >= upperSql.length() ||
                !Character.isLetterOrDigit(upperSql.charAt(idx + keyword.length())));
            if (validPrefix && validSuffix) {
                return idx;
            }
            idx = upperSql.indexOf(keyword, idx + 1);
        }
        return -1;
    }

    /**
     * Validates a SQL string without returning a plan.
     *
     * @param sql the SQL string to validate
     * @return true if the SQL is valid and supported, false otherwise
     */
    public boolean isValid(String sql) {
        try {
            parse(sql);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}