package com.csvsql.parser.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Main command for CSV SQL Parser CLI.
 *
 * <p>This is the root command that provides access to all subcommands:</p>
 * <ul>
 *   <li>{@code query} - Execute a single SQL query against CSV files</li>
 *   <li>{@code repl} - Start an interactive REPL session</li>
 *   <li>{@code batch} - Execute multiple queries from a file</li>
 * </ul>
 *
 * <p>Usage examples:</p>
 * <pre>
 * # Show help
 * java -jar csv-sql-parser.jar
 *
 * # Execute a query
 * java -jar csv-sql-parser.jar query "SELECT * FROM data.csv LIMIT 10"
 *
 * # Start REPL
 * java -jar csv-sql-parser.jar repl
 *
 * # Batch execution
 * java -jar csv-sql-parser.jar batch queries.sql
 * </pre>
 *
 * @see QueryCommand
 * @see ReplCommand
 * @see BatchCommand
 */
@Command(
    name = "csv-sql",
    description = "Query CSV files using MySQL SQL syntax",
    version = "CSV SQL Parser 1.0.0",
    subcommands = {
        QueryCommand.class,
        ReplCommand.class,
        BatchCommand.class
    },
    mixinStandardHelpOptions = true
)
public class MainCommand implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        // If no subcommand is specified, show help
        spec.commandLine().usage(System.out);
    }
}