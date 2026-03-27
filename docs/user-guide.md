# CSV SQL Parser User Guide

## Introduction

CSV SQL Parser is a command-line tool that allows you to query CSV files using standard MySQL SQL syntax. It supports SELECT statements with WHERE clauses, JOINs, GROUP BY, ORDER BY, and various built-in functions.

## Installation

### Prerequisites
- Java 8 or higher
- Maven 3.6+ (for building from source)

### Building from Source

```bash
git clone <repository-url>
cd csv-sql-parser
mvn package -DskipTests
```

The compiled JAR file will be in `target/csv-sql-parser-1.0.0-SNAPSHOT.jar`.

## Quick Start

### Basic Query

```bash
java -jar csv-sql-parser-1.0.0-SNAPSHOT.jar query "SELECT * FROM 'data/employees.csv' LIMIT 10"
```

### Query with WHERE Clause

```bash
java -jar csv-sql-parser-1.0.0-SNAPSHOT.jar query "SELECT name, salary FROM 'data/employees.csv' WHERE salary > 50000"
```

### Using REPL Mode

```bash
java -jar csv-sql-parser-1.0.0-SNAPSHOT.jar repl
```

In REPL mode, you can enter queries interactively:

```
csv-sql> SELECT * FROM 'employees.csv' WHERE department = 'Engineering';
csv-sql> .help
csv-sql> .quit
```

## Command Line Options

### Query Command

```
csv-sql query [OPTIONS] <sql>

Execute a SQL query against CSV files

      <sql>               SQL query to execute
  -d, --delimiter=<delimiter>
                          CSV delimiter character (default: ,)
  -e, --encoding=<encoding>
                          File encoding (default: UTF-8)
  -f, --format=<format>   Output format: table, csv, json (default: table)
  -h, --help              Show this help message and exit.
      --no-header         CSV file has no header row
  -o, --output=<outputFile>
                          Output file path (default: stdout)
  -v, --verbose           Show execution details
```

### REPL Command

```
csv-sql repl

Start an interactive SQL shell

Available commands:
  .help                 Show help message
  .quit, .exit          Exit the REPL
  .version              Show version information
  .format <type>        Set output format (table, csv, json)
  .output <file>        Redirect output to file
  .tables               List loaded tables
  .schema <table>       Show table schema
  .load <file> [alias]  Load a CSV file as table
```

## Output Formats

### Table Format (default)

Displays results in a formatted ASCII table with borders:

```
┌─────────┬─────┬────────┐
│ name    │ age │ salary │
├─────────┼─────┼────────┤
│ Alice   │  30 │  80000 │
│ Bob     │  25 │  60000 │
└─────────┴─────┴────────┘
```

### CSV Format

Outputs results in CSV format:

```
name,age,salary
Alice,30,80000
Bob,25,60000
```

### JSON Format

Outputs results in JSON format:

```json
{
  "columns" : [ "name", "age", "salary" ],
  "rows" : [ {
    "name" : "Alice",
    "age" : 30,
    "salary" : 80000
  } ],
  "rowCount" : 1,
  "executionTimeMs" : 15
}
```

## Specifying File Paths

Use backticks or single quotes to specify file paths in SQL queries:

```sql
SELECT * FROM `/path/to/file.csv`
SELECT * FROM '/path/to/file.csv'
SELECT * FROM `C:\Users\Data\file.csv`
```

## Tips and Tricks

### Large Files

For large CSV files (> 50MB), the tool automatically uses chunked loading to reduce memory usage.

### Encoding Detection

The tool automatically detects file encoding (UTF-8, GBK, GB2312, ISO-8859-1). You can also specify encoding manually:

```bash
java -jar csv-sql-parser.jar query -e GBK "SELECT * FROM 'chinese_data.csv'"
```

### Delimiter Detection

Common delimiters (comma, semicolon, tab, pipe) are automatically detected. Specify manually if needed:

```bash
java -jar csv-sql-parser.jar query -d ';' "SELECT * FROM 'semicolon_file.csv'"
```

### Output Redirection

Save results to a file:

```bash
java -jar csv-sql-parser.jar query -o results.json -f json "SELECT * FROM 'data.csv'"
```

## Error Handling

The tool provides helpful error messages with suggestions:

```
Error: Column 'nmae' not found in table 'employees.csv'

Available columns:
  - id
  - name
  - age
  - salary

Did you mean: name?
```

## Getting Help

For more information:
- Use `.help` in REPL mode
- Use `-h` or `--help` with any command
- Check the SQL syntax reference for supported features