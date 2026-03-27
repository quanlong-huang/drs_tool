# Feature Specification: CSV SQL Parser

**Version**: 1.1
**Created**: 2026-03-26
**Updated**: 2026-03-26
**Status**: Draft
**Author**: System Generated

---

## Executive Summary

CSV SQL Parser 是一个数据分析工具，允许用户使用标准 MySQL SQL 语法查询和分析 CSV 文件。这消除了用户在运行 SQL 查询之前将 CSV 数据导入数据库的需要，简化了数据分析师、开发人员和业务用户的数据探索工作流程。

---

## Problem Statement

### Current Situation

需要分析 CSV 文件的用户必须：
- 将数据导入数据库系统（耗时的设置）
- 使用查询功能有限的电子表格应用程序
- 为每个分析任务编写自定义脚本
- 学习具有不同查询语言的多种工具

### Pain Points

- **设置耗时**：将 CSV 文件导入数据库需要数据库管理知识和设置时间
- **查询能力有限**：电子表格应用程序无法处理复杂的 SQL 操作，如连接、子查询和聚合
- **重复的自定义脚本**：每个新的 CSV 分析都需要编写新代码
- **工具碎片化**：用户必须在不同的数据格式之间切换多个工具
- **学习曲线**：不同的工具需要学习不同的查询语言和接口

### Desired Outcome

用户可以使用熟悉的 MySQL SQL 语法直接查询 CSV 文件，无需任何数据库设置，通过单一、统一的工具实现快速的数据探索和分析。

---

## User Stories

**作为** 数据分析师，
**我想要** 对 CSV 文件编写 SQL 查询，
**以便** 我可以快速探索和分析数据，而无需设置数据库。

**作为** 开发人员，
**我想要** 使用标准 MySQL 语法查询 CSV 文件，
**以便** 我可以利用现有的 SQL 知识和技能。

**作为** 业务用户，
**我想要** 以可读格式接收查询结果，
**以便** 我可以高效地做出数据驱动的决策。

---

## Scope

### In Scope

- 解析并执行针对 CSV 文件的标准 MySQL SELECT 语句
- 支持常见的 SQL 操作：SELECT, WHERE, ORDER BY, GROUP BY, LIMIT
- 支持聚合函数：COUNT, SUM, AVG, MIN, MAX
- 支持 CSV 文件之间的 JOIN 操作
- 带有自动类型推断的 CSV 文件加载
- 多种格式的查询结果输出（表格显示、CSV 导出、JSON 导出）
- 无效 SQL 语法和文件问题的错误处理
- 支持标准 MySQL 数据类型和表达式
- 查询中的基本算术和字符串操作

### Out of Scope

- 数据修改操作（INSERT, UPDATE, DELETE）
- 数据库创建和管理功能
- 用户身份验证和访问控制
- 查询结果的持久存储或缓存
- 高级 MySQL 功能（存储过程、触发器、视图）
- 实时流数据处理
- 与外部数据库或数据源的集成
- GUI 界面（仅命令行工具）

---

## User Scenarios & Testing

### Primary User Flow

**场景**：基本 CSV 查询
**给定** 一个名为 "sales.csv" 的 CSV 文件，包含销售数据，列包括 (id, product, quantity, price, date)
**当** 用户执行查询 `SELECT product, SUM(quantity) as total_qty FROM sales.csv GROUP BY product ORDER BY total_qty DESC`
**那么** 系统返回显示每个产品及其总销售数量的结果集，按数量降序排列

**场景**：多表连接
**给定** 两个 CSV 文件："orders.csv" (order_id, customer_id, amount) 和 "customers.csv" (customer_id, name, email)
**当** 用户执行查询 `SELECT c.name, COUNT(o.order_id) as order_count FROM orders.csv o JOIN customers.csv c ON o.customer_id = c.customer_id GROUP BY c.name`
**那么** 系统返回客户名称及其订单数量

**场景**：过滤查询
**给定** 一个名为 "employees.csv" 的 CSV 文件，包含员工数据
**当** 用户执行查询 `SELECT * FROM employees.csv WHERE department = 'Engineering' AND salary > 75000 ORDER BY salary DESC LIMIT 10`
**那么** 系统返回薪资最高的前 10 名工程部门员工

