package com.csvsql.parser.engine;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.*;

/**
 * Evaluates SQL expressions including CASE WHEN, COALESCE, CAST, arithmetic, etc.
 *
 * <p>ExpressionEvaluator is responsible for evaluating SQL expressions during
 * query execution. It supports:</p>
 *
 * <p><b>Conditional Expressions:</b></p>
 * <ul>
 *   <li>CASE WHEN condition THEN result ELSE result END</li>
 *   <li>CASE expr WHEN value THEN result END</li>
 * </ul>
 *
 * <p><b>NULL Handling Functions:</b></p>
 * <ul>
 *   <li>COALESCE(val1, val2, ...) - returns first non-NULL value</li>
 *   <li>IFNULL(val, default) - returns default if val is NULL</li>
 *   <li>NULLIF(val1, val2) - returns NULL if values are equal</li>
 * </ul>
 *
 * <p><b>Type Conversion:</b></p>
 * <ul>
 *   <li>CAST(expr AS type) - converts expression to specified type</li>
 *   <li>CONVERT(expr, type) - alias for CAST</li>
 * </ul>
 *
 * <p><b>String Functions:</b></p>
 * <ul>
 *   <li>UPPER(s), LOWER(s) - case conversion</li>
 *   <li>LENGTH(s) - string length</li>
 *   <li>CONCAT(s1, s2, ...) - string concatenation</li>
 *   <li>SUBSTRING(s, start, len) - substring extraction</li>
 *   <li>TRIM(s) - remove leading/trailing whitespace</li>
 * </ul>
 *
 * <p><b>Math Functions:</b></p>
 * <ul>
 *   <li>ABS(n) - absolute value</li>
 *   <li>ROUND(n, decimals) - round to specified decimals</li>
 *   <li>CEILING(n), FLOOR(n) - ceiling and floor</li>
 * </ul>
 *
 * <p><b>Date Functions:</b></p>
 * <ul>
 *   <li>YEAR(date), MONTH(date), DAY(date) - extract date components</li>
 * </ul>
 *
 * <p><b>Arithmetic Operations:</b></p>
 * <ul>
 *   <li>Addition (+), Subtraction (-)</li>
 *   <li>Multiplication (*), Division (/)</li>
 *   <li>Modulo (%)</li>
 * </ul>
 *
 * @see SelectExecutor
 * @see QueryExecutor
 */
public class ExpressionEvaluator {

    /**
     * Evaluates an expression against a row of data.
     *
     * <p>This method recursively evaluates the expression tree, handling
     * all supported expression types. Column references are resolved
     * against the provided row data.</p>
     *
     * @param expr the expression to evaluate
     * @param row  the row data for column resolution
     * @return the evaluated result, or null if the expression evaluates to NULL
     */
    public Object evaluate(Expression expr, Map<String, Object> row) {
        if (expr == null) {
            return null;
        }

        // CASE expression
        if (expr instanceof CaseExpression) {
            return evaluateCase((CaseExpression) expr, row);
        }

        // Function calls
        if (expr instanceof Function) {
            return evaluateFunction((Function) expr, row);
        }

        // Arithmetic operations
        if (expr instanceof Addition) {
            return evaluateArithmetic((Addition) expr, row, ArithmeticOp.ADD);
        }
        if (expr instanceof Subtraction) {
            return evaluateArithmetic((Subtraction) expr, row, ArithmeticOp.SUB);
        }
        if (expr instanceof Multiplication) {
            return evaluateArithmetic((Multiplication) expr, row, ArithmeticOp.MUL);
        }
        if (expr instanceof Division) {
            return evaluateArithmetic((Division) expr, row, ArithmeticOp.DIV);
        }
        if (expr instanceof Modulo) {
            return evaluateArithmetic((Modulo) expr, row, ArithmeticOp.MOD);
        }

        // Literals
        if (expr instanceof LongValue) {
            return ((LongValue) expr).getValue();
        }
        if (expr instanceof DoubleValue) {
            return ((DoubleValue) expr).getValue();
        }
        if (expr instanceof StringValue) {
            return ((StringValue) expr).getValue();
        }
        if (expr instanceof NullValue) {
            return null;
        }

        // Column reference
        if (expr instanceof Column) {
            return getColumnValue((Column) expr, row);
        }

        // Parenthesis
        if (expr instanceof Parenthesis) {
            return evaluate(((Parenthesis) expr).getExpression(), row);
        }

        // Comparison expressions (return boolean)
        if (expr instanceof EqualsTo || expr instanceof NotEqualsTo ||
            expr instanceof GreaterThan || expr instanceof GreaterThanEquals ||
            expr instanceof MinorThan || expr instanceof MinorThanEquals) {
            return evaluateComparison(expr, row);
        }

        // Logical expressions
        if (expr instanceof AndExpression) {
            AndExpression and = (AndExpression) expr;
            Boolean left = (Boolean) evaluate(and.getLeftExpression(), row);
            Boolean right = (Boolean) evaluate(and.getRightExpression(), row);
            return left != null && right != null && left && right;
        }
        if (expr instanceof OrExpression) {
            OrExpression or = (OrExpression) expr;
            Boolean left = (Boolean) evaluate(or.getLeftExpression(), row);
            Boolean right = (Boolean) evaluate(or.getRightExpression(), row);
            return left != null && left || right != null && right;
        }

        // Default: return string representation
        return expr.toString();
    }

