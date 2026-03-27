# Tasks: CSV SQL Parser

**Version**: 1.2
**Created**: 2026-03-26
**Updated**: 2026-03-26
**Source**: specs/1-csv-sql-parser/plan.md

---

## Overview

本文档包含 CSV SQL Parser 项目的所有实现任务。任务按用户故事组织，支持独立实现和测试。

---

## User Stories Summary

| ID | 用户故事 | 描述 | 优先级 |
|----|----------|------|--------|
| US1 | 数据分析师 | 对 CSV 文件编写 SQL 查询，快速探索和分析数据 | P1 |
| US2 | 开发人员 | 使用标准 MySQL 语法查询 CSV 文件，利用现有 SQL 知识 | P1 |
| US3 | 业务用户 | 以可读格式接收查询结果，高效做出数据驱动决策 | P2 |

---

## Phase 1: Setup (项目基础设施)

**Goal**: 建立基本项目结构、CLI 框架和核心基础设施

**Independent Test Criteria**: 项目可编译，CLI 可执行并显示帮助信息

- [x] T001 Create Maven project structure with standard layout in pom.xml
- [x] T002 Configure pom.xml with all dependencies (JSqlParser, Commons CSV, Picocli, Jackson, Jansi, SLF4J, JUnit 5, Mockito) in pom.xml
- [x] T003 Create main entry point CsvSqlParserApplication.java in src/main/java/com/csvsql/parser/
- [x] T004 [P] Implement MainCommand.java with Picocli in src/main/java/com/csvsql/parser/cli/
- [x] T005 [P] Implement QueryCommand.java with query options in src/main/java/com/csvsql/parser/cli/
- [x] T006 [P] Implement ReplCommand.java with REPL options in src/main/java/com/csvsql/parser/cli/
- [x] T007 [P] Implement BatchCommand.java with batch options in src/main/java/com/csvsql/parser/cli/
- [x] T008 Create base exception CsvSqlException.java in src/main/java/com/csvsql/parser/error/
- [x] T009 [P] Create SqlSyntaxException.java in src/main/java/com/csvsql/parser/error/
- [x] T010 [P] Create FileNotFoundException.java in src/main/java/com/csvsql/parser/error/
- [x] T011 [P] Create ColumnNotFoundException.java in src/main/java/com/csvsql/parser/error/
- [x] T012 [P] Create UnsupportedFeatureException.java in src/main/java/com/csvsql/parser/error/
- [x] T013 Configure SLF4J + Logback logging in src/main/resources/logback.xml
- [x] T014 Create test fixtures and sample data in src/test/resources/test-data/

---

## Phase 2: Foundational (核心基础)

**Goal**: 实现 CSV 加载和 SQL 解析基础，所有用户故事的依赖项

**Independent Test Criteria**: CSV 文件可加载，SQL 语句可解析为 AST

### CSV Data Layer

- [x] T015 Implement CsvLoader.java with Apache Commons CSV in src/main/java/com/csvsql/parser/data/
- [x] T016 Implement TypeInferer.java for auto type detection in src/main/java/com/csvsql/parser/data/
- [x] T017 Implement ColumnType.java enum in src/main/java/com/csvsql/parser/data/
- [x] T018 Implement CsvTable.java data structure in src/main/java/com/csvsql/parser/data/
- [x] T019 Implement TableRegistry.java for table management in src/main/java/com/csvsql/parser/data/

### SQL Parser

- [x] T020 Implement SqlParser.java with JSqlParser integration in src/main/java/com/csvsql/parser/parser/
- [x] T021 Implement QueryPlan.java data structure in src/main/java/com/csvsql/parser/parser/
- [x] T022 Implement QueryPlanBuilder.java in src/main/java/com/csvsql/parser/parser/
- [x] T023 Implement SupportedFeatures.java for syntax validation in src/main/java/com/csvsql/parser/parser/

### REPL Foundation

- [x] T024 Implement ReplRunner.java with basic REPL loop in src/main/java/com/csvsql/parser/cli/
- [x] T025 Implement built-in REPL commands (.help, .quit, .version) in ReplRunner.java

