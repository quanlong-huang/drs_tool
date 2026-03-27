# Performance Guide

This document provides guidance on optimizing query performance when using CSV SQL Parser.

## Memory Management

### Large File Handling

CSV SQL Parser automatically handles large files (> 50MB) using chunked loading to reduce memory consumption. Files smaller than 50MB are loaded entirely into memory for faster access.

### Chunked Loading

For files larger than 50MB:
- Data is read in chunks of 10,000 rows by default
- Type inference is performed on a sample of the first 1,000 rows
- Memory usage remains relatively constant regardless of file size

To manually enable chunked loading for smaller files:

```java
CsvLoader loader = new CsvLoader();
loader.setUseChunkedLoading(true);
loader.setChunkSize(5000);  // 5,000 rows per chunk
```

### Memory Monitoring

The tool includes memory monitoring utilities:

```java
MemoryUtils.logMemoryUsage("Before query");
// ... execute query ...
MemoryUtils.logMemoryUsage("After query");
```

## Query Optimization

### WHERE Clause Optimization

Filter early to reduce the number of rows processed:

```sql
-- Good: Filter first, then join
SELECT * FROM large_table l
JOIN small_table s ON l.id = s.id
WHERE l.status = 'active'

-- Less efficient: Join first, then filter
SELECT * FROM large_table l
JOIN small_table s ON l.id = s.id
WHERE s.category = 'A'
```

### Index Utilization

JOINs are optimized using hash indexes:
- The right table in a JOIN is indexed by the join column
- For best performance, join on columns with high cardinality
- Avoid joining on columns with many NULL values

### GROUP BY Optimization

When using GROUP BY:
- Columns in GROUP BY should be indexed-like (high cardinality for grouping, low for buckets)
- Use HAVING instead of WHERE for aggregate conditions

```sql
-- Good
SELECT department, AVG(salary)
FROM employees
GROUP BY department
HAVING AVG(salary) > 50000

-- Use WHERE for row-level filtering
SELECT department, AVG(salary)
FROM employees
WHERE status = 'active'
GROUP BY department
```

### LIMIT Usage

Use LIMIT to reduce result set size early:

```sql
-- Good: LIMIT reduces rows early
SELECT * FROM large_table ORDER BY created_date DESC LIMIT 100

-- This still scans the entire table for sorting
```

## Type Inference

### Sampling Size

Type inference uses a sample of 1,000 rows by default. For files with inconsistent data patterns, increase the sample size:

```java
CsvLoader loader = new CsvLoader();
loader.setSampleSize(5000);  // Sample 5,000 rows for type inference
```

### Type Performance

Column types affect comparison performance:
- **INTEGER**: Fast comparisons, optimal for numeric ranges
- **DOUBLE**: Good for decimals, slightly slower than INTEGER
- **STRING**: Slower comparisons, required for text data
- **BOOLEAN**: Fast for binary comparisons
- **LOCALDATE**: Efficient for date range queries

## File Handling

### Encoding Detection

Automatic encoding detection adds minimal overhead:
1. BOM (Byte Order Mark) check: O(1)
2. GBK/Chinese character check: O(n) where n is file size
3. UTF-8 validation: O(n)

For known encodings, specify explicitly to skip detection:

```bash
java -jar csv-sql-parser.jar query -e UTF-8 "SELECT * FROM 'data.csv'"
```

### Delimiter Detection

Delimiter detection scans only the first line, adding minimal overhead.

## Lazy Loading

TableRegistry supports lazy loading to defer file reading until actually needed:

```java
TableRegistry registry = new TableRegistry();
registry.setLazyLoadingEnabled(true);

// Schedule loading, but don't read file yet
registry.scheduleLazyLoad("large_table", "/path/to/large.csv", null);

// File is loaded only when table is accessed
CsvTable table = registry.getTable("large_table");
```

## Performance Benchmarks

### Typical Performance

| File Size | Rows | Query Time (SELECT *) |
|-----------|------|----------------------|
| 1 MB | 10,000 | ~50ms |
| 10 MB | 100,000 | ~300ms |
| 50 MB | 500,000 | ~1.5s |
| 100 MB | 1,000,000 | ~3s |

### Complex Query Performance

| Query Type | Rows | Time |
|------------|------|------|
| WHERE with index | 100,000 | ~100ms |
| GROUP BY | 100,000 | ~200ms |
| JOIN (hash) | 100,000 | ~300ms |
| ORDER BY | 100,000 | ~400ms |

## Best Practices

### File Organization

1. **Use header rows**: Enables column name references
2. **Consistent types**: Mixed types in columns slow down type inference
3. **Normalize data**: Split large files by logical units

### Query Patterns

1. **Filter early**: Use WHERE to reduce data before joins
2. **Limit results**: Use LIMIT for exploration queries
3. **Select needed columns**: Avoid SELECT * for large tables

### Memory Tips

1. **Close REPL**: Exit REPL when done to free memory
2. **Process in batches**: For very large datasets, process in multiple queries
3. **Monitor memory**: Check memory usage for large operations

## Troubleshooting

### OutOfMemoryError

If you encounter OutOfMemoryError with large files:

1. Increase JVM heap size:
```bash
java -Xmx2g -jar csv-sql-parser.jar query "..."
```

2. Use chunked loading:
```java
loader.setUseChunkedLoading(true).setChunkSize(5000);
```

3. Process data in smaller batches using LIMIT and OFFSET

### Slow Queries

For slow queries:

1. Check if proper indexes are being used (JOIN optimization)
2. Reduce the number of columns selected
3. Add more specific WHERE conditions
4. Use EXPLAIN equivalent (verbose mode):
```bash
java -jar csv-sql-parser.jar query -v "SELECT ..."
```