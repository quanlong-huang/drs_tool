package com.csvsql.parser.parser;

import com.csvsql.parser.error.SqlSyntaxException;
import com.csvsql.parser.error.UnsupportedFeatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SqlParser.
 */
class SqlParserTest {

    private SqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new SqlParser();
    }

    @Test
    void testParseSimpleSelect() {
        QueryPlan plan = parser.parse("SELECT * FROM employees");

        assertNotNull(plan);
        assertEquals(1, plan.getTables().size());
        assertEquals("employees", plan.getTables().get(0).getName());
        assertTrue(plan.isSelectAll());
    }

    @Test
    void testParseSelectColumns() {
        QueryPlan plan = parser.parse("SELECT name, age, salary FROM employees");

        assertNotNull(plan);
        assertEquals(3, plan.getColumns().size());
        assertEquals("name", plan.getColumns().get(0).getName());
        assertEquals("age", plan.getColumns().get(1).getName());
        assertEquals("salary", plan.getColumns().get(2).getName());
    }

    @Test
    void testParseSelectWithAlias() {
        QueryPlan plan = parser.parse("SELECT name AS employee_name FROM employees");

        assertNotNull(plan);
        assertEquals(1, plan.getColumns().size());
        assertEquals("name", plan.getColumns().get(0).getName());
        assertEquals("employee_name", plan.getColumns().get(0).getAlias());
    }

    @Test
    void testParseWhereClause() {
        QueryPlan plan = parser.parse("SELECT * FROM employees WHERE age > 30");

        assertNotNull(plan);
        assertTrue(plan.hasWhere());
        assertEquals(1, plan.getWhereConditions().size());
    }

    @Test
    void testParseWhereWithAnd() {
        QueryPlan plan = parser.parse("SELECT * FROM employees WHERE age > 30 AND salary < 100000");

        assertNotNull(plan);
        assertTrue(plan.hasWhere());
    }

    @Test
    void testParseOrderBy() {
        QueryPlan plan = parser.parse("SELECT * FROM employees ORDER BY salary DESC");

        assertNotNull(plan);
        assertTrue(plan.hasOrderBy());
        assertEquals(1, plan.getOrderByColumns().size());
        assertEquals("salary", plan.getOrderByColumns().get(0).getColumn());
        assertFalse(plan.getOrderByColumns().get(0).isAscending());
    }

    @Test
    void testParseLimit() {
        QueryPlan plan = parser.parse("SELECT * FROM employees LIMIT 10");

        assertNotNull(plan);
        assertTrue(plan.hasLimit());
        assertEquals(10, plan.getLimit());
    }

    @Test
    void testParseLimitOffset() {
        QueryPlan plan = parser.parse("SELECT * FROM employees LIMIT 10 OFFSET 5");

        assertNotNull(plan);
        assertTrue(plan.hasLimit());
        assertTrue(plan.hasOffset());
        assertEquals(10, plan.getLimit());
        assertEquals(5, plan.getOffset());
    }

    @Test
    void testParseGroupBy() {
        QueryPlan plan = parser.parse("SELECT department, COUNT(*) FROM employees GROUP BY department");

        assertNotNull(plan);
        assertTrue(plan.hasGroupBy());
        assertEquals(1, plan.getGroupByColumns().size());
        assertEquals("department", plan.getGroupByColumns().get(0));
    }

    @Test
    void testParseAggregateFunctions() {
        QueryPlan plan = parser.parse("SELECT COUNT(*), SUM(salary), AVG(age) FROM employees");

        assertNotNull(plan);
        assertEquals(3, plan.getColumns().size());

        assertTrue(plan.getColumns().get(0).isFunction());
        assertEquals("COUNT", plan.getColumns().get(0).getFunctionName());

        assertTrue(plan.getColumns().get(1).isFunction());
        assertEquals("SUM", plan.getColumns().get(1).getFunctionName());

        assertTrue(plan.getColumns().get(2).isFunction());
        assertEquals("AVG", plan.getColumns().get(2).getFunctionName());
    }

    @Test
    void testParseJoin() {
        QueryPlan plan = parser.parse(
            "SELECT * FROM employees e JOIN departments d ON e.dept_id = d.id"
        );

        assertNotNull(plan);
        assertTrue(plan.hasJoins());
        assertEquals(1, plan.getJoins().size());
    }

    @Test
    void testParseQuotedFilePath() {
        QueryPlan plan = parser.parse("SELECT * FROM `/path/to/file.csv`");

        assertNotNull(plan);
        assertEquals("/path/to/file.csv", plan.getTables().get(0).getName());
    }

    @Test
    void testParseInExpression() {
        QueryPlan plan = parser.parse("SELECT * FROM employees WHERE department IN ('Engineering', 'Sales')");

        assertNotNull(plan);
        assertTrue(plan.hasWhere());
    }

    @Test
    void testParseLikeExpression() {
        QueryPlan plan = parser.parse("SELECT * FROM employees WHERE name LIKE 'John%'");

        assertNotNull(plan);
        assertTrue(plan.hasWhere());
    }

    @Test
    void testInvalidSql() {
        assertThrows(SqlSyntaxException.class, () -> {
            parser.parse("INVALID SQL STATEMENT");
        });
    }

    @Test
    void testIsValid() {
        assertTrue(parser.isValid("SELECT * FROM employees"));
        assertFalse(parser.isValid("INVALID SQL"));
    }
}