---

## Phase 3: User Story 1 - 数据分析师查询功能

**Story**: 作为数据分析师，我想要对 CSV 文件编写 SQL 查询，以便快速探索和分析数据

**Independent Test Criteria**:
- 可执行 `SELECT * FROM data.csv LIMIT 10`
- 可执行带 WHERE 子句的过滤查询
- 可执行 GROUP BY 聚合查询
- 结果正确显示在控制台

### Core Query Execution

- [x] T026 [US1] Implement QueryExecutor.java as main coordinator in src/main/java/com/csvsql/parser/engine/
- [x] T027 [US1] Implement SelectExecutor.java for column projection in src/main/java/com/csvsql/parser/engine/
- [x] T028 [US1] Implement WhereEvaluator.java for WHERE filtering in src/main/java/com/csvsql/parser/engine/
- [x] T029 [US1] Implement GROUP BY execution in GroupByExecutor.java in src/main/java/com/csvsql/parser/engine/
- [x] T030 [US1] Implement ORDER BY sorting logic in SelectExecutor.java in src/main/java/com/csvsql/parser/engine/
- [x] T031 [US1] Implement LIMIT/OFFSET handling in SelectExecutor.java in src/main/java/com/csvsql/parser/engine/

### Aggregate Functions

- [x] T032 [US1] Implement AggregateFunctions.java (COUNT, SUM, AVG, MIN, MAX) in src/main/java/com/csvsql/parser/engine/functions/

### JOIN Support

- [x] T033 [US1] Implement JoinExecutor.java for INNER JOIN in src/main/java/com/csvsql/parser/engine/
- [x] T034 [US1] Implement LEFT JOIN support in JoinExecutor.java in src/main/java/com/csvsql/parser/engine/
- [x] T035 [US1] Implement RIGHT JOIN support in JoinExecutor.java in src/main/java/com/csvsql/parser/engine/

### Basic Output

- [x] T036 [US1] Implement TableFormatter.java with Jansi for console output in src/main/java/com/csvsql/parser/output/
- [x] T037 [US1] Implement ResultSet.java data structure in src/main/java/com/csvsql/parser/output/

---

## Phase 4: User Story 2 - 开发人员高级功能

**Story**: 作为开发人员，我想要使用标准 MySQL 语法查询 CSV 文件，以便利用现有的 SQL 知识和技能

**Independent Test Criteria**:
- 字符串函数 (UPPER, LOWER, CONCAT) 正常工作
- 日期函数 (YEAR, MONTH, DAY) 正常工作
- 算术表达式在 SELECT 和 WHERE 中正常工作
- 条件表达式 (CASE WHEN, COALESCE) 正常工作
- 类型转换 (CAST) 正常工作

### String Functions

- [x] T038 [US2] Implement StringFunctions.java (UPPER, LOWER, LENGTH, CONCAT, SUBSTRING, TRIM) in src/main/java/com/csvsql/parser/engine/functions/

### Date Functions

- [x] T039 [US2] Implement DateFunctions.java (YEAR, MONTH, DAY, DATE_FORMAT) in src/main/java/com/csvsql/parser/engine/functions/

### Math Functions

- [x] T040 [US2] Implement MathFunctions.java (ROUND, CEILING, FLOOR, ABS) in src/main/java/com/csvsql/parser/engine/functions/

### Conditional Expressions

- [x] T041 [US2] Implement CASE WHEN expression support in QueryExecutor.java in src/main/java/com/csvsql/parser/engine/
- [x] T042 [US2] Implement COALESCE and IFNULL functions in QueryExecutor.java in src/main/java/com/csvsql/parser/engine/

### Type Casting

- [x] T043 [US2] Implement CAST and CONVERT support in QueryExecutor.java in src/main/java/com/csvsql/parser/engine/

### Arithmetic Expressions

- [x] T044 [US2] Implement arithmetic expressions (+, -, *, /, %) in WhereEvaluator.java in src/main/java/com/csvsql/parser/engine/

