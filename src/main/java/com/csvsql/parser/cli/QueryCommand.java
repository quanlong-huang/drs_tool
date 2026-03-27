package com.csvsql.parser.cli;

import com.csvsql.parser.data.TableRegistry;
import com.csvsql.parser.output.*;
import com.csvsql.parser.parser.QueryPlan;
import com.csvsql.parser.parser.SqlParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Callable;

/**
 * Query command for executing SQL queries against CSV files.
 *
 * <p>This command executes a single SQL query and outputs the results in the
 * specified format. It supports various output formats and configuration options.</p>
 *
 * <p>Usage examples:</p>
 * <pre>
 * # Basic query
 * csv-sql query "SELECT * FROM employees.csv WHERE salary > 50000"
 *
 * # With JSON output
 * csv-sql query "SELECT name, salary FROM employees.csv" -f json
 *
 * # Save to file
 * csv-sql query "SELECT * FROM data.csv" -o output.csv -f csv
 *
 * # With custom delimiter
 * csv-sql query "SELECT * FROM data.csv" -d ";"
 * </pre>
 *
 * @see MainCommand
 * @see com.csvsql.parser.parser.SqlParser
 * @see com.csvsql.parser.engine.QueryExecutor
 */
@Command(
    name = "query",
    description = "Execute a SQL query against CSV files",
    mixinStandardHelpOptions = true
)
public class QueryCommand implements Callable<Integer> {

    @Parameters(
        index = "0",
        description = "SQL query to execute"
    )
    private String sql;

    @Option(
        names = {"-f", "--format"},
        description = "Output format: table, csv, json (default: table)",
        defaultValue = "table"
    )
    private String format;

    @Option(
        names = {"-o", "--output"},
        description = "Output file path (default: stdout)"
    )
    private String outputFile;

    @Option(
        names = {"-d", "--delimiter"},
        description = "CSV delimiter character (default: ,)",
        defaultValue = ","
    )
    private String delimiter;

    @Option(
        names = {"-e", "--encoding"},
        description = "File encoding (default: UTF-8)",
        defaultValue = "UTF-8"
    )
    private String encoding;

    @Option(
        names = {"--no-header"},
        description = "CSV file has no header row"
    )
    private boolean noHeader;

    @Option(
        names = {"-v", "--verbose"},
        description = "Show execution details"
    )
    private boolean verbose;

    @Override
    public Integer call() {
        try {
            // Initialize components
            TableRegistry tableRegistry = new TableRegistry();
            SqlParser parser = new SqlParser();
            com.csvsql.parser.engine.QueryExecutor executor =
                new com.csvsql.parser.engine.QueryExecutor(tableRegistry);

            // Parse the SQL
            if (verbose) {
                System.err.println("Parsing SQL: " + sql);
            }
            QueryPlan plan = parser.parse(sql);

            // Execute the query
            if (verbose) {
                System.err.println("Executing query plan: " + plan);
            }
            ResultSet result = executor.execute(plan);

            // Format and output results
            OutputFormatter formatter = getFormatter(format);
            String output = formatter.format(result);

            // Write output
            if (outputFile != null && !outputFile.isEmpty()) {
                writeToFile(output, outputFile);
                if (verbose) {
                    System.err.println("Output written to: " + outputFile);
                }
            } else {
                System.out.print(output);
            }

            return 0;

        } catch (com.csvsql.parser.error.SqlSyntaxException e) {
            System.err.println("SQL Syntax Error: " + e.getMessage());
            if (e.hasSuggestion()) {
                System.err.println("Suggestion: " + e.getSuggestion());
            }
            return 1;
        } catch (com.csvsql.parser.error.FileNotFoundException e) {
            System.err.println(e.toString());
            return 1;
        } catch (com.csvsql.parser.error.UnsupportedFeatureException e) {
            System.err.println(e.toString());
            return 1;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private OutputFormatter getFormatter(String format) {
        switch (format.toLowerCase()) {
            case "csv":
                return new CsvFormatter();
            case "json":
                return new JsonFormatter();
            case "table":
            default:
                return new TableFormatter();
        }
    }

    private void writeToFile(String content, String filePath) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.print(content);
        }
    }

    // Getters for testing
    public String getSql() {
        return sql;
    }

    public String getFormat() {
        return format;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public String getEncoding() {
        return encoding;
    }

    public boolean isNoHeader() {
        return noHeader;
    }

    public boolean isVerbose() {
        return verbose;
    }
}