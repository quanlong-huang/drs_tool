package com.csvsql.parser.parser;

import net.sf.jsqlparser.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parsed query execution plan.
 *
 * <p>QueryPlan is the intermediate representation between the SQL parser and
 * the query executor. It contains all the information needed to execute a
 * SELECT query:</p>
 * <ul>
 *   <li>Table references (FROM clause)</li>
 *   <li>Column references (SELECT clause)</li>
 *   <li>JOIN clauses</li>
 *   <li>WHERE conditions</li>
 *   <li>GROUP BY columns</li>
 *   <li>HAVING conditions</li>
 *   <li>ORDER BY specifications</li>
 *   <li>LIMIT and OFFSET</li>
 * </ul>
 *
 * <p>The query plan is built by {@link QueryPlanBuilder} from a parsed SQL
 * statement and executed by {@link com.csvsql.parser.engine.QueryExecutor}.</p>
 *
 * @see SqlParser
 * @see QueryPlanBuilder
 * @see com.csvsql.parser.engine.QueryExecutor
 */
public class QueryPlan {

    private final String originalSql;
    private List<TableReference> tables = new ArrayList<>();
    private List<ColumnReference> columns = new ArrayList<>();
    private List<JoinClause> joins = new ArrayList<>();
    private List<FilterCondition> whereConditions = new ArrayList<>();
    private List<FilterCondition> havingConditions = new ArrayList<>();
    private List<String> groupByColumns = new ArrayList<>();
    private List<SortSpecification> orderByColumns = new ArrayList<>();
    private Integer limit;
    private Integer offset;
    private boolean selectAll = false;
    private List<String> selectAllTableColumns = new ArrayList<>(); // Tables for "table.*" selections

    public QueryPlan(String originalSql) {
        this.originalSql = originalSql;
    }

    // Getters and setters
    public String getOriginalSql() {
        return originalSql;
    }

    public List<TableReference> getTables() {
        return tables;
    }

    public void setTables(List<TableReference> tables) {
        this.tables = tables;
    }

    public void addTable(TableReference table) {
        this.tables.add(table);
    }

