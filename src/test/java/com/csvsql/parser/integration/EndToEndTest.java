package com.csvsql.parser.integration;

import com.csvsql.parser.data.CsvLoader;
import com.csvsql.parser.data.CsvTable;
import com.csvsql.parser.data.TableRegistry;
import com.csvsql.parser.engine.QueryExecutor;
import com.csvsql.parser.output.*;
import com.csvsql.parser.parser.QueryPlan;
import com.csvsql.parser.parser.SqlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests.
 */
class EndToEndTest {

    @TempDir
    Path tempDir;

    private File employeesFile;
    private File departmentsFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create test data files
        employeesFile = createCsvFile("employees.csv",
            "id,name,age,salary,department_id\n" +
            "1,Alice,30,80000,1\n" +
            "2,Bob,25,60000,2\n" +
            "3,Charlie,35,95000,1\n" +
            "4,Diana,28,70000,2\n" +
            "5,Eve,32,85000,3\n" +
            "6,Frank,45,120000,1\n");

        departmentsFile = createCsvFile("departments.csv",
            "id,name,location\n" +
            "1,Engineering,Building A\n" +
            "2,Sales,Building B\n" +
            "3,Marketing,Building C\n");
    }

    private File createCsvFile(String name, String content) throws IOException {
        File file = new File(tempDir.toFile(), name);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }

    @Test
    void testBasicQuery() throws IOException {
        TableRegistry registry = new TableRegistry();
        CsvLoader loader = new CsvLoader();
        CsvTable table = loader.load(employeesFile.getAbsolutePath());
        registry.register(table, "employees");

        SqlParser parser = new SqlParser();
        QueryPlan plan = parser.parse("SELECT * FROM employees LIMIT 3");

        QueryExecutor executor = new QueryExecutor(registry);
        ResultSet result = executor.execute(plan);

        assertEquals(3, result.getRowCount());
    }

    @Test
    void testWhereAndOrderBy() throws IOException {
        TableRegistry registry = new TableRegistry();
        CsvLoader loader = new CsvLoader();
        CsvTable table = loader.load(employeesFile.getAbsolutePath());
        registry.register(table, "employees");

        SqlParser parser = new SqlParser();
        QueryPlan plan = parser.parse(
            "SELECT name, salary FROM employees WHERE salary > 70000 ORDER BY salary DESC"
        );

        QueryExecutor executor = new QueryExecutor(registry);
        ResultSet result = executor.execute(plan);

        assertTrue(result.getRowCount() >= 3);

        // Verify descending order
        List<Map<String, Object>> rows = result.getRows();
        int prevSalary = Integer.MAX_VALUE;
        for (Map<String, Object> row : rows) {
            int salary = ((Number) row.get("salary")).intValue();
            assertTrue(salary <= prevSalary);
            prevSalary = salary;
        }
    }

    @Test
    void testGroupByAggregation() throws IOException {
        TableRegistry registry = new TableRegistry();
        CsvLoader loader = new CsvLoader();
        CsvTable table = loader.load(employeesFile.getAbsolutePath());
        registry.register(table, "employees");

        SqlParser parser = new SqlParser();
        QueryPlan plan = parser.parse(
            "SELECT department_id, COUNT(*) AS count, AVG(salary) AS avg_salary " +
            "FROM employees GROUP BY department_id ORDER BY department_id"
        );

        QueryExecutor executor = new QueryExecutor(registry);
        ResultSet result = executor.execute(plan);

        assertEquals(3, result.getRowCount());
    }

    @Test
    void testJoin() throws IOException {
        TableRegistry registry = new TableRegistry();
        CsvLoader loader = new CsvLoader();

        CsvTable employees = loader.load(employeesFile.getAbsolutePath());
        CsvTable departments = loader.load(departmentsFile.getAbsolutePath());

        registry.register(employees, "employees");
        registry.register(departments, "departments");

        SqlParser parser = new SqlParser();
        QueryPlan plan = parser.parse(
            "SELECT employees.name, departments.name AS dept_name " +
            "FROM employees " +
            "JOIN departments ON employees.department_id = departments.id"
        );

        QueryExecutor executor = new QueryExecutor(registry);
        ResultSet result = executor.execute(plan);

        assertEquals(6, result.getRowCount());
    }

    @Test
    void testOutputFormats() throws IOException {
        TableRegistry registry = new TableRegistry();
        CsvLoader loader = new CsvLoader();
        CsvTable table = loader.load(employeesFile.getAbsolutePath());
        registry.register(table, "employees");

        SqlParser parser = new SqlParser();
        QueryPlan plan = parser.parse("SELECT name, age FROM employees LIMIT 2");

        QueryExecutor executor = new QueryExecutor(registry);
        ResultSet result = executor.execute(plan);

        // Test CSV output
        CsvFormatter csvFormatter = new CsvFormatter();
        String csvOutput = csvFormatter.format(result);
        assertTrue(csvOutput.contains("name,age"));
        assertTrue(csvOutput.contains("Alice"));

        // Test JSON output
        JsonFormatter jsonFormatter = new JsonFormatter();
        String jsonOutput = jsonFormatter.format(result);
        assertTrue(jsonOutput.contains("\"name\" : \"Alice\""));
        assertTrue(jsonOutput.contains("\"rowCount\" : 2"));

        // Test Table output
        TableFormatter tableFormatter = new TableFormatter();
        String tableOutput = tableFormatter.format(result);
        assertTrue(tableOutput.contains("Alice"));
    }

    @Test
    void testComplexQuery() throws IOException {
        TableRegistry registry = new TableRegistry();
        CsvLoader loader = new CsvLoader();
        CsvTable table = loader.load(employeesFile.getAbsolutePath());
        registry.register(table, "employees");

        SqlParser parser = new SqlParser();
        QueryPlan plan = parser.parse(
            "SELECT " +
            "  department_id, " +
            "  COUNT(*) AS emp_count, " +
            "  SUM(salary) AS total_salary, " +
            "  AVG(age) AS avg_age " +
            "FROM employees " +
            "WHERE salary >= 60000 " +
            "GROUP BY department_id " +
            "ORDER BY total_salary DESC " +
            "LIMIT 2"
        );

        QueryExecutor executor = new QueryExecutor(registry);
        ResultSet result = executor.execute(plan);

        assertTrue(result.getRowCount() <= 2);
        assertTrue(result.getColumnNames().contains("emp_count"));
        assertTrue(result.getColumnNames().contains("total_salary"));
        assertTrue(result.getColumnNames().contains("avg_age"));
    }

    @Test
    void testCaseWhenExpression() throws IOException {
        TableRegistry registry = new TableRegistry();
        CsvLoader loader = new CsvLoader();
        CsvTable table = loader.load(employeesFile.getAbsolutePath());
        registry.register(table, "employees");

        SqlParser parser = new SqlParser();
        QueryPlan plan = parser.parse(
            "SELECT name, " +
            "  CASE WHEN salary >= 100000 THEN 'Executive' " +
            "       WHEN salary >= 80000 THEN 'Senior' " +
            "       ELSE 'Junior' END AS level " +
            "FROM employees ORDER BY salary DESC"
        );

        QueryExecutor executor = new QueryExecutor(registry);
        ResultSet result = executor.execute(plan);

        assertEquals(6, result.getRowCount());
        assertTrue(result.getColumnNames().contains("level"));
    }

    @Test
    void testInAndNotIn() throws IOException {
        TableRegistry registry = new TableRegistry();
        CsvLoader loader = new CsvLoader();
        CsvTable table = loader.load(employeesFile.getAbsolutePath());
        registry.register(table, "employees");

        SqlParser parser = new SqlParser();
        QueryPlan plan = parser.parse(
            "SELECT * FROM employees WHERE department_id IN (1, 2)"
        );

        QueryExecutor executor = new QueryExecutor(registry);
        ResultSet result = executor.execute(plan);

        assertEquals(5, result.getRowCount());
    }

    @Test
    void testFullPipeline() throws IOException {
        // Full pipeline: Load -> Parse -> Execute -> Format
        CsvLoader loader = new CsvLoader();
        CsvTable table = loader.load(employeesFile.getAbsolutePath());

        TableRegistry registry = new TableRegistry();
        registry.register(table, "employees");

        SqlParser parser = new SqlParser();
        QueryPlan plan = parser.parse(
            "SELECT name, salary * 1.1 AS new_salary FROM employees WHERE age > 30"
        );

        QueryExecutor executor = new QueryExecutor(registry);
        ResultSet result = executor.execute(plan);

        JsonFormatter formatter = new JsonFormatter();
        String output = formatter.format(result);

        assertNotNull(output);
        assertTrue(output.contains("\"rowCount\""));
        assertTrue(result.getRowCount() >= 2);
    }
}