### Edge Cases

1. **空 CSV 文件**：系统处理只有标题但没有数据行的文件，返回具有正确列结构的空结果集
2. **大型 CSV 文件**：系统在合理的时间限制内处理 100,000+ 行的文件
3. **缺失或格式错误的 CSV**：当 CSV 格式无效时，系统提供清晰的错误消息
4. **CSV 中的 NULL 值**：系统正确解释空字段并处理 SQL 中的 NULL 比较
5. **列名冲突**：系统使用正确的转义处理具有保留 SQL 关键字的 CSV 列
6. **同名 CSV 文件**：系统区分不同目录中的文件或使用完整路径的文件

### Error Scenarios

1. **无效 SQL 语法**：系统提供指示语法问题的具体错误消息，带有行/列引用
2. **不存在的 CSV 文件**：系统返回清晰的错误，指示找不到文件
3. **不存在的列引用**：系统指示哪个列名无效
4. **操作中的类型不匹配**：系统为不兼容的数据类型操作提供信息性错误
5. **内存约束**：系统优雅地处理大型结果集而不会崩溃

---

## Functional Requirements

### FR-1: CSV File Loading

**Description**: 系统必须加载 CSV 文件并将其解析为可查询的数据结构。

**Acceptance Criteria**:
- [ ] 加载标准逗号分隔值的 CSV 文件
- [ ] 自动检测和处理 CSV 标题行
- [ ] 自动推断列的数据类型（数值、字符串、日期）
- [ ] 支持常见的 CSV 变体（引号字段、转义字符）
- [ ] 处理各种字符编码（UTF-8, GBK, Latin-1）
- [ ] 为大文件提供文件加载进度反馈

**Priority**: High

### FR-2: SQL Query Parsing

**Description**: 系统必须解析标准 MySQL SELECT 语句并验证语法。

**Acceptance Criteria**:
- [ ] 解析带有列规范的 SELECT 语句
- [ ] 支持带有表（CSV 文件）引用的 FROM 子句
- [ ] 解析带有比较运算符（=, !=, <, >, <=, >=）的 WHERE 子句
- [ ] 支持 WHERE 子句中的逻辑运算符（AND, OR, NOT）
- [ ] 解析带有 ASC/DESC 修饰符的 ORDER BY
- [ ] 解析带有聚合函数的 GROUP BY
- [ ] 支持 LIMIT 和 OFFSET 子句
- [ ] 解析 JOIN 操作（INNER, LEFT, RIGHT）
- [ ] 验证 SQL 语法并提供有意义的错误消息

**Priority**: High

### FR-3: Query Execution

**Description**: 系统必须针对加载的 CSV 数据执行解析后的 SQL 查询。

**Acceptance Criteria**:
- [ ] 执行 SELECT 查询返回指定列
- [ ] 正确应用 WHERE 子句过滤
- [ ] 对结果集执行 ORDER BY 排序
- [ ] 执行 GROUP BY 聚合（COUNT, SUM, AVG, MIN, MAX）
- [ ] 对结果集应用 LIMIT 和 OFFSET
- [ ] 在多个 CSV 文件之间执行 JOIN 操作
- [ ] 处理列别名（AS 关键字）
- [ ] 支持 SELECT 子句中的算术表达式
- [ ] 支持字符串函数（CONCAT, SUBSTRING, LENGTH, UPPER, LOWER）

**Priority**: High

### FR-4: Result Output

**Description**: 系统必须以用户友好的格式呈现查询结果。

**Acceptance Criteria**:
- [ ] 在控制台中以格式化表格视图显示结果
- [ ] 将结果导出为 CSV 格式
- [ ] 将结果导出为 JSON 格式
- [ ] 显示带有结果数据的列标题
- [ ] 通过分页或流式处理大型结果集
- [ ] 支持输出重定向到文件
- [ ] 显示查询执行时间和行数统计

**Priority**: Medium

### FR-5: Error Handling

**Description**: 系统必须为常见问题提供清晰、可操作的错误消息。

**Acceptance Criteria**:
- [ ] 报告带有行号和上下文的 SQL 语法错误
- [ ] 用完整文件路径指示缺失的文件
- [ ] 报告带有建议的无效列引用
- [ ] 优雅地处理文件权限错误
- [ ] 为大型数据集提供内存使用警告
- [ ] 记录带有时间戳和查询上下文的错误以进行调试

