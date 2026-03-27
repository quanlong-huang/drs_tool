package com.csvsql.parser;

import com.csvsql.parser.cli.MainCommand;
import picocli.CommandLine;

/**
 * Main entry point for the CSV SQL Parser application.
 *
 * <p>CSV SQL Parser is a command-line tool that enables querying CSV files
 * using standard MySQL SQL syntax. It supports SELECT statements with
 * WHERE clauses, JOINs, GROUP BY, ORDER BY, and various built-in functions.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * java -jar csv-sql-parser.jar query "SELECT * FROM 'data.csv'"
 * java -jar csv-sql-parser.jar repl
 * </pre>
 *
 * @author CSV SQL Parser Team
 * @version 1.0.0
 * @see com.csvsql.parser.cli.MainCommand
 * @see com.csvsql.parser.cli.QueryCommand
 * @see com.csvsql.parser.cli.ReplCommand
 */
public class CsvSqlParserApplication {

    /**
     * Main entry point for the application.
     *
     * <p>Initializes the command-line interface using Picocli and executes
     * the appropriate subcommand based on user input. The application supports
     * multiple subcommands including query, repl, and batch.</p>
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new MainCommand()).execute(args);
        System.exit(exitCode);
    }
}