    public List<ColumnReference> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnReference> columns) {
        this.columns = columns;
    }

    public void addColumn(ColumnReference column) {
        this.columns.add(column);
    }

    public List<JoinClause> getJoins() {
        return joins;
    }

    public void setJoins(List<JoinClause> joins) {
        this.joins = joins;
    }

    public void addJoin(JoinClause join) {
        this.joins.add(join);
    }

    public List<FilterCondition> getWhereConditions() {
        return whereConditions;
    }

    public void setWhereConditions(List<FilterCondition> whereConditions) {
        this.whereConditions = whereConditions;
    }

    public void addWhereCondition(FilterCondition condition) {
        this.whereConditions.add(condition);
    }

    public List<FilterCondition> getHavingConditions() {
        return havingConditions;
    }

    public void setHavingConditions(List<FilterCondition> havingConditions) {
        this.havingConditions = havingConditions;
    }

    public void addHavingCondition(FilterCondition condition) {
        this.havingConditions.add(condition);
    }

    public List<String> getGroupByColumns() {
        return groupByColumns;
    }

    public void setGroupByColumns(List<String> groupByColumns) {
        this.groupByColumns = groupByColumns;
    }

    public void addGroupByColumn(String column) {
        this.groupByColumns.add(column);
    }

    public List<SortSpecification> getOrderByColumns() {
        return orderByColumns;
    }

    public void setOrderByColumns(List<SortSpecification> orderByColumns) {
        this.orderByColumns = orderByColumns;
    }

    public void addOrderByColumn(SortSpecification sortSpec) {
        this.orderByColumns.add(sortSpec);
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public boolean isSelectAll() {
        return selectAll;
    }

    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    public void addTableAllColumns(String tableName) {
        this.selectAllTableColumns.add(tableName);
    }

    public List<String> getSelectAllTableColumns() {
        return selectAllTableColumns;
    }

    public boolean hasSelectAllTableColumns() {
        return !selectAllTableColumns.isEmpty();
    }

    // Utility methods
    public boolean hasJoins() {
        return !joins.isEmpty();
    }

    public boolean hasWhere() {
        return !whereConditions.isEmpty();
    }

    public boolean hasGroupBy() {
        return !groupByColumns.isEmpty();
    }

    public boolean hasHaving() {
        return !havingConditions.isEmpty();
    }

    public boolean hasOrderBy() {
        return !orderByColumns.isEmpty();
    }

    public boolean hasLimit() {
        return limit != null;
    }

    public boolean hasOffset() {
        return offset != null;
    }

    @Override
    public String toString() {
        return String.format("QueryPlan[tables=%d, columns=%d, joins=%d, where=%d, groupBy=%d, orderBy=%d, limit=%s, offset=%s]",
            tables.size(), columns.size(), joins.size(), whereConditions.size(),
            groupByColumns.size(), orderByColumns.size(), limit, offset);
    }

    // Inner classes for query components

    public static class TableReference {
        private final String name;
        private final String alias;
        private final String filePath;

        public TableReference(String name, String alias, String filePath) {
            this.name = name;
            this.alias = alias;
            this.filePath = filePath;
        }

        public String getName() {
            return name;
        }

        public String getAlias() {
            return alias;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getEffectiveName() {
            return alias != null ? alias : name;
        }
    }

    public static class ColumnReference {
        private final String name;
        private final String alias;
        private final String tableName;
        private final boolean isFunction;
        private final String functionName;
        private final Expression expression;  // For complex expressions like CASE WHEN

        public ColumnReference(String name, String alias, String tableName) {
            this.name = name;
            this.alias = alias;
            this.tableName = tableName;
            this.isFunction = false;
            this.functionName = null;
            this.expression = null;
        }

        public ColumnReference(String name, String alias, String tableName, boolean isFunction, String functionName) {
            this.name = name;
            this.alias = alias;
            this.tableName = tableName;
            this.isFunction = isFunction;
            this.functionName = functionName;
            this.expression = null;
        }

        public ColumnReference(String name, String alias, String tableName, boolean isFunction, String functionName, Expression expression) {
            this.name = name;
            this.alias = alias;
            this.tableName = tableName;
            this.isFunction = isFunction;
            this.functionName = functionName;
            this.expression = expression;
        }

        public String getName() {
            return name;
        }

        public String getAlias() {
            return alias;
        }

        public String getTableName() {
            return tableName;
        }

        public boolean isFunction() {
            return isFunction;
        }

        public String getFunctionName() {
            return functionName;
        }

        public Expression getExpression() {
            return expression;
        }

        public boolean hasExpression() {
            return expression != null;
        }

        public String getEffectiveName() {
            return alias != null ? alias : name;
        }
    }

    public static class JoinClause {
        public enum JoinType {
            INNER, LEFT, RIGHT
        }

        private final JoinType type;
        private final TableReference table;
        private final String leftColumn;
        private final String rightColumn;

        public JoinClause(JoinType type, TableReference table, String leftColumn, String rightColumn) {
            this.type = type;
            this.table = table;
            this.leftColumn = leftColumn;
            this.rightColumn = rightColumn;
        }

        public JoinType getType() {
            return type;
        }

        public TableReference getTable() {
            return table;
        }

        public String getLeftColumn() {
            return leftColumn;
        }

        public String getRightColumn() {
            return rightColumn;
        }
    }

    public static class FilterCondition {
        public enum Operator {
            EQ, NE, LT, GT, LE, GE, LIKE, IN, NOT_IN, BETWEEN, IS_NULL, IS_NOT_NULL, AND, OR, NOT
        }

        private final String column;
        private final Operator operator;
        private final Object value;
        private final List<FilterCondition> subConditions;

        public FilterCondition(String column, Operator operator, Object value) {
            this.column = column;
            this.operator = operator;
            this.value = value;
            this.subConditions = null;
        }

        public FilterCondition(Operator operator, List<FilterCondition> subConditions) {
            this.column = null;
            this.operator = operator;
            this.value = null;
            this.subConditions = subConditions;
        }

        public String getColumn() {
            return column;
        }

        public Operator getOperator() {
            return operator;
        }

        public Object getValue() {
            return value;
        }

        public List<FilterCondition> getSubConditions() {
            return subConditions;
        }

        public boolean isLogical() {
            return operator == Operator.AND || operator == Operator.OR || operator == Operator.NOT;
        }
    }

    public static class SortSpecification {
        private final String column;
        private final boolean ascending;

        public SortSpecification(String column, boolean ascending) {
            this.column = column;
            this.ascending = ascending;
        }

        public String getColumn() {
            return column;
        }

        public boolean isAscending() {
            return ascending;
        }
    }
}