package com.csvsql.parser.parser;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a query plan from a parsed SELECT statement.
 *
 * <p>QueryPlanBuilder transforms a JSqlParser {@link Select} object into a
 * {@link QueryPlan} that can be executed by the query executor. It handles:</p>
 * <ul>
 *   <li>FROM clause - table references and file paths</li>
 *   <li>JOIN clauses - INNER, LEFT, RIGHT joins with ON conditions</li>
 *   <li>SELECT items - columns, functions, expressions, aliases</li>
 *   <li>WHERE clause - filter conditions with operators</li>
 *   <li>GROUP BY and HAVING - aggregation specifications</li>
 *   <li>ORDER BY - sort specifications</li>
 *   <li>LIMIT and OFFSET - result pagination</li>
 * </ul>
 *
 * @see SqlParser
 * @see QueryPlan
 * @see com.csvsql.parser.engine.QueryExecutor
 */
public class QueryPlanBuilder {

    /**
     * Build a query plan from a SELECT statement.
     *
     * @param select the parsed SELECT statement
     * @return the query plan
     */
    public QueryPlan build(Select select) {
        SelectBody selectBody = select.getSelectBody();

        if (!(selectBody instanceof PlainSelect)) {
            throw new UnsupportedOperationException("Only simple SELECT statements are supported");
        }

        PlainSelect plainSelect = (PlainSelect) selectBody;
        QueryPlan plan = new QueryPlan(select.toString());

        // Process FROM clause (tables)
        processFromItem(plainSelect.getFromItem(), plan);

        // Process JOINs
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                processJoin(join, plan);
            }
        }

        // Process SELECT columns
        processSelectItems(plainSelect.getSelectItems(), plan);

        // Process WHERE clause
        if (plainSelect.getWhere() != null) {
            processWhere(plainSelect.getWhere(), plan);
        }

        // Process GROUP BY
        if (plainSelect.getGroupBy() != null) {
            processGroupBy(plainSelect.getGroupBy(), plan);
        }

        // Process HAVING
        if (plainSelect.getHaving() != null) {
            processHaving(plainSelect.getHaving(), plan);
        }

        // Process ORDER BY
        if (plainSelect.getOrderByElements() != null) {
            processOrderBy(plainSelect.getOrderByElements(), plan);
        }

        // Process LIMIT and OFFSET
        if (plainSelect.getLimit() != null) {
            Expression rowCount = plainSelect.getLimit().getRowCount();
            if (rowCount instanceof LongValue) {
                plan.setLimit((int) ((LongValue) rowCount).getValue());
            }
        }
        if (plainSelect.getOffset() != null) {
            Expression offset = plainSelect.getOffset().getOffset();
            if (offset instanceof LongValue) {
                plan.setOffset((int) ((LongValue) offset).getValue());
            }
        }

        return plan;
    }

    private void processFromItem(FromItem fromItem, QueryPlan plan) {
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            // Use getFullyQualifiedName() to handle file names with dots (e.g., orders.csv)
            // JSqlParser parses "orders.csv" as schema="orders", table="csv"
            String tableName = stripQuotes(table.getFullyQualifiedName());
            String alias = table.getAlias() != null ? table.getAlias().getName() : null;

            // Extract file path from table name (could be a file path)
            String filePath = tableName;
            plan.addTable(new QueryPlan.TableReference(tableName, alias, filePath));
        }
    }

    private void processJoin(Join join, QueryPlan plan) {
        if (join.getRightItem() instanceof Table) {
            Table table = (Table) join.getRightItem();
            // Use getFullyQualifiedName() to handle file names with dots (e.g., orders.csv)
            String tableName = stripQuotes(table.getFullyQualifiedName());
            String alias = table.getAlias() != null ? table.getAlias().getName() : null;

            // Determine join type
            QueryPlan.JoinClause.JoinType joinType = QueryPlan.JoinClause.JoinType.INNER;
            if (join.isLeft()) {
                joinType = QueryPlan.JoinClause.JoinType.LEFT;
            } else if (join.isRight()) {
                joinType = QueryPlan.JoinClause.JoinType.RIGHT;
            }

            // Add the JOIN table to the plan's table list so it gets loaded
            QueryPlan.TableReference tableRef = new QueryPlan.TableReference(tableName, alias, tableName);
            plan.addTable(tableRef);

            // Extract ON condition
            Expression onExpression = join.getOnExpression();
            String[] columns = extractJoinColumns(onExpression);
            if (columns != null && columns.length == 2) {
                plan.addJoin(new QueryPlan.JoinClause(joinType, tableRef, columns[0], columns[1]));
            }
        }
    }

    private String[] extractJoinColumns(Expression expression) {
        if (expression instanceof EqualsTo) {
            EqualsTo equals = (EqualsTo) expression;
            String left = extractColumnName(equals.getLeftExpression());
            String right = extractColumnName(equals.getRightExpression());
            if (left != null && right != null) {
                return new String[]{left, right};
            }
        }
        return null;
    }

    private String extractColumnName(Expression expression) {
        if (expression instanceof Column) {
            Column column = (Column) expression;
            if (column.getTable() != null) {
                // Use getFullyQualifiedName() to handle table names with dots
                String tablePart = column.getTable().getFullyQualifiedName();
                return stripQuotes(tablePart) + "." + column.getColumnName();
            }
            return column.getColumnName();
        }
        return null;
    }

    private void processSelectItems(List<SelectItem> selectItems, QueryPlan plan) {
        for (SelectItem item : selectItems) {
            if (item instanceof AllColumns) {
                plan.setSelectAll(true);
            } else if (item instanceof AllTableColumns) {
                // Handle table.* (e.g., o.*)
                AllTableColumns atc = (AllTableColumns) item;
                String tableName = atc.getTable() != null ? atc.getTable().getName() : null;
                plan.addTableAllColumns(tableName);
            } else if (item instanceof SelectExpressionItem) {
                SelectExpressionItem exprItem = (SelectExpressionItem) item;
                Expression expr = exprItem.getExpression();
                String alias = exprItem.getAlias() != null ? exprItem.getAlias().getName() : null;

                if (expr instanceof Column) {
                    Column col = (Column) expr;
                    String tableName = col.getTable() != null ? stripQuotes(col.getTable().getFullyQualifiedName()) : null;
                    plan.addColumn(new QueryPlan.ColumnReference(col.getColumnName(), alias, tableName));
                } else if (expr instanceof Function) {
                    Function func = (Function) expr;
                    String funcName = func.getName().toUpperCase();
                    // Extract column name from function parameters
                    String colName = extractFunctionColumnName(func);
                    plan.addColumn(new QueryPlan.ColumnReference(colName, alias, null, true, funcName, expr));
                } else if (expr instanceof CaseExpression) {
                    // CASE WHEN expression
                    String colName = alias != null ? alias : "CASE_" + System.identityHashCode(expr);
                    plan.addColumn(new QueryPlan.ColumnReference(colName, alias, null, true, "CASE", expr));
                } else {
                    // Handle other expressions (arithmetic, etc.) - store the expression
                    String colName = alias != null ? alias : expr.toString();
                    plan.addColumn(new QueryPlan.ColumnReference(colName, alias, null, false, null, expr));
                }
            }
        }
    }

    /**
     * Extract the column name from a function's parameters.
     */
    private String extractFunctionColumnName(Function func) {
        if (func.getParameters() == null || func.getParameters().getExpressions() == null) {
            return "*";
        }
        List<Expression> params = func.getParameters().getExpressions();
        if (params.isEmpty()) {
            return "*";
        }
        Expression firstParam = params.get(0);
        if (firstParam instanceof Column) {
            return ((Column) firstParam).getColumnName();
        } else if (firstParam instanceof AllColumns) {
            return "*";
        }
        return firstParam.toString();
    }

    private void processWhere(Expression where, QueryPlan plan) {
        QueryPlan.FilterCondition condition = buildFilterCondition(where);
        if (condition != null) {
            plan.addWhereCondition(condition);
        }
    }

    private QueryPlan.FilterCondition buildFilterCondition(Expression expression) {
        if (expression instanceof AndExpression) {
            AndExpression and = (AndExpression) expression;
            List<QueryPlan.FilterCondition> subConditions = new ArrayList<>();
            subConditions.add(buildFilterCondition(and.getLeftExpression()));
            subConditions.add(buildFilterCondition(and.getRightExpression()));
            return new QueryPlan.FilterCondition(QueryPlan.FilterCondition.Operator.AND, subConditions);
        }

        if (expression instanceof OrExpression) {
            OrExpression or = (OrExpression) expression;
            List<QueryPlan.FilterCondition> subConditions = new ArrayList<>();
            subConditions.add(buildFilterCondition(or.getLeftExpression()));
            subConditions.add(buildFilterCondition(or.getRightExpression()));
            return new QueryPlan.FilterCondition(QueryPlan.FilterCondition.Operator.OR, subConditions);
        }

        if (expression instanceof Parenthesis) {
            return buildFilterCondition(((Parenthesis) expression).getExpression());
        }

        if (expression instanceof EqualsTo) {
            EqualsTo eq = (EqualsTo) expression;
            return new QueryPlan.FilterCondition(
                extractColumnName(eq.getLeftExpression()),
                QueryPlan.FilterCondition.Operator.EQ,
                extractValue(eq.getRightExpression())
            );
        }

        if (expression instanceof NotEqualsTo) {
            NotEqualsTo ne = (NotEqualsTo) expression;
            return new QueryPlan.FilterCondition(
                extractColumnName(ne.getLeftExpression()),
                QueryPlan.FilterCondition.Operator.NE,
                extractValue(ne.getRightExpression())
            );
        }

        if (expression instanceof GreaterThan) {
            GreaterThan gt = (GreaterThan) expression;
            return new QueryPlan.FilterCondition(
                extractColumnName(gt.getLeftExpression()),
                QueryPlan.FilterCondition.Operator.GT,
                extractValue(gt.getRightExpression())
            );
        }

        if (expression instanceof GreaterThanEquals) {
            GreaterThanEquals ge = (GreaterThanEquals) expression;
            return new QueryPlan.FilterCondition(
                extractColumnName(ge.getLeftExpression()),
                QueryPlan.FilterCondition.Operator.GE,
                extractValue(ge.getRightExpression())
            );
        }

        if (expression instanceof MinorThan) {
            MinorThan lt = (MinorThan) expression;
            return new QueryPlan.FilterCondition(
                extractColumnName(lt.getLeftExpression()),
                QueryPlan.FilterCondition.Operator.LT,
                extractValue(lt.getRightExpression())
            );
        }

        if (expression instanceof MinorThanEquals) {
            MinorThanEquals le = (MinorThanEquals) expression;
            return new QueryPlan.FilterCondition(
                extractColumnName(le.getLeftExpression()),
                QueryPlan.FilterCondition.Operator.LE,
                extractValue(le.getRightExpression())
            );
        }

        if (expression instanceof LikeExpression) {
            LikeExpression like = (LikeExpression) expression;
            return new QueryPlan.FilterCondition(
                extractColumnName(like.getLeftExpression()),
                QueryPlan.FilterCondition.Operator.LIKE,
                extractValue(like.getRightExpression())
            );
        }

        if (expression instanceof IsNullExpression) {
            IsNullExpression isNull = (IsNullExpression) expression;
            return new QueryPlan.FilterCondition(
                extractColumnName(isNull.getLeftExpression()),
                isNull.isNot() ? QueryPlan.FilterCondition.Operator.IS_NOT_NULL : QueryPlan.FilterCondition.Operator.IS_NULL,
                null
            );
        }

        if (expression instanceof InExpression) {
            InExpression inExpr = (InExpression) expression;
            String column = extractColumnName(inExpr.getLeftExpression());
            List<Object> values = new ArrayList<>();

            // In JSqlParser 4.x, use getRightItemsList() to get the IN values
            ItemsList rightItems = inExpr.getRightItemsList();
            if (rightItems instanceof ExpressionList) {
                ExpressionList exprList = (ExpressionList) rightItems;
                for (Expression ex : exprList.getExpressions()) {
                    values.add(extractValue(ex));
                }
            } else if (rightItems instanceof net.sf.jsqlparser.statement.select.SubSelect) {
                // Sub-select IN - not supported for CSV files, skip
                return null;
            }

            QueryPlan.FilterCondition.Operator op = inExpr.isNot()
                ? QueryPlan.FilterCondition.Operator.NOT_IN
                : QueryPlan.FilterCondition.Operator.IN;
            return new QueryPlan.FilterCondition(column, op, values);
        }

        return null;
    }

    private Object extractValue(Expression expression) {
        if (expression instanceof LongValue) {
            return ((LongValue) expression).getValue();
        }
        if (expression instanceof DoubleValue) {
            return ((DoubleValue) expression).getValue();
        }
        if (expression instanceof StringValue) {
            return ((StringValue) expression).getValue();
        }
        if (expression instanceof Column) {
            return extractColumnName(expression);
        }
        return expression.toString();
    }

    private void processGroupBy(GroupByElement groupBy, QueryPlan plan) {
        if (groupBy.getGroupByExpressionList() != null) {
            for (Expression expr : groupBy.getGroupByExpressionList().getExpressions()) {
                String colName = extractColumnName(expr);
                if (colName != null) {
                    plan.addGroupByColumn(colName);
                }
            }
        }
    }

    private void processHaving(Expression having, QueryPlan plan) {
        QueryPlan.FilterCondition condition = buildFilterCondition(having);
        if (condition != null) {
            plan.addHavingCondition(condition);
        }
    }

    private void processOrderBy(List<OrderByElement> orderByElements, QueryPlan plan) {
        for (OrderByElement element : orderByElements) {
            String colName = extractColumnName(element.getExpression());
            if (colName != null) {
                plan.addOrderByColumn(new QueryPlan.SortSpecification(colName, element.isAsc()));
            }
        }
    }

    /**
     * Strip surrounding quotes from a string (for quoted identifiers like file paths).
     */
    private String stripQuotes(String str) {
        if (str == null) return null;
        if (str.length() >= 2) {
            char first = str.charAt(0);
            char last = str.charAt(str.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'') ||
                (first == '`' && last == '`')) {
                return str.substring(1, str.length() - 1);
            }
        }
        return str;
    }
}