    private Object evaluateCase(CaseExpression caseExpr, Map<String, Object> row) {
        Expression switchExpr = caseExpr.getSwitchExpression();
        List<WhenClause> whenClauses = caseExpr.getWhenClauses();
        Expression elseExpr = caseExpr.getElseExpression();

        for (WhenClause whenClause : whenClauses) {
            Expression whenExpr = whenClause.getWhenExpression();
            Expression thenExpr = whenClause.getThenExpression();

            if (switchExpr != null) {
                // CASE expr WHEN value THEN result
                Object switchValue = evaluate(switchExpr, row);
                Object whenValue = evaluate(whenExpr, row);
                if (equals(switchValue, whenValue)) {
                    return evaluate(thenExpr, row);
                }
            } else {
                // CASE WHEN condition THEN result
                Boolean condition = (Boolean) evaluate(whenExpr, row);
                if (condition != null && condition) {
                    return evaluate(thenExpr, row);
                }
            }
        }

        // ELSE clause
        if (elseExpr != null) {
            return evaluate(elseExpr, row);
        }

        return null;
    }

    private Object evaluateFunction(Function func, Map<String, Object> row) {
        String funcName = func.getName().toUpperCase();

        switch (funcName) {
            case "COALESCE":
                return evaluateCoalesce(func, row);
            case "IFNULL":
                return evaluateIfnull(func, row);
            case "NULLIF":
                return evaluateNullif(func, row);
            case "CAST":
            case "CONVERT":
                return evaluateCast(func, row);
            case "UPPER":
                return evaluateUpper(func, row);
            case "LOWER":
                return evaluateLower(func, row);
            case "LENGTH":
            case "LEN":
                return evaluateLength(func, row);
            case "CONCAT":
                return evaluateConcat(func, row);
            case "SUBSTRING":
            case "SUBSTR":
                return evaluateSubstring(func, row);
            case "TRIM":
                return evaluateTrim(func, row);
            case "ABS":
                return evaluateAbs(func, row);
            case "ROUND":
                return evaluateRound(func, row);
            case "CEILING":
            case "CEIL":
                return evaluateCeiling(func, row);
            case "FLOOR":
                return evaluateFloor(func, row);
            case "YEAR":
                return evaluateYear(func, row);
            case "MONTH":
                return evaluateMonth(func, row);
            case "DAY":
                return evaluateDay(func, row);
            default:
                // Unknown function - return null
                return null;
        }
    }

