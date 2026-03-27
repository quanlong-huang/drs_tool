# CSV SQL Parser 构建使用手册

**版本**: 1.0.0
**日期**: 2026-03-27

---

## 目录

1. [环境要求](#1-环境要求)
2. [快速开始](#2-快速开始)
3. [构建指南](#3-构建指南)
4. [安装部署](#4-安装部署)
5. [使用指南](#5-使用指南)
6. [SQL 语法参考](#6-sql-语法参考)
7. [配置选项](#7-配置选项)
8. [故障排除](#8-故障排除)
9. [示例场景](#9-示例场景)

---

## 1. 环境要求

### 1.1 系统要求

| 要求 | 规格 |
|------|------|
| **Java 版本** | JDK 8 或更高版本 (推荐 JDK 11+) |
| **Maven 版本** | Maven 3.6+ |
| **内存** | 最小 256MB，推荐 1GB+ (处理大文件建议 2GB+) |
| **操作系统** | Windows / macOS / Linux |
| **磁盘空间** | 100MB+ (用于程序和依赖) |

### 1.2 验证环境

```bash
# 检查 Java 版本
java -version
# 输出示例:
# java version "1.8.0_xxx" 或更高

# 检查 Maven 版本
mvn -version
# 输出示例:
# Apache Maven 3.9.x
```

### 1.3 依赖说明

项目依赖以下库 (由 Maven 自动管理):

| 库 | 版本 | 用途 |
|---|------|------|
| JSqlParser | 4.6 | SQL 语法解析 |
| Apache Commons CSV | 1.10.0 | CSV 文件处理 |
| Picocli | 4.7.5 | 命令行框架 |
| Jackson | 2.15.2 | JSON 处理 |
| Jansi | 2.4.0 | 终端颜色支持 |
| SLF4J + Logback | 1.7.36 / 1.2.12 | 日志框架 |

---

## 2. 快速开始

### 2.1 下载与构建

```bash
# 克隆或下载项目
cd /path/to/project

# 构建项目
mvn clean package -DskipTests

# 构建完成后，可执行 JAR 位于:
# target/csv-sql-parser-1.0.0-SNAPSHOT.jar
```

### 2.2 运行第一个查询

```bash
# 假设有一个 data.csv 文件
java -jar target/csv-sql-parser-1.0.0-SNAPSHOT.jar query "SELECT * FROM data.csv LIMIT 10"

# 或者使用交互模式
java -jar target/csv-sql-parser-1.0.0-SNAPSHOT.jar repl
```

### 2.3 示例输出

```
┌────┬──────────┬────────────┬────────┐
│ id │ name     │ department │ salary │
├────┼──────────┼────────────┼────────┤
│  1 │ Alice    │ Engineering│  75000 │
│  2 │ Bob      │ Sales      │  65000 │
│  3 │ Charlie  │ Marketing  │  70000 │
└────┴──────────┴────────────┴────────┘
(3 rows, 15ms)
```

---

## 3. 构建指南

### 3.1 标准构建

```bash
# 完整构建 (包含测试)
mvn clean package

# 跳过测试构建
mvn clean package -DskipTests

# 仅编译
mvn clean compile
```

### 3.2 构建产物

```
target/
├── classes/                              # 编译后的类文件
├── test-classes/                         # 测试类文件
├── csv-sql-parser-1.0.0-SNAPSHOT.jar     # 原始 JAR (不含依赖)
├── csv-sql-parser-1.0.0-SNAPSHOT-shaded.jar  # 可执行 JAR (含所有依赖)
├── surefire-reports/                     # 测试报告
└── site/                                 # 项目站点 (如果生成)
```

### 3.3 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=SqlParserTest

# 运行特定测试方法
mvn test -Dtest=QueryExecutorTest#testSimpleSelect

# 生成测试覆盖率报告
mvn test jacoco:report
# 报告位置: target/site/jacoco/index.html
```

### 3.4 生成文档

```bash
# 生成 Javadoc
mvn javadoc:javadoc
# 文档位置: target/site/apidocs/

# 生成项目站点
mvn site
# 站点位置: target/site/
```

### 3.5 常见构建问题

**问题 1: 编码错误**
```bash
# 解决方案: 指定编码
mvn clean package -Dproject.build.sourceEncoding=UTF-8
```

**问题 2: 内存不足**
```bash
# 解决方案: 增加 Maven 内存
export MAVEN_OPTS="-Xmx1024m"
mvn clean package
```

**问题 3: 依赖下载失败**
```bash
# 解决方案: 更新依赖
mvn clean install -U
```

---

## 4. 安装部署

### 4.1 方式一: 直接运行 JAR

```bash
# 构建后直接运行
java -jar target/csv-sql-parser-1.0.0-SNAPSHOT.jar --help
```

### 4.2 方式二: 创建命令别名

**Linux/macOS (Bash)**:
```bash
# 添加到 ~/.bashrc 或 ~/.zshrc
alias csv-sql='java -jar /path/to/csv-sql-parser-1.0.0-SNAPSHOT.jar'

# 生效
source ~/.bashrc

# 使用
csv-sql query "SELECT * FROM data.csv"
```

**Windows (PowerShell)**:
```powershell
# 添加到 PowerShell 配置文件
function csv-sql { java -jar "C:\path\to\csv-sql-parser-1.0.0-SNAPSHOT.jar" $args }

# 使用
csv-sql query "SELECT * FROM data.csv"
```

### 4.3 方式三: 添加到 PATH

```bash
# 创建启动脚本
cat > /usr/local/bin/csv-sql << 'EOF'
#!/bin/bash
java -jar /path/to/csv-sql-parser-1.0.0-SNAPSHOT.jar "$@"
EOF

# 添加执行权限
chmod +x /usr/local/bin/csv-sql

# 使用
csv-sql query "SELECT * FROM data.csv"
```

### 4.4 方式四: 作为库使用

在 Maven 项目中添加依赖:

```xml
<dependency>
    <groupId>com.csvsql</groupId>
    <artifactId>csv-sql-parser</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

程序化使用:

```java
import com.csvsql.parser.parser.SqlParser;
import com.csvsql.parser.engine.QueryExecutor;
import com.csvsql.parser.data.TableRegistry;
import com.csvsql.parser.output.ResultSet;

// 初始化
TableRegistry registry = new TableRegistry();
SqlParser parser = new SqlParser();
QueryExecutor executor = new QueryExecutor(registry);

// 执行查询
QueryPlan plan = parser.parse("SELECT * FROM data.csv WHERE id > 10");
ResultSet result = executor.execute(plan);

// 处理结果
for (Map<String, Object> row : result.getRows()) {
    System.out.println(row);
}
```

---

## 5. 使用指南

### 5.1 命令概览

```bash
java -jar csv-sql-parser.jar [command] [options]

Commands:
  query    执行单次 SQL 查询
  repl     启动交互式 REPL 会话
  batch    批量执行 SQL 文件

Global Options:
  --help     显示帮助信息
  --version  显示版本信息
```

### 5.2 Query 命令

**语法**:
```bash
java -jar csv-sql-parser.jar query <sql> [options]
```

**参数**:
| 参数 | 简写 | 说明 | 默认值 |
|------|------|------|--------|
| `<sql>` | - | SQL 查询语句 (必需) | - |
| `--format` | `-f` | 输出格式: table, csv, json | table |
| `--output` | `-o` | 输出文件路径 | stdout |
| `--delimiter` | `-d` | CSV 分隔符 | , |
| `--encoding` | `-e` | 文件编码 | UTF-8 |
| `--no-header` | - | CSV 无标题行 | false |
| `--verbose` | `-v` | 显示详细信息 | false |

**示例**:

```bash
# 基本查询
java -jar csv-sql-parser.jar query "SELECT * FROM employees.csv LIMIT 10"

# 条件过滤
java -jar csv-sql-parser.jar query "SELECT name, salary FROM employees.csv WHERE salary > 50000"

# 聚合查询
java -jar csv-sql-parser.jar query "SELECT department, COUNT(*) as count, AVG(salary) FROM employees.csv GROUP BY department"

# JSON 输出
java -jar csv-sql-parser.jar query "SELECT * FROM employees.csv LIMIT 5" -f json

# 保存到文件
java -jar csv-sql-parser.jar query "SELECT * FROM employees.csv" -o result.csv -f csv

# 处理无标题 CSV
java -jar csv-sql-parser.jar query "SELECT * FROM data.csv" --no-header

# 自定义编码
java -jar csv-sql-parser.jar query "SELECT * FROM chinese_data.csv" -e GBK
```

### 5.3 REPL 命令

**启动 REPL**:
```bash
java -jar csv-sql-parser.jar repl [options]

Options:
  --history, -H   历史文件路径   默认: ~/.csv_sql_history
  --prompt        自定义提示符   默认: csv-sql>
```

**REPL 内置命令**:

| 命令 | 说明 |
|------|------|
| `.help` | 显示帮助信息 |
| `.quit` / `.exit` | 退出 REPL |
| `.version` | 显示版本信息 |
| `.format <type>` | 设置输出格式 (table/csv/json) |
| `.output <file>` | 重定向输出到文件 |
| `.tables` | 列出已加载的表 |
| `.schema <table>` | 显示表结构 |
| `.load <file> [alias]` | 加载 CSV 文件 |

**REPL 会话示例**:

```
$ java -jar csv-sql-parser.jar repl
CSV SQL Parser v1.0.0
Type '.help' for available commands, '.quit' to exit.

csv-sql> SELECT * FROM employees.csv LIMIT 3
┌────┬─────────┬────────────┬────────┐
│ id │ name    │ department │ salary │
├────┼─────────┼────────────┼────────┤
│  1 │ Alice   │ Engineering│  75000 │
│  2 │ Bob     │ Sales      │  65000 │
│  3 │ Charlie │ Marketing  │  70000 │
└────┴─────────┴────────────┴────────┘
(3 rows, 12ms)

csv-sql> .format json
Output format set to: json

csv-sql> SELECT COUNT(*) FROM employees.csv
{
  "columns" : [ "COUNT(*)" ],
  "rows" : [ {
    "COUNT(*)" : 100
  } ],
  "rowCount" : 1,
  "executionTimeMs" : 8
}

csv-sql> .quit
Goodbye!
```

### 5.4 Batch 命令

**语法**:
```bash
java -jar csv-sql-parser.jar batch <file> [options]

Arguments:
  <file>   SQL 文件路径 (每行一个查询或用 ; 分隔)

Options:
  --format, -f        输出格式
  --output-dir, -o    输出目录
  --stop-on-error     遇错停止
  --verbose, -v       显示进度
```

**SQL 文件示例** (`queries.sql`):
```sql
-- 查询高薪员工
SELECT * FROM employees.csv WHERE salary > 80000 ORDER BY salary DESC;

-- 按部门统计
SELECT department, COUNT(*) as count, AVG(salary) as avg_salary
FROM employees.csv
GROUP BY department;

-- JOIN 查询
SELECT o.order_id, c.name, o.total
FROM orders.csv o
JOIN customers.csv c ON o.customer_id = c.id;
```

**执行批处理**:
```bash
java -jar csv-sql-parser.jar batch queries.sql --verbose
```

---

## 6. SQL 语法参考

### 6.1 支持的 SQL 特性

**SELECT 语句**:
```sql
SELECT [DISTINCT] column1, column2, ...
FROM table_name
[WHERE condition]
[GROUP BY column1, ...]
[HAVING condition]
[ORDER BY column1 [ASC|DESC], ...]
[LIMIT n]
[OFFSET m]
```

**JOIN 操作**:
```sql
-- INNER JOIN
SELECT * FROM table1.csv
INNER JOIN table2.csv ON table1.id = table2.id

-- LEFT JOIN
SELECT * FROM table1.csv
LEFT JOIN table2.csv ON table1.id = table2.id

-- RIGHT JOIN
SELECT * FROM table1.csv
RIGHT JOIN table2.csv ON table1.id = table2.id
```

**聚合函数**:
| 函数 | 说明 | 示例 |
|------|------|------|
| `COUNT(*)` | 计数 | `SELECT COUNT(*) FROM table.csv` |
| `COUNT(column)` | 非空计数 | `SELECT COUNT(name) FROM table.csv` |
| `SUM(column)` | 求和 | `SELECT SUM(salary) FROM table.csv` |
| `AVG(column)` | 平均值 | `SELECT AVG(age) FROM table.csv` |
| `MIN(column)` | 最小值 | `SELECT MIN(price) FROM table.csv` |
| `MAX(column)` | 最大值 | `SELECT MAX(score) FROM table.csv` |

**字符串函数**:
| 函数 | 说明 | 示例 |
|------|------|------|
| `UPPER(s)` | 大写 | `UPPER(name)` |
| `LOWER(s)` | 小写 | `LOWER(name)` |
| `LENGTH(s)` | 长度 | `LENGTH(description)` |
| `CONCAT(s1, s2)` | 连接 | `CONCAT(first, last)` |
| `SUBSTRING(s, start, len)` | 子串 | `SUBSTRING(name, 1, 5)` |
| `TRIM(s)` | 去空格 | `TRIM(name)` |

**日期函数**:
| 函数 | 说明 | 示例 |
|------|------|------|
| `YEAR(date)` | 年份 | `YEAR(birth_date)` |
| `MONTH(date)` | 月份 | `MONTH(order_date)` |
| `DAY(date)` | 日 | `DAY(create_time)` |

**数学函数**:
| 函数 | 说明 | 示例 |
|------|------|------|
| `ROUND(n, d)` | 四舍五入 | `ROUND(price, 2)` |
| `ABS(n)` | 绝对值 | `ABS(diff)` |
| `CEILING(n)` | 向上取整 | `CEILING(4.3)` → 5 |
| `FLOOR(n)` | 向下取整 | `FLOOR(4.7)` → 4 |

**表达式**:
```sql
-- CASE WHEN
SELECT
    name,
    CASE
        WHEN salary > 80000 THEN 'High'
        WHEN salary > 50000 THEN 'Medium'
        ELSE 'Low'
    END as salary_level
FROM employees.csv

-- COALESCE
SELECT COALESCE(phone, email, 'N/A') as contact FROM users.csv

-- CAST
SELECT CAST(price AS INT) FROM products.csv

-- 算术运算
SELECT price * quantity as total FROM orders.csv
```

**WHERE 条件操作符**:
| 操作符 | 说明 | 示例 |
|------|------|------|
| `=`, `!=` | 等于/不等于 | `WHERE id = 1` |
| `<`, `>`, `<=`, `>=` | 比较 | `WHERE salary > 50000` |
| `AND`, `OR`, `NOT` | 逻辑 | `WHERE a > 1 AND b < 10` |
| `LIKE` | 模式匹配 | `WHERE name LIKE 'A%'` |
| `IN` | 包含 | `WHERE id IN (1, 2, 3)` |
| `IS NULL` / `IS NOT NULL` | NULL 判断 | `WHERE phone IS NULL` |

### 6.2 不支持的特性

以下 SQL 特性不受支持:

- `INSERT`, `UPDATE`, `DELETE` 语句
- 子查询 (Subqueries)
- `UNION`, `INTERSECT`, `EXCEPT`
- 窗口函数 (Window Functions)
- 公用表表达式 (CTE / WITH 子句)
- 存储过程和触发器
- 视图 (Views)

---

## 7. 配置选项

### 7.1 JVM 选项

```bash
# 设置最大内存
java -Xmx2g -jar csv-sql-parser.jar query "SELECT * FROM large.csv"

# 设置初始内存
java -Xms512m -Xmx2g -jar csv-sql-parser.jar repl

# 启用 G1 垃圾回收器 (大文件推荐)
java -XX:+UseG1GC -Xmx4g -jar csv-sql-parser.jar query "..."
```

### 7.2 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `CSV_SQL_MAX_MEMORY` | 最大内存使用 (MB) | 自动检测 |
| `CSV_SQL_ENCODING` | 默认编码 | UTF-8 |
| `CSV_SQL_HISTORY` | REPL 历史文件 | ~/.csv_sql_history |

### 7.3 日志配置

日志配置文件: `src/main/resources/logback.xml`

```xml
<!-- 调整日志级别 -->
<root level="INFO">
    <appender-ref ref="CONSOLE" />
</root>

<!-- 输出到文件 -->
<appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <file>csv-sql-parser.log</file>
    <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```

---

## 8. 故障排除

### 8.1 常见错误

**错误 1: 文件未找到**

```
Error: File not found: data.csv
💡 Did you mean: ./data.csv?
```

解决方案: 检查文件路径是否正确，使用绝对路径或相对于当前目录的正确路径。

**错误 2: SQL 语法错误**

```
✗ SQL Syntax Error
Unexpected token 'SELEC' at line 1, column 1

  1 | SELEC * FROM data.csv
    | ^^^^^

💡 Did you mean 'SELECT'?
```

解决方案: 检查 SQL 语法，参考错误提示修正拼写错误。

**错误 3: 列未找到**

```
✗ Column Not Found
Column 'nam' does not exist

Available columns: id, name, department, salary

💡 Did you mean: name?
```

解决方案: 检查列名拼写，参考可用列列表。

**错误 4: 不支持的功能**

```
✗ Unsupported Feature
Unsupported statement type: INSERT

💡 INSERT, UPDATE, DELETE statements are not supported.
   This tool is for querying CSV files only.
```

解决方案: 使用支持的 SELECT 语法，参考 SQL 语法参考章节。

### 8.2 性能问题

**问题: 大文件加载慢**

解决方案:
```bash
# 增加内存
java -Xmx4g -jar csv-sql-parser.jar query "..."

# 使用 WHERE 条件早期过滤
java -jar csv-sql-parser.jar query "SELECT * FROM large.csv WHERE id > 100000"
```

**问题: JOIN 操作慢**

解决方案:
- 确保 JOIN 列有合适的数据类型
- 先用 WHERE 条件过滤数据
- 考虑拆分大查询

### 8.3 编码问题

**问题: 中文乱码**

解决方案:
```bash
# 指定 GBK 编码
java -jar csv-sql-parser.jar query "SELECT * FROM chinese.csv" -e GBK

# 或指定 GB2312
java -jar csv-sql-parser.jar query "..." -e GB2312
```

### 8.4 获取帮助

```bash
# 显示命令帮助
java -jar csv-sql-parser.jar --help
java -jar csv-sql-parser.jar query --help
java -jar csv-sql-parser.jar repl --help

# 启用详细模式
java -jar csv-sql-parser.jar query "..." -v
```

---

## 9. 示例场景

### 9.1 数据探索

```bash
# 查看数据结构
csv-sql query "SELECT * FROM sales.csv LIMIT 5"

# 统计行数
csv-sql query "SELECT COUNT(*) FROM sales.csv"

# 查看列范围
csv-sql query "SELECT MIN(date), MAX(date), MIN(amount), MAX(amount) FROM sales.csv"
```

### 9.2 数据分析

```bash
# 按类别汇总
csv-sql query "
SELECT
    category,
    COUNT(*) as count,
    SUM(amount) as total,
    AVG(amount) as average
FROM sales.csv
GROUP BY category
ORDER BY total DESC"

# 时间趋势分析
csv-sql query "
SELECT
    YEAR(date) as year,
    MONTH(date) as month,
    SUM(amount) as monthly_total
FROM sales.csv
GROUP BY YEAR(date), MONTH(date)
ORDER BY year DESC, month DESC"
```

### 9.3 数据关联

```bash
# JOIN 示例: 订单与客户
csv-sql query "
SELECT
    o.order_id,
    c.name as customer_name,
    o.order_date,
    o.total
FROM orders.csv o
JOIN customers.csv c ON o.customer_id = c.customer_id
WHERE o.total > 1000
ORDER BY o.total DESC
LIMIT 20"
```

### 9.4 数据清洗

```bash
# 查找缺失值
csv-sql query "SELECT * FROM data.csv WHERE phone IS NULL"

# 查找重复记录
csv-sql query "
SELECT email, COUNT(*) as count
FROM users.csv
GROUP BY email
HAVING count > 1"

# 数据格式化
csv-sql query "
SELECT
    UPPER(TRIM(name)) as name,
    CAST(salary AS INT) as salary,
    COALESCE(phone, 'N/A') as phone
FROM employees.csv"
```

### 9.5 数据导出

```bash
# 导出为 CSV
csv-sql query "SELECT * FROM large.csv WHERE year = 2023" -o filtered.csv -f csv

# 导出为 JSON
csv-sql query "SELECT * FROM products.csv WHERE price > 100" -o products.json -f json

# 导出报表
csv-sql query "
SELECT
    department,
    COUNT(*) as employee_count,
    AVG(salary) as avg_salary
FROM employees.csv
GROUP BY department
ORDER BY avg_salary DESC" -o department_report.csv -f csv
```

---

## 附录 A: 命令速查表

```bash
# 查询模式
csv-sql query "SELECT * FROM file.csv"                    # 基本查询
csv-sql query "SELECT * FROM file.csv LIMIT 10" -f json   # JSON 输出
csv-sql query "SELECT * FROM file.csv" -o output.csv      # 保存到文件

# REPL 模式
csv-sql repl                                              # 启动 REPL
csv-sql> .help                                            # 显示帮助
csv-sql> .format json                                     # 切换格式
csv-sql> .quit                                            # 退出

# 批处理模式
csv-sql batch queries.sql                                 # 执行 SQL 文件
csv-sql batch queries.sql -o ./results/                   # 输出到目录
```

---

## 附录 B: 性能调优建议

| 场景 | 建议 |
|------|------|
| 大文件 (>100MB) | 增加 JVM 内存: `-Xmx4g` |
| 复杂 JOIN | 先用 WHERE 过滤，再 JOIN |
| 大量聚合 | 考虑预先排序数据 |
| 内存受限 | 使用 LIMIT 限制结果集 |
| 编码问题 | 指定正确编码: `-e GBK` |

---

## 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|----------|
| 1.0.0 | 2026-03-27 | 初始版本 |