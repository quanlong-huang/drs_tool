package com.csvsql.parser.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * REPL command for interactive SQL query execution.
 *
 * <p>This command starts an interactive Read-Eval-Print Loop (REPL) session
 * where users can execute multiple queries interactively. Features include:</p>
 * <ul>
 *   <li>Command history with navigation (up/down arrows)</li>
 *   <li>Tab completion for SQL keywords</li>
 *   <li>Persistent history saved to file</li>
 *   <li>Customizable prompt</li>
 * </ul>
 *
 * <p>Usage examples:</p>
 * <pre>
 * # Start REPL with default settings
 * csv-sql repl
 *
 * # With custom history file
 * csv-sql repl -H /path/to/history
 *
 * # With custom prompt
 * csv-sql repl --prompt "my-db> "
 * </pre>
 *
 * @see MainCommand
 * @see ReplRunner
 */
@Command(
    name = "repl",
    description = "Start interactive REPL session",
    mixinStandardHelpOptions = true
)
public class ReplCommand implements Callable<Integer> {

    @Option(
        names = {"-H", "--history"},
        description = "History file path (default: ~/.csv_sql_history)",
        defaultValue = "${user.home}/.csv_sql_history"
    )
    private String historyFile;

    @Option(
        names = {"--prompt"},
        description = "Custom prompt string (default: csv-sql>)",
        defaultValue = "csv-sql> "
    )
    private String prompt;

    @Override
    public Integer call() {
        try {
            ReplRunner runner = new ReplRunner(historyFile, prompt);
            runner.run();
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    // Getters for testing
    public String getHistoryFile() {
        return historyFile;
    }

    public String getPrompt() {
        return prompt;
    }
}