### LIKE and IN Operators

- [x] T045 [US2] Implement LIKE pattern matching in WhereEvaluator.java in src/main/java/com/csvsql/parser/engine/
- [x] T046 [US2] Implement IN operator support in WhereEvaluator.java in src/main/java/com/csvsql/parser/engine/

---

## Phase 5: User Story 3 - 业务用户输出功能

**Story**: 作为业务用户，我想要以可读格式接收查询结果，以便高效地做出数据驱动的决策

**Independent Test Criteria**:
- 结果可导出为 CSV 文件
- 结果可导出为 JSON 文件
- 表格显示包含执行统计信息
- 输出可重定向到文件

### Output Formatters

- [x] T047 [US3] Implement OutputFormatter.java interface in src/main/java/com/csvsql/parser/output/
- [x] T048 [US3] Implement CsvFormatter.java for CSV export in src/main/java/com/csvsql/parser/output/
- [x] T049 [US3] Implement JsonFormatter.java with Jackson in src/main/java/com/csvsql/parser/output/

### Execution Statistics

- [x] T050 [US3] Add query execution time and row count to ResultSet.java in src/main/java/com/csvsql/parser/output/

### Output Redirection

- [x] T051 [US3] Implement output file redirection in QueryCommand.java in src/main/java/com/csvsql/parser/cli/
- [x] T052 [US3] Implement .output REPL command in ReplRunner.java in src/main/java/com/csvsql/parser/cli/

### REPL Output Control

- [x] T053 [US3] Implement .format REPL command in ReplRunner.java in src/main/java/com/csvsql/parser/cli/
- [x] T054 [US3] Implement .tables REPL command in ReplRunner.java in src/main/java/com/csvsql/parser/cli/
- [x] T055 [US3] Implement .schema REPL command in ReplRunner.java in src/main/java/com/csvsql/parser/cli/

---

## Phase 6: Polish & Cross-Cutting (完善和横切关注点)

**Goal**: 错误处理、性能优化、测试覆盖和文档

### Error Handling

- [x] T056 Implement ErrorFormatter.java for detailed error messages in src/main/java/com/csvsql/parser/error/
- [x] T057 Add column name suggestions in ColumnNotFoundException.java in src/main/java/com/csvsql/parser/error/
- [x] T058 Add SQL syntax error reporting with line/column info in SqlSyntaxException.java in src/main/java/com/csvsql/parser/error/

### Performance Optimization

- [x] T059 Implement lazy loading in TableRegistry.java in src/main/java/com/csvsql/parser/data/
- [x] T060 Implement chunked loading for large CSV files in CsvLoader.java in src/main/java/com/csvsql/parser/data/
- [x] T061 Optimize JOIN with HashMap index in JoinExecutor.java in src/main/java/com/csvsql/parser/engine/
- [x] T062 Add memory monitoring utilities in MemoryUtils.java in src/main/java/com/csvsql/parser/util/

### Utility Classes

- [x] T063 [P] Implement FileUtils.java in src/main/java/com/csvsql/parser/util/
- [x] T064 [P] Implement StringUtils.java in src/main/java/com/csvsql/parser/util/

### CSV Variants Support

- [x] T065 Add encoding detection (UTF-8, GBK, Latin-1) in CsvLoader.java in src/main/java/com/csvsql/parser/data/
- [x] T066 Add CSV dialect detection (delimiter, quote char) in CsvLoader.java in src/main/java/com/csvsql/parser/data/
- [x] T067 Add NULL value handling in TypeInferer.java in src/main/java/com/csvsql/parser/data/

### Test Coverage

- [x] T068 Write CSV loader tests in CsvLoaderTest.java in src/test/java/com/csvsql/parser/data/
- [x] T069 Write type inference tests in TypeInfererTest.java in src/test/java/com/csvsql/parser/data/
- [x] T070 Write SQL parser tests in SqlParserTest.java in src/test/java/com/csvsql/parser/parser/
- [x] T071 Write query executor tests in QueryExecutorTest.java in src/test/java/com/csvsql/parser/engine/
- [x] T072 Write output formatter tests in FormatterTest.java in src/test/java/com/csvsql/parser/output/
- [x] T073 Write end-to-end integration tests in EndToEndTest.java in src/test/java/com/csvsql/parser/integration/