**Priority**: High

### FR-6: SQL Function Support

**Description**: 系统必须支持常见的 MySQL 函数进行数据操作。

**Acceptance Criteria**:
- [ ] 支持聚合函数：COUNT, SUM, AVG, MIN, MAX
- [ ] 支持字符串函数：CONCAT, SUBSTRING, LENGTH, UPPER, LOWER, TRIM
- [ ] 支持数值函数：ROUND, CEILING, FLOOR, ABS
- [ ] 支持日期函数：DATE_FORMAT, YEAR, MONTH, DAY
- [ ] 支持条件表达式：CASE WHEN, IFNULL, COALESCE
- [ ] 支持类型转换：CAST, CONVERT

**Priority**: Medium

---

## Non-Functional Requirements

### Performance

- 简单查询在 10,000 行 CSV 文件上 2 秒内完成
- 中等复杂查询（带 JOIN）在 50,000 行数据集上 10 秒内完成
- 复杂查询（多表 JOIN + 聚合）在 50,000 行数据集上 30 秒内完成
- 处理 500MB 文件时内存使用不超过 1GB
- 大于 10MB 的文件显示文件加载进度

### Reliability

- 系统在格式错误的输入上不会崩溃
- 尽可能返回部分结果，即使某些行包含错误
- 系统为长时间运行的操作提供清晰的反馈

### Compatibility

- 在主流操作系统上运行（Windows, macOS, Linux）
- 处理来自各种来源的 CSV 文件（Excel 导出、数据库转储、手动创建）
- 支持不同行结尾的 CSV 文件（Unix LF, Windows CRLF）
- 处理 CSV 文件中常见的各种字符编码

### Usability

- 错误消息清楚地表明问题并建议解决方案
- 查询语法遵循大多数 SQL 用户熟悉的标准 MySQL 约定
- 支持的 SQL 语法和函数有帮助文档
- 命令行界面使用直观的标志和参数

---

## Data Limits

| 限制项 | 最大值 | 说明 |
|--------|--------|------|
| 单个 CSV 文件大小 | 500MB | 超过此大小将使用流式处理 |
| 最大行数 | 1,000,000 行 | 建议上限 |
| 最大列数 | 500 列 | 超过可能影响性能 |
| 单元格最大字符数 | 65,535 字符 | 类似 Excel 限制 |
| 单次查询 JOIN 表数 | 5 个表 | 复杂度控制 |
| SQL 语句最大长度 | 64KB | 解析器限制 |

---

## File Path Syntax

在 SQL 中引用 CSV 文件支持以下格式：

```sql
-- 相对路径（相对于当前工作目录）
SELECT * FROM data.csv
SELECT * FROM ./data/sales.csv

-- 绝对路径
SELECT * FROM /home/user/data/sales.csv
SELECT * FROM C:\Users\data\sales.csv

-- 带引号的路径（路径包含空格或特殊字符时）
SELECT * FROM "data files/my data.csv"
SELECT * FROM '/home/user/my data.csv'

-- 使用别名
SELECT * FROM sales.csv AS s
SELECT * FROM long_file_name.csv AS data
```

---

## Success Criteria

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| 查询执行准确性 | 支持的 SQL 语法 100% 正确结果 | 已知输入和预期输出的自动化测试套件 |
| 支持的 SQL 语法覆盖率 | 常用 MySQL SELECT 功能的 80% | 与 MySQL 参考文档比较 |
| 用户任务完成率 | 90% 的用户在首次使用 5 分钟内成功运行查询 | 任务完成跟踪的用户测试会话 |
| 查询响应时间 | 10,000 行文件上 95% 的查询在 2 秒内完成 | 标准化测试查询的性能基准测试 |
| 错误消息清晰度 | 85% 的用户无需外部帮助即可解决错误 | 遇到错误后的用户反馈调查 |

---

## Test Coverage Requirements

| 测试类型 | 覆盖率要求 | 说明 |
|----------|------------|------|
| 单元测试 | ≥ 80% | 核心模块需 ≥ 90% |
| 集成测试 | 覆盖所有主要功能路径 | 端到端测试 |
| 性能测试 | 所有性能指标 | 基准测试套件 |
| 错误场景测试 | 所有定义的错误场景 | 边界和异常情况 |

