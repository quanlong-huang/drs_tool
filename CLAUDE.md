# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

```bash
# Build project (skip tests)
mvn clean package -DskipTests

# Build with tests
mvn clean package

# Run all tests
mvn test

# Run single test class
mvn test -Dtest=SqlParserTest

# Run single test method
mvn test -Dtest=QueryExecutorTest#testSimpleSelect

# Generate test coverage report
mvn test jacoco:report
# Report: target/site/jacoco/index.html

# Generate Javadoc
mvn javadoc:javadoc
# Output: target/site/apidocs/
```

## Running the Application

```bash
# Build first
mvn clean package -DskipTests

# Query mode
java -jar target/csv-sql-parser-1.0.0-SNAPSHOT.jar query "SELECT * FROM data.csv LIMIT 10"

# REPL mode
java -jar target/csv-sql-parser-1.0.0-SNAPSHOT.jar repl

# Batch mode
java -jar target/csv-sql-parser-1.0.0-SNAPSHOT.jar batch queries.sql
```

## Architecture Overview

This is a CLI tool for querying CSV files using MySQL-compatible SQL syntax. The codebase follows a layered architecture:

### Layers (top to bottom)

1. **CLI Layer** (`com.csvsql.parser.cli`) - Command routing via Picocli
   - `MainCommand` → routes to `QueryCommand`, `ReplCommand`, `BatchCommand`
   - `ReplRunner` - Interactive REPL session

2. **Parser Layer** (`com.csvsql.parser.parser`) - SQL parsing
   - `SqlParser` - Entry point, uses JSqlParser
   - `QueryPlanBuilder` - Converts parsed SQL to `QueryPlan`
   - `SupportedFeatures` - Validates supported SQL features
   - `QueryPlan` - Intermediate representation with tables, columns, joins, conditions

3. **Engine Layer** (`com.csvsql.parser.engine`) - Query execution
   - `QueryExecutor` - Coordinator, executes pipeline stages in order:
     1. Load tables → 2. WHERE → 3. JOINs → 4. GROUP BY → 5. HAVING → 6. SELECT → 7. ORDER BY → 8. LIMIT/OFFSET
   - `SelectExecutor` - Column projection, sorting, pagination
   - `JoinExecutor` - JOIN operations (INNER, LEFT, RIGHT) with HashMap index
   - `WhereEvaluator` - WHERE/HAVING condition evaluation
   - `GroupByExecutor` - GROUP BY and aggregation
   - `ExpressionEvaluator` - CASE WHEN, COALESCE, CAST, arithmetic, functions

4. **Data Layer** (`com.csvsql.parser.data`) - CSV handling
   - `CsvLoader` - Loads CSV with encoding detection, type inference
   - `TypeInferer` - Infers INTEGER, LONG, DOUBLE, DATE, BOOLEAN, STRING
   - `TableRegistry` - Manages loaded tables with alias support
   - `CsvTable` - In-memory table structure

5. **Output Layer** (`com.csvsql.parser.output`) - Result formatting
   - `OutputFormatter` interface → `TableFormatter`, `CsvFormatter`, `JsonFormatter`

### Key Data Flow

```
SQL String → SqlParser → QueryPlan → QueryExecutor → ResultSet → OutputFormatter
```

### Important Patterns

- **QueryPlan**: Central intermediate representation. Contains `TableReference`, `ColumnReference`, `JoinClause`, `FilterCondition`
- **Pipeline execution**: `QueryExecutor.execute()` runs stages sequentially
- **Table aliases**: Tables registered by alias in `TableRegistry`, lookups must try alias first then full name
- **File path handling**: JSqlParser interprets `/` as division. `SqlParser.preprocessFilePaths()` auto-quotes paths like `/tmp/file.csv` → `` `/tmp/file.csv` ``
- **Type inference order**: INTEGER → LONG → DOUBLE → DATE → BOOLEAN → STRING

## Tech Stack

- Java 8 (must maintain compatibility)
- JSqlParser 4.6 - SQL parsing
- Apache Commons CSV 1.10.0 - CSV handling
- Picocli 4.7.5 - CLI framework
- Jackson 2.15.2 - JSON output
- JUnit 5 + Mockito - Testing

## SQL Features

**Supported**: SELECT, FROM, WHERE, JOIN (INNER/LEFT/RIGHT), GROUP BY, HAVING, ORDER BY, LIMIT/OFFSET, aggregate functions (COUNT, SUM, AVG, MIN, MAX), string/date/math functions, CASE WHEN, COALESCE, CAST, IN/NOT IN, LIKE

**Not Supported**: INSERT/UPDATE/DELETE, subqueries, UNION, window functions, CTEs

## Code Style

- Comprehensive Javadoc on all public classes and methods
- Use `LinkedHashMap` for row data to preserve column order
- Check `isFunction()` before `hasExpression()` in projection (aggregate functions have both flags)
- Use `getFullyQualifiedName()` for table names with dots (e.g., `orders.csv`)