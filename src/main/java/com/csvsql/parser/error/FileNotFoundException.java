package com.csvsql.parser.error;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when a CSV file is not found.
 *
 * <p>This exception is thrown when attempting to load a CSV file that does
 * not exist or cannot be read. It can provide suggestions for similar file
 * paths.</p>
 *
 * <p>Example error message:</p>
 * <pre>
 * Error: File not found: data/emploees.csv
 *
 * Did you mean:
 *   - data/employees.csv
 * </pre>
 *
 * @see CsvSqlException
 * @see com.csvsql.parser.data.CsvLoader
 */
public class FileNotFoundException extends CsvSqlException {

    private final String filePath;
    private final List<String> suggestions;

    public FileNotFoundException(String filePath) {
        super("File not found: " + filePath);
        this.filePath = filePath;
        this.suggestions = new ArrayList<>();
    }

    public FileNotFoundException(String filePath, String suggestion) {
        super("File not found: " + filePath, suggestion);
        this.filePath = filePath;
        this.suggestions = new ArrayList<>();
        this.suggestions.add(suggestion);
    }

    public String getFilePath() {
        return filePath;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void addSuggestion(String suggestion) {
        suggestions.add(suggestion);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Error: File not found: ").append(filePath);

        if (!suggestions.isEmpty()) {
            sb.append("\n\nDid you mean:");
            for (String suggestion : suggestions) {
                sb.append("\n  - ").append(suggestion);
            }
        }

        return sb.toString();
    }
}