### Documentation

- [x] T074 Write user guide in docs/user-guide.md
- [x] T075 Write SQL syntax reference in docs/sql-reference.md
- [x] T076 Write performance guide in docs/performance-guide.md
- [ ] T077 Add Javadoc to all public classes and methods

---

## Dependency Graph

```
Phase 1 (Setup)
    │
    ▼
Phase 2 (Foundational)
    │
    ├──────────────────┬──────────────────┐
    ▼                  ▼                  ▼
Phase 3 (US1)    Phase 4 (US2)    Phase 5 (US3)
    │                  │                  │
    └──────────────────┴──────────────────┘
                       │
                       ▼
              Phase 6 (Polish)
```

---

## Parallel Execution Opportunities

### Phase 1 Parallel Tasks
- T004, T005, T006, T007 可并行开发（不同命令类）
- T009, T010, T011, T012 可并行开发（不同异常类）

### Phase 2 Parallel Tasks
- CSV Data Layer (T015-T019) 和 SQL Parser (T020-T023) 可并行开发

### Phase 4 Parallel Tasks
- T038 (StringFunctions), T039 (DateFunctions), T040 (MathFunctions) 可并行开发

### Phase 6 Parallel Tasks
- T063, T064 可并行开发（不同工具类）
- T068-T073 测试可并行编写

---

## Task Summary

| Phase | Tasks | Completed | Remaining |
|-------|-------|-----------|-----------|
| Phase 1: Setup | 14 | 14 | 0 |
| Phase 2: Foundational | 11 | 11 | 0 |
| Phase 3: US1 | 12 | 12 | 0 |
| Phase 4: US2 | 9 | 9 | 0 |
| Phase 5: US3 | 9 | 9 | 0 |
| Phase 6: Polish | 22 | 21 | 1 |
| **Total** | **77** | **76** | **1** |

---

## MVP Scope (最小可行产品)

**推荐 MVP 范围**: Phase 1 + Phase 2 + Phase 3 (User Story 1)

完成 MVP 后，用户可以：
- 使用 CLI 执行基本 SQL 查询
- 加载 CSV 文件并自动推断类型
- 使用 WHERE、GROUP BY、ORDER BY、LIMIT
- 执行基本 JOIN 操作
- 在控制台查看结果

**MVP Status**: ✅ COMPLETE

### 验证的功能

**Phase 3 (US1) - 数据分析师查询功能:**
- ✅ SELECT * FROM table
- ✅ SELECT columns with WHERE clause
- ✅ ORDER BY ASC/DESC
- ✅ GROUP BY with aggregations (COUNT, SUM, AVG, MIN, MAX)
- ✅ LIMIT
- ✅ JSON output format
- ✅ CSV output format
- ✅ Table output format with ANSI colors

**Phase 4 (US2) - 开发人员高级功能:**
- ✅ CASE WHEN expressions
- ✅ COALESCE / IFNULL functions
- ✅ CAST / CONVERT type casting
- ✅ Arithmetic expressions (+, -, *, /, %)
- ✅ IN / NOT IN operators
- ✅ LIKE pattern matching
- ✅ String functions (UPPER, LOWER, LENGTH, CONCAT, SUBSTRING, TRIM)
- ✅ Date functions (YEAR, MONTH, DAY)
- ✅ Math functions (ROUND, CEILING, FLOOR, ABS)
- ✅ COUNT(1), COUNT(*) support

---

## Notes

- 所有文件路径相对于项目根目录
- [P] 标记的任务可与其他 [P] 任务并行执行
- [US1/US2/US3] 标记的任务属于对应用户故事
- 测试任务在 Phase 6 集中处理，但可与对应实现任务并行开发