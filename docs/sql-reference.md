# SQL Syntax Reference

This document describes the SQL syntax supported by CSV SQL Parser.

## SELECT Statement

### Basic Syntax

```sql
SELECT [columns] FROM table_name [WHERE condition] [GROUP BY columns] [HAVING condition] [ORDER BY columns] [LIMIT n [OFFSET m]]
```

### Column Selection

```sql
-- Select all columns
SELECT * FROM employees

-- Select specific columns
SELECT name, salary FROM employees

-- Select with alias
SELECT name AS employee_name, salary * 1.1 AS new_salary FROM employees
```

## WHERE Clause

### Comparison Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `=` | Equal | `WHERE age = 30` |
| `!=` or `<>` | Not equal | `WHERE status != 'inactive'` |
| `>` | Greater than | `WHERE salary > 50000` |
| `>=` | Greater than or equal | `WHERE age >= 25` |
| `<` | Less than | `WHERE price < 100` |
| `<=` | Less than or equal | `WHERE quantity <= 10` |

### Logical Operators

```sql
-- AND
WHERE age > 25 AND salary < 100000

-- OR
WHERE department = 'Sales' OR department = 'Marketing'

-- NOT
WHERE NOT status = 'inactive'

-- Parentheses for grouping
WHERE (age > 30 OR salary > 80000) AND department = 'Engineering'
```

### NULL Handling

```sql
-- IS NULL
WHERE manager_id IS NULL

-- IS NOT NULL
WHERE email IS NOT NULL
```

### IN Operator

```sql
-- IN with values
WHERE department IN ('Engineering', 'Sales', 'Marketing')

-- NOT IN
WHERE status NOT IN ('inactive', 'deleted')
```

### LIKE Operator

```sql
-- Starts with
WHERE name LIKE 'John%'

-- Ends with
WHERE email LIKE '%@company.com'

-- Contains
WHERE description LIKE '%error%'

-- Single character wildcard
WHERE code LIKE 'A_123'
```

### BETWEEN

```sql
WHERE salary BETWEEN 50000 AND 100000
```

## ORDER BY Clause

```sql
-- Ascending (default)
ORDER BY name ASC

-- Descending
ORDER BY salary DESC

-- Multiple columns
ORDER BY department ASC, salary DESC
```

## GROUP BY Clause

```sql
-- Group by single column
SELECT department, COUNT(*) FROM employees GROUP BY department

-- Group by multiple columns
SELECT department, location, AVG(salary) FROM employees GROUP BY department, location
```

## HAVING Clause

```sql
-- Filter after GROUP BY
SELECT department, COUNT(*) AS count FROM employees GROUP BY department HAVING COUNT(*) > 5
```

## LIMIT and OFFSET

```sql
-- Limit results
SELECT * FROM employees LIMIT 10

-- Limit with offset
SELECT * FROM employees LIMIT 10 OFFSET 20
```

## JOIN

### INNER JOIN

```sql
SELECT e.name, d.name AS department
FROM employees e
JOIN departments d ON e.department_id = d.id
```

### LEFT JOIN

```sql
SELECT e.name, d.name AS department
FROM employees e
LEFT JOIN departments d ON e.department_id = d.id
```

### RIGHT JOIN

```sql
SELECT e.name, d.name AS department
FROM employees e
RIGHT JOIN departments d ON e.department_id = d.id
```

## Aggregate Functions

| Function | Description | Example |
|----------|-------------|---------|
| `COUNT(*)` | Count all rows | `SELECT COUNT(*) FROM employees` |
| `COUNT(column)` | Count non-null values | `SELECT COUNT(manager_id) FROM employees` |
| `SUM(column)` | Sum of values | `SELECT SUM(salary) FROM employees` |
| `AVG(column)` | Average of values | `SELECT AVG(age) FROM employees` |
| `MIN(column)` | Minimum value | `SELECT MIN(price) FROM products` |
| `MAX(column)` | Maximum value | `SELECT MAX(salary) FROM employees` |

## String Functions

| Function | Description | Example |
|----------|-------------|---------|
| `UPPER(s)` | Convert to uppercase | `UPPER(name)` |
| `LOWER(s)` | Convert to lowercase | `LOWER(email)` |
| `LENGTH(s)` | String length | `LENGTH(description)` |
| `CONCAT(s1, s2, ...)` | Concatenate strings | `CONCAT(first_name, ' ', last_name)` |
| `SUBSTRING(s, start, len)` | Extract substring | `SUBSTRING(phone, 1, 3)` |
| `TRIM(s)` | Remove leading/trailing spaces | `TRIM(name)` |

## Math Functions

| Function | Description | Example |
|----------|-------------|---------|
| `ABS(n)` | Absolute value | `ABS(balance)` |
| `ROUND(n, decimals)` | Round to decimals | `ROUND(price, 2)` |
| `CEILING(n)` | Round up | `CEILING(average)` |
| `FLOOR(n)` | Round down | `FLOOR(ratio)` |

## Date Functions

| Function | Description | Example |
|----------|-------------|---------|
| `YEAR(date)` | Extract year | `YEAR(hire_date)` |
| `MONTH(date)` | Extract month | `MONTH(birth_date)` |
| `DAY(date)` | Extract day | `DAY(created_at)` |

## Conditional Expressions

### CASE WHEN

```sql
SELECT name,
  CASE
    WHEN salary >= 100000 THEN 'Executive'
    WHEN salary >= 70000 THEN 'Senior'
    ELSE 'Junior'
  END AS level
FROM employees
```

### COALESCE

Returns the first non-null value:

```sql
SELECT COALESCE(phone, mobile, 'N/A') AS contact_number FROM customers
```

### IFNULL

Returns alternative if value is null:

```sql
SELECT IFNULL(bonus, 0) AS bonus_amount FROM employees
```

## Arithmetic Expressions

```sql
-- Addition
SELECT price + tax AS total FROM products

-- Subtraction
SELECT salary - deduction AS net_salary FROM payroll

-- Multiplication
SELECT quantity * unit_price AS total FROM orders

-- Division
SELECT total / quantity AS unit_price FROM orders

-- Modulo
SELECT id % 10 AS bucket FROM items
```

## Type Casting

```sql
-- CAST
SELECT CAST(price AS INTEGER) FROM products

-- CONVERT (alias for CAST)
SELECT CONVERT(salary, DECIMAL) FROM employees
```

## Comments

```sql
-- Single line comment
SELECT * FROM employees  -- This is a comment

/* Multi-line
   comment */
SELECT * FROM products
```

## Reserved Keywords

The following keywords are reserved and should be quoted if used as identifiers:

```
SELECT, FROM, WHERE, JOIN, LEFT, RIGHT, INNER, ON, AND, OR, NOT, IN, LIKE,
BETWEEN, IS, NULL, AS, ORDER, BY, ASC, DESC, GROUP, HAVING, LIMIT, OFFSET,
COUNT, SUM, AVG, MIN, MAX, UPPER, LOWER, LENGTH, CONCAT, SUBSTRING, TRIM,
CASE, WHEN, THEN, ELSE, END, COALESCE, IFNULL, CAST, CONVERT, TRUE, FALSE
```

## Limitations

- Only SELECT statements are supported (no INSERT, UPDATE, DELETE)
- Subqueries are not supported
- UNION is not supported
- Window functions are not supported
- Common Table Expressions (CTE) are not supported