---

## Assumptions

- 用户具备基本的 SQL 语法知识（SELECT, WHERE, JOIN 概念）
- CSV 文件遵循标准格式（逗号分隔，数据中的逗号使用引号字段）
- 用户对他们想要查询的 CSV 文件具有文件系统访问权限
- 该工具将用于数据探索和分析，而非生产数据处理
- 查询结果通常由人工审查或导出以供进一步分析
- CSV 文件包含结构化的表格数据（非非结构化文本）
- 支持交互式和批处理两种执行模式
- 输出主要显示在终端/控制台或导出到文件

---

## Dependencies

- CSV 解析库（Apache Commons CSV）
- SQL 解析库（JSqlParser）
- 用于读取 CSV 和写入输出文件的文件系统访问
- 用于用户交互的标准输入/输出能力

---

## Risks & Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| SQL 解析器可能不支持用户期望的所有 MySQL 功能 | High | Medium | 提供清晰的受支持语法和函数文档；首先实现最常用的功能 |
| 大型 CSV 文件可能导致内存问题 | High | Medium | 实现流式/分块处理；提供内存警告 |
| CSV 格式变体可能导致解析错误 | Medium | High | 支持常见的 CSV 方言；提供带有格式要求的清晰错误消息 |
| 复杂 JOIN 可能导致性能下降 | Medium | Medium | 优化查询执行；在文档中设置预期；如有需要限制 JOIN 复杂度 |

---

## Key Entities

### CSV File

**Description**: 表示包含表格数据的逗号分隔值文件，作为 SQL 查询的数据源。

**Attributes**:
- File path: String - 文件系统中 CSV 文件的完整路径
- Columns: List - 从 CSV 标题行提取的列名
- Data types: Map - 每列的推断数据类型
- Row count: Integer - 文件中的数据行总数

### Query

**Description**: 用户提供的针对 CSV 文件执行的 SQL SELECT 语句。

**Attributes**:
- SQL text: String - 用户提供的原始 SQL 语句
- Parsed statement: Object - SQL 查询的结构化表示
- Referenced tables: List - 查询中引用的 CSV 文件（表）
- Columns selected: List - SELECT 子句中指定的列

### Result Set

**Description**: 针对 CSV 文件执行 SQL 查询产生的输出数据。

**Attributes**:
- Columns: List - 结果集中的列名
- Rows: List - 查询返回的数据行
- Row count: Integer - 结果集中的行数
- Execution time: Long - 执行查询所用的时间（毫秒）

---

## Glossary

| Term | Definition |
|------|------------|
| CSV | 逗号分隔值 - 一种表格数据的文件格式，其中值由逗号分隔 |
| SQL | 结构化查询语言 - 一种用于管理和查询关系数据库的标准语言 |
| MySQL | 使用 SQL 的开源关系数据库管理系统 |
| JOIN | SQL 操作，基于相关列组合两个或多个表的行 |
| Aggregate Function | 对一组值执行计算的函数（例如 SUM, COUNT, AVG） |
| Result Set | 执行 SQL 查询返回的行的集合 |

---

## Appendix

### Supported SQL Syntax Examples

**Basic SELECT:**
```sql
SELECT * FROM employees.csv
SELECT name, salary FROM employees.csv
```

**WHERE clause:**
```sql
SELECT * FROM products.csv WHERE price > 100
SELECT * FROM orders.csv WHERE status = 'completed' AND total > 500
```

**ORDER BY:**
```sql
SELECT * FROM products.csv ORDER BY price DESC
SELECT * FROM employees.csv ORDER BY department, salary DESC
```

**GROUP BY with Aggregates:**
```sql
SELECT department, COUNT(*) as count, AVG(salary) as avg_salary
FROM employees.csv GROUP BY department
```

**JOIN:**
```sql
SELECT o.order_id, c.name, o.amount
FROM orders.csv o
JOIN customers.csv c ON o.customer_id = c.customer_id
```

**LIMIT and OFFSET:**
```sql
SELECT * FROM products.csv LIMIT 10
SELECT * FROM products.csv LIMIT 10 OFFSET 20
```