    private Object evaluateCoalesce(Function func, Map<String, Object> row) {
        if (func.getParameters() == null) return null;
        List<Expression> params = func.getParameters().getExpressions();
        for (Expression param : params) {
            Object value = evaluate(param, row);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Object evaluateIfnull(Function func, Map<String, Object> row) {
        if (func.getParameters() == null) return null;
        List<Expression> params = func.getParameters().getExpressions();
        if (params.size() >= 2) {
            Object value = evaluate(params.get(0), row);
            if (value != null) {
                return value;
            }
            return evaluate(params.get(1), row);
        }
        return null;
    }

    private Object evaluateNullif(Function func, Map<String, Object> row) {
        if (func.getParameters() == null) return null;
        List<Expression> params = func.getParameters().getExpressions();
        if (params.size() >= 2) {
            Object value1 = evaluate(params.get(0), row);
            Object value2 = evaluate(params.get(1), row);
            if (equals(value1, value2)) {
                return null;
            }
            return value1;
        }
        return null;
    }

    private Object evaluateCast(Function func, Map<String, Object> row) {
        if (func.getParameters() == null) return null;
        List<Expression> params = func.getParameters().getExpressions();
        if (params.isEmpty()) return null;

        Object value = evaluate(params.get(0), row);
        if (value == null) return null;

        // Try to get the target type from the function
        // JSqlParser may store it differently, so we use toString() as fallback
        String targetType = func.toString().toUpperCase();

        if (targetType.contains("INT") || targetType.contains("INTEGER")) {
            return castToInteger(value);
        } else if (targetType.contains("DECIMAL") || targetType.contains("DOUBLE") || targetType.contains("FLOAT")) {
            return castToDouble(value);
        } else if (targetType.contains("CHAR") || targetType.contains("VARCHAR") || targetType.contains("TEXT")) {
            return value.toString();
        } else if (targetType.contains("BOOL")) {
            return castToBoolean(value);
        }

        // Default: return as string
        return value.toString();
    }

    private Object evaluateUpper(Function func, Map<String, Object> row) {
        Object value = getFirstParamValue(func, row);
        return value != null ? value.toString().toUpperCase() : null;
    }

    private Object evaluateLower(Function func, Map<String, Object> row) {
        Object value = getFirstParamValue(func, row);
        return value != null ? value.toString().toLowerCase() : null;
    }

    private Object evaluateLength(Function func, Map<String, Object> row) {
        Object value = getFirstParamValue(func, row);
        return value != null ? value.toString().length() : null;
    }

    private Object evaluateConcat(Function func, Map<String, Object> row) {
        if (func.getParameters() == null) return null;
        List<Expression> params = func.getParameters().getExpressions();
        StringBuilder sb = new StringBuilder();
        for (Expression param : params) {
            Object value = evaluate(param, row);
            if (value != null) {
                sb.append(value.toString());
            }
        }
        return sb.toString();
    }

    private Object evaluateSubstring(Function func, Map<String, Object> row) {
        if (func.getParameters() == null) return null;
        List<Expression> params = func.getParameters().getExpressions();
        if (params.isEmpty()) return null;

        Object value = evaluate(params.get(0), row);
        if (value == null) return null;

        String str = value.toString();
        int start = 1;
        int length = str.length();

        if (params.size() >= 2) {
            Object startObj = evaluate(params.get(1), row);
            if (startObj instanceof Number) {
                start = ((Number) startObj).intValue();
            }
        }
        if (params.size() >= 3) {
            Object lenObj = evaluate(params.get(2), row);
            if (lenObj instanceof Number) {
                length = ((Number) lenObj).intValue();
            }
        }

        // SQL substring is 1-indexed
        int javaStart = Math.max(0, start - 1);
        int end = Math.min(javaStart + length, str.length());
        return str.substring(javaStart, end);
    }

    private Object evaluateTrim(Function func, Map<String, Object> row) {
        Object value = getFirstParamValue(func, row);
        return value != null ? value.toString().trim() : null;
    }

    private Object evaluateAbs(Function func, Map<String, Object> row) {
        Object value = getFirstParamValue(func, row);
        if (value instanceof Number) {
            return Math.abs(((Number) value).doubleValue());
        }
        return null;
    }

    private Object evaluateRound(Function func, Map<String, Object> row) {
        if (func.getParameters() == null) return null;
        List<Expression> params = func.getParameters().getExpressions();
        if (params.isEmpty()) return null;

        Object value = evaluate(params.get(0), row);
        if (!(value instanceof Number)) return null;

        int decimals = 0;
        if (params.size() >= 2) {
            Object decObj = evaluate(params.get(1), row);
            if (decObj instanceof Number) {
                decimals = ((Number) decObj).intValue();
            }
        }

        double d = ((Number) value).doubleValue();
        double factor = Math.pow(10, decimals);
        return Math.round(d * factor) / factor;
    }

    private Object evaluateCeiling(Function func, Map<String, Object> row) {
        Object value = getFirstParamValue(func, row);
        if (value instanceof Number) {
            return Math.ceil(((Number) value).doubleValue());
        }
        return null;
    }

    private Object evaluateFloor(Function func, Map<String, Object> row) {
        Object value = getFirstParamValue(func, row);
        if (value instanceof Number) {
            return Math.floor(((Number) value).doubleValue());
        }
        return null;
    }

    private Object evaluateYear(Function func, Map<String, Object> row) {
        Object value = getFirstParamValue(func, row);
        if (value instanceof java.time.temporal.TemporalAccessor) {
            return ((java.time.temporal.TemporalAccessor) value).get(java.time.temporal.ChronoField.YEAR);
        }
        return null;
    }

    private Object evaluateMonth(Function func, Map<String, Object> row) {
        Object value = getFirstParamValue(func, row);
        if (value instanceof java.time.temporal.TemporalAccessor) {
            return ((java.time.temporal.TemporalAccessor) value).get(java.time.temporal.ChronoField.MONTH_OF_YEAR);
        }
        return null;
    }

    private Object evaluateDay(Function func, Map<String, Object> row) {
        Object value = getFirstParamValue(func, row);
        if (value instanceof java.time.temporal.TemporalAccessor) {
            return ((java.time.temporal.TemporalAccessor) value).get(java.time.temporal.ChronoField.DAY_OF_MONTH);
        }
        return null;
    }

    private Object getFirstParamValue(Function func, Map<String, Object> row) {
        if (func.getParameters() == null) return null;
        List<Expression> params = func.getParameters().getExpressions();
        if (params.isEmpty()) return null;
        return evaluate(params.get(0), row);
    }

    private enum ArithmeticOp {
        ADD, SUB, MUL, DIV, MOD
    }

    private Object evaluateArithmetic(BinaryExpression expr, Map<String, Object> row, ArithmeticOp op) {
        Object left = evaluate(expr.getLeftExpression(), row);
        Object right = evaluate(expr.getRightExpression(), row);

        if (left == null || right == null) {
            return null;
        }

        // Convert to numbers
        double leftNum = toDouble(left);
        double rightNum = toDouble(right);

        switch (op) {
            case ADD:
                return leftNum + rightNum;
            case SUB:
                return leftNum - rightNum;
            case MUL:
                return leftNum * rightNum;
            case DIV:
                if (rightNum == 0) return null;
                return leftNum / rightNum;
            case MOD:
                if (rightNum == 0) return null;
                return leftNum % rightNum;
            default:
                return null;
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Object getColumnValue(Column column, Map<String, Object> row) {
        String colName = column.getColumnName();

        // Try with table prefix
        if (column.getTable() != null) {
            String fullName = column.getTable().getName() + "." + colName;
            if (row.containsKey(fullName)) {
                return row.get(fullName);
            }
        }

        // Try exact match
        if (row.containsKey(colName)) {
            return row.get(colName);
        }

        // Try with any table prefix
        for (String key : row.keySet()) {
            if (key.endsWith("." + colName)) {
                return row.get(key);
            }
        }

        return null;
    }

    private Boolean evaluateComparison(Expression expr, Map<String, Object> row) {
        if (expr instanceof EqualsTo) {
            EqualsTo eq = (EqualsTo) expr;
            Object left = evaluate(eq.getLeftExpression(), row);
            Object right = evaluate(eq.getRightExpression(), row);
            return equals(left, right);
        }
        if (expr instanceof NotEqualsTo) {
            NotEqualsTo ne = (NotEqualsTo) expr;
            Object left = evaluate(ne.getLeftExpression(), row);
            Object right = evaluate(ne.getRightExpression(), row);
            return !equals(left, right);
        }
        if (expr instanceof GreaterThan) {
            GreaterThan gt = (GreaterThan) expr;
            Object left = evaluate(gt.getLeftExpression(), row);
            Object right = evaluate(gt.getRightExpression(), row);
            return compare(left, right) > 0;
        }
        if (expr instanceof GreaterThanEquals) {
            GreaterThanEquals ge = (GreaterThanEquals) expr;
            Object left = evaluate(ge.getLeftExpression(), row);
            Object right = evaluate(ge.getRightExpression(), row);
            return compare(left, right) >= 0;
        }
        if (expr instanceof MinorThan) {
            MinorThan lt = (MinorThan) expr;
            Object left = evaluate(lt.getLeftExpression(), row);
            Object right = evaluate(lt.getRightExpression(), row);
            return compare(left, right) < 0;
        }
        if (expr instanceof MinorThanEquals) {
            MinorThanEquals le = (MinorThanEquals) expr;
            Object left = evaluate(le.getLeftExpression(), row);
            Object right = evaluate(le.getRightExpression(), row);
            return compare(left, right) <= 0;
        }
        return null;
    }

    private boolean equals(Object val1, Object val2) {
        if (val1 == null && val2 == null) return true;
        if (val1 == null || val2 == null) return false;

        if (val1 instanceof Number && val2 instanceof Number) {
            return ((Number) val1).doubleValue() == ((Number) val2).doubleValue();
        }

        if (val1 instanceof String && val2 instanceof String) {
            return val1.toString().equalsIgnoreCase(val2.toString());
        }

        return val1.equals(val2);
    }

    @SuppressWarnings("unchecked")
    private int compare(Object val1, Object val2) {
        if (val1 == null && val2 == null) return 0;
        if (val1 == null) return 1;
        if (val2 == null) return -1;

        if (val1 instanceof Number && val2 instanceof Number) {
            double diff = ((Number) val1).doubleValue() - ((Number) val2).doubleValue();
            return diff < 0 ? -1 : (diff > 0 ? 1 : 0);
        }

        if (val1 instanceof Comparable && val2 instanceof Comparable) {
            try {
                return ((Comparable<Object>) val1).compareTo(val2);
            } catch (ClassCastException e) {
                // Fall through to string comparison
            }
        }

        return val1.toString().compareTo(val2.toString());
    }

    private Integer castToInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double castToDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean castToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String str = value.toString().toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str);
    }
}