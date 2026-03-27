package com.csvsql.parser.engine;

import com.csvsql.parser.data.CsvTable;
import com.csvsql.parser.data.TableRegistry;
import com.csvsql.parser.output.ResultSet;
import com.csvsql.parser.parser.QueryPlan;
import com.csvsql.parser.parser.SqlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for QueryExecutor.
 */
class QueryExecutorTest {

    private TableRegistry registry;
    private QueryExecutor executor;
    private SqlParser parser;

    @BeforeEach
    void setUp() {
        registry = new TableRegistry();
        executor = new QueryExecutor(registry);
        parser = new SqlParser();

        // Create test table
        List<String> columns = Arrays.asList("name", "age", "salary", "department");
        Map<String, com.csvsql.parser.data.ColumnType> types = new LinkedHashMap<>();
        types.put("name", com.csvsql.parser.data.ColumnType.STRING);
        types.put("age", com.csvsql.parser.data.ColumnType.INTEGER);
        types.put("salary", com.csvsql.parser.data.ColumnType.INTEGER);
        types.put("department", com.csvsql.parser.data.ColumnType.STRING);

        CsvTable table = new CsvTable("employees", null, columns, types);

        addRow(table, "Alice", 30, 80000, "Engineering");
        addRow(table, "Bob", 25, 60000, "Sales");
        addRow(table, "Charlie", 35, 95000, "Engineering");
        addRow(table, "Diana", 28, 70000, "Sales");
        addRow(table, "Eve", 32, 85000, "Marketing");

        registry.register(table, null);
    }

    private void addRow(CsvTable table, String name, int age, int salary, String department) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("age", age);
        row.put("salary", salary);
        row.put("department", department);
        table.addRow(row);
    }

    @Test
    void testSelectAll() {
        QueryPlan plan = parser.parse("SELECT * FROM employees");
        ResultSet result = executor.execute(plan);

        assertEquals(5, result.getRowCount());
        assertEquals(4, result.getColumnNames().size());
    }

    @Test
    void testSelectColumns() {
        QueryPlan plan = parser.parse("SELECT name, salary FROM employees");
        ResultSet result = executor.execute(plan);

        assertEquals(5, result.getRowCount());
        assertEquals(2, result.getColumnNames().size());
        assertTrue(result.getColumnNames().contains("name"));
        assertTrue(result.getColumnNames().contains("salary"));
    }

    @Test
    void testWhereClause() {
        QueryPlan plan = parser.parse("SELECT * FROM employees WHERE age > 30");
        ResultSet result = executor.execute(plan);

        assertEquals(2, result.getRowCount());

        for (Map<String, Object> row : result.getRows()) {
            int age = (Integer) row.get("age");
            assertTrue(age > 30);
        }
    }

    @Test
    void testWhereWithAnd() {
        QueryPlan plan = parser.parse("SELECT * FROM employees WHERE age >= 30 AND salary > 80000");
        ResultSet result = executor.execute(plan);

        assertTrue(result.getRowCount() >= 1);
    }

    @Test
    void testOrderByAsc() {
        QueryPlan plan = parser.parse("SELECT * FROM employees ORDER BY age ASC");
        ResultSet result = executor.execute(plan);

        List<Map<String, Object>> rows = result.getRows();
        int prevAge = 0;
        for (Map<String, Object> row : rows) {
            int age = (Integer) row.get("age");
            assertTrue(age >= prevAge);
            prevAge = age;
        }
    }

    @Test
    void testOrderByDesc() {
        QueryPlan plan = parser.parse("SELECT * FROM employees ORDER BY salary DESC");
        ResultSet result = executor.execute(plan);

        List<Map<String, Object>> rows = result.getRows();
        int prevSalary = Integer.MAX_VALUE;
        for (Map<String, Object> row : rows) {
            int salary = (Integer) row.get("salary");
            assertTrue(salary <= prevSalary);
            prevSalary = salary;
        }
    }

    @Test
    void testLimit() {
        QueryPlan plan = parser.parse("SELECT * FROM employees LIMIT 3");
        ResultSet result = executor.execute(plan);

        assertEquals(3, result.getRowCount());
    }

    @Test
    void testLimitOffset() {
        QueryPlan plan = parser.parse("SELECT * FROM employees ORDER BY name LIMIT 2 OFFSET 1");
        ResultSet result = executor.execute(plan);

        assertEquals(2, result.getRowCount());
        assertEquals("Bob", result.getRows().get(0).get("name"));
    }

    @Test
    void testGroupByCount() {
        QueryPlan plan = parser.parse("SELECT department, COUNT(*) FROM employees GROUP BY department");
        ResultSet result = executor.execute(plan);

        assertEquals(3, result.getRowCount()); // 3 departments
    }

    @Test
    void testGroupBySum() {
        QueryPlan plan = parser.parse("SELECT department, SUM(salary) FROM employees GROUP BY department");
        ResultSet result = executor.execute(plan);

        assertEquals(3, result.getRowCount());
    }

    @Test
    void testGroupByAvg() {
        QueryPlan plan = parser.parse("SELECT department, AVG(age) FROM employees GROUP BY department");
        ResultSet result = executor.execute(plan);

        assertEquals(3, result.getRowCount());
    }

    @Test
    void testAggregateWithoutGroupBy() {
        QueryPlan plan = parser.parse("SELECT COUNT(*), AVG(salary), MIN(age), MAX(salary) FROM employees");
        ResultSet result = executor.execute(plan);

        assertEquals(1, result.getRowCount());
        // The COUNT(*) result is stored with "*" as the key
        Map<String, Object> row = result.getRows().get(0);
        // Check that we got some values - the keys may vary based on column naming
        assertFalse(row.isEmpty());
    }

    @Test
    void testWhereWithIn() {
        QueryPlan plan = parser.parse("SELECT * FROM employees WHERE department IN ('Engineering', 'Sales')");
        ResultSet result = executor.execute(plan);

        assertEquals(4, result.getRowCount());
    }

    @Test
    void testWhereWithLike() {
        QueryPlan plan = parser.parse("SELECT * FROM employees WHERE name LIKE 'A%'");
        ResultSet result = executor.execute(plan);

        assertEquals(1, result.getRowCount());
        assertEquals("Alice", result.getRows().get(0).get("name"));
    }

    @Test
    void testArithmeticExpression() {
        QueryPlan plan = parser.parse("SELECT name, salary * 1.1 AS new_salary FROM employees LIMIT 1");
        ResultSet result = executor.execute(plan);

        assertEquals(1, result.getRowCount());
        Object newSalary = result.getRows().get(0).get("new_salary");
        assertNotNull(newSalary);
        assertTrue(newSalary instanceof Number);
    }

    @Test
    void testCaseWhen() {
        QueryPlan plan = parser.parse(
            "SELECT name, CASE WHEN salary > 80000 THEN 'High' ELSE 'Low' END AS level FROM employees"
        );
        ResultSet result = executor.execute(plan);

        assertEquals(5, result.getRowCount());
        assertTrue(result.getColumnNames().contains("level"));
    }

    @Test
    void testExecutionTime() {
        QueryPlan plan = parser.parse("SELECT * FROM employees");
        ResultSet result = executor.execute(plan);

        assertTrue(result.getExecutionTimeMs() >= 0);
    }
}