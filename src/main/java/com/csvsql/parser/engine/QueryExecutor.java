package com.csvsql.parser.engine;

import com.csvsql.parser.data.CsvTable;
import com.csvsql.parser.data.TableRegistry;
import com.csvsql.parser.output.*;
import com.csvsql.parser.parser.QueryPlan;
import com.csvsql.parser.parser.QueryPlan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Main query executor that coordinates SQL query execution.
 *
 * <p>QueryExecutor is the central component that orchestrates the execution
 * of SQL queries against CSV data. It processes a {@link QueryPlan} through
 * multiple stages:</p>
 * <ol>
 *   <li>Load required tables from the registry</li>
 *   <li>Apply WHERE clause filtering (early optimization)</li>
 *   <li>Execute JOINs if present</li>
 *   <li>Apply GROUP BY and aggregations</li>
 *   <li>Apply HAVING clause</li>
 *   <li>Project SELECT columns</li>
 *   <li>Sort by ORDER BY</li>
 *   <li>Apply LIMIT and OFFSET</li>
 * </ol>
 *
 * <p>Example usage:</p>
 * <pre>
 * TableRegistry registry = new TableRegistry();
 * SqlParser parser = new SqlParser();
 * QueryExecutor executor = new QueryExecutor(registry);
 *
 * QueryPlan plan = parser.parse("SELECT * FROM employees WHERE salary > 50000");
 * ResultSet result = executor.execute(plan);
 * </pre>
 *
 * @see QueryPlan
 * @see TableRegistry
 * @see ResultSet
 * @see SelectExecutor
 * @see WhereEvaluator
 * @see GroupByExecutor
 * @see JoinExecutor
 */
public class QueryExecutor {

    private static final Logger logger = LoggerFactory.getLogger(QueryExecutor.class);

    private final TableRegistry tableRegistry;
    private final SelectExecutor selectExecutor;
    private final JoinExecutor joinExecutor;
    private final WhereEvaluator whereEvaluator;
    private final GroupByExecutor groupByExecutor;

    /**
     * Creates a new QueryExecutor with the specified table registry.
     *
     * @param tableRegistry the registry for loading and managing tables
     */
    public QueryExecutor(TableRegistry tableRegistry) {
        this.tableRegistry = tableRegistry;
        this.selectExecutor = new SelectExecutor();
        this.joinExecutor = new JoinExecutor();
        this.whereEvaluator = new WhereEvaluator();
        this.groupByExecutor = new GroupByExecutor();
    }

    /**
     * Executes a query plan and returns the result set.
     *
     * <p>The execution follows a pipeline model where each stage transforms
     * the data before passing to the next stage. Execution time is measured
     * and included in the result.</p>
     *
     * @param plan the query plan to execute
     * @return the result set containing query results and metadata
     */
    public ResultSet execute(QueryPlan plan) {
        long startTime = System.currentTimeMillis();
        logger.info("Executing query: {}", plan.getOriginalSql());

        // Step 1: Load required tables
        List<CsvTable> tables = loadTables(plan);
        if (tables.isEmpty()) {
            return new ResultSet(Collections.emptyList(), Collections.emptyList(), 0);
        }

        // Step 2: Get the primary table data
        CsvTable primaryTable = tables.get(0);
        List<Map<String, Object>> workingRows = new ArrayList<>();
        for (Map<String, Object> row : primaryTable.getRows()) {
            workingRows.add(new LinkedHashMap<>(row));
        }

        // Step 3: Apply WHERE filtering (early filtering for performance)
        if (plan.hasWhere()) {
            workingRows = whereEvaluator.evaluate(workingRows, plan.getWhereConditions());
            logger.debug("After WHERE: {} rows", workingRows.size());
        }

        // Step 4: Execute JOINs
        if (plan.hasJoins()) {
            for (JoinClause join : plan.getJoins()) {
                // Use effective name (alias if present, otherwise table name)
                String tableLookupName = join.getTable().getEffectiveName();
                CsvTable joinTable = tableRegistry.getTable(tableLookupName);
                if (joinTable == null) {
                    // Fallback to original name
                    joinTable = tableRegistry.getTable(join.getTable().getName());
                }
                if (joinTable == null) {
                    throw new RuntimeException("Table not found: " + join.getTable().getName());
                }
                workingRows = joinExecutor.executeJoin(workingRows, joinTable, join);
                logger.debug("After JOIN: {} rows", workingRows.size());
            }
        }

        // Step 5: Apply GROUP BY and aggregation
        if (plan.hasGroupBy()) {
            workingRows = groupByExecutor.execute(workingRows, plan);
            logger.debug("After GROUP BY: {} rows", workingRows.size());
        } else if (hasAggregateFunctions(plan)) {
            // Aggregate without GROUP BY (single result row)
            workingRows = groupByExecutor.executeAggregateAll(workingRows, plan);
            logger.debug("After aggregation: {} rows", workingRows.size());
        }

        // Step 6: Apply HAVING
        if (plan.hasHaving()) {
            workingRows = whereEvaluator.evaluate(workingRows, plan.getHavingConditions());
            logger.debug("After HAVING: {} rows", workingRows.size());
        }

        // Step 7: Apply SELECT projection
        List<String> resultColumns = getResultColumns(plan, tables);
        workingRows = selectExecutor.project(workingRows, plan, resultColumns);

        // Step 8: Apply ORDER BY
        if (plan.hasOrderBy()) {
            workingRows = selectExecutor.orderBy(workingRows, plan.getOrderByColumns());
            logger.debug("After ORDER BY: {} rows", workingRows.size());
        }

        // Step 9: Apply LIMIT and OFFSET
        if (plan.hasLimit() || plan.hasOffset()) {
            workingRows = selectExecutor.applyLimitOffset(workingRows, plan.getLimit(), plan.getOffset());
            logger.debug("After LIMIT/OFFSET: {} rows", workingRows.size());
        }

        long executionTime = System.currentTimeMillis() - startTime;
        logger.info("Query executed in {}ms, returned {} rows", executionTime, workingRows.size());

        return new ResultSet(resultColumns, workingRows, executionTime);
    }

    /**
     * Loads all tables referenced in the query plan.
     *
     * @param plan the query plan containing table references
     * @return list of loaded tables
     */
    private List<CsvTable> loadTables(QueryPlan plan) {
        List<CsvTable> tables = new ArrayList<>();

        for (TableReference tableRef : plan.getTables()) {
            String tableName = tableRef.getName();
            String alias = tableRef.getAlias();
            CsvTable table = null;

            // Try to find by alias first (tables may be registered with alias)
            if (alias != null && !alias.isEmpty()) {
                table = tableRegistry.getTable(alias);
            }

            // If not found, try by table name
            if (table == null) {
                table = tableRegistry.getTable(tableName);
            }

            // If still not found, load the table
            if (table == null) {
                try {
                    table = tableRegistry.loadAndRegister(tableName, alias);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to load table: " + tableName, e);
                }
            }

            if (table != null) {
                tables.add(table);
            }
        }

        return tables;
    }

    /**
     * Checks if the query plan contains aggregate functions.
     *
     * @param plan the query plan to check
     * @return true if any column is an aggregate function
     */
    private boolean hasAggregateFunctions(QueryPlan plan) {
        for (ColumnReference col : plan.getColumns()) {
            if (col.isFunction()) {
                String funcName = col.getFunctionName();
                if (funcName != null && (funcName.equals("COUNT") || funcName.equals("SUM") ||
                    funcName.equals("AVG") || funcName.equals("MIN") || funcName.equals("MAX"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines the result column names based on the query plan.
     *
     * <p>When SELECT * is used with JOINs, this method returns columns from all
     * joined tables, not just the primary table.</p>
     *
     * <p>When SELECT table.* is used, this method returns columns from the specified
     * table plus any other explicitly selected columns.</p>
     *
     * @param plan   the query plan
     * @param tables all loaded tables (including joined tables)
     * @return list of result column names
     */
    private List<String> getResultColumns(QueryPlan plan, List<CsvTable> tables) {
        List<String> columns = new ArrayList<>();

        if (plan.isSelectAll()) {
            // SELECT * - include columns from all tables
            for (CsvTable table : tables) {
                columns.addAll(table.getColumnNames());
            }
        } else if (plan.hasSelectAllTableColumns()) {
            // SELECT table.* or table.*, other_col - handle table.* selections
            List<String> tableAllCols = plan.getSelectAllTableColumns();

            // Build a map of table name/alias to CsvTable
            Map<String, CsvTable> tableMap = new HashMap<>();
            for (CsvTable table : tables) {
                tableMap.put(table.getName(), table);
            }

            // Add columns from tables specified in table.* selections
            for (String tableName : tableAllCols) {
                CsvTable table = findTableByNameOrAlias(tableName, tables, tableMap);
                if (table != null) {
                    columns.addAll(table.getColumnNames());
                }
            }

            // Add explicitly selected columns
            for (ColumnReference col : plan.getColumns()) {
                columns.add(col.getEffectiveName());
            }
        } else {
            for (ColumnReference col : plan.getColumns()) {
                columns.add(col.getEffectiveName());
            }
        }

        return columns;
    }

    /**
     * Find a table by its name or alias.
     */
    private CsvTable findTableByNameOrAlias(String name, List<CsvTable> tables, Map<String, CsvTable> tableMap) {
        // Try exact match first
        if (tableMap.containsKey(name)) {
            return tableMap.get(name);
        }

        // Try to find by alias (compare with table name from CsvTable)
        for (CsvTable table : tables) {
            if (name.equalsIgnoreCase(table.getName())) {
                return table;
            }
        }

        return null;
    }
}