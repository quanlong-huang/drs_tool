# CSV SQL Parser 架构设计文档

**版本**: 1.0.0
**日期**: 2026-03-27
**作者**: CSV SQL Parser 开发团队

---

## 目录

1. [概述](#1-概述)
2. [系统架构](#2-系统架构)
3. [模块设计](#3-模块设计)
4. [数据流](#4-数据流)
5. [核心组件](#5-核心组件)
6. [技术选型](#6-技术选型)
7. [性能设计](#7-性能设计)
8. [错误处理](#8-错误处理)
9. [扩展性设计](#9-扩展性设计)
10. [部署架构](#10-部署架构)

---

## 1. 概述

### 1.1 项目背景

CSV SQL Parser 是一个命令行工具，允许用户使用标准 MySQL SQL 语法对 CSV 文件进行查询和分析。该工具解决了数据分析师和开发人员在处理 CSV 文件时缺乏 SQL 查询能力的问题。

### 1.2 设计目标

- **易用性**: 提供直观的 SQL 接口，降低学习成本
- **兼容性**: 支持 MySQL 标准 SQL 语法
- **高性能**: 高效处理大型 CSV 文件
- **可扩展**: 模块化设计，便于功能扩展
- **健壮性**: 完善的错误处理和用户引导

### 1.3 系统边界

```
┌─────────────────────────────────────────────────────────────┐
│                     CSV SQL Parser                          │
│                                                             │
│  输入: SQL 查询语句 + CSV 文件                               │
│  输出: 格式化查询结果 (Table/CSV/JSON)                       │
│                                                             │
│  不支持: 数据修改(INSERT/UPDATE/DELETE)、数据库持久化        │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              CLI Layer                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ │
│  │ MainCommand │  │QueryCommand │  │ ReplCommand │  │  BatchCommand   │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            Parser Layer                                  │
│  ┌─────────────┐  ┌─────────────────┐  ┌─────────────────────────────┐  │
│  │  SqlParser  │→ │ QueryPlanBuilder│→ │       QueryPlan             │  │
│  │(JSqlParser) │  │                 │  │  (执行计划中间表示)           │  │
│  └─────────────┘  └─────────────────┘  └─────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │                    SupportedFeatures (功能验证)                      ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           Engine Layer                                   │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                       QueryExecutor (协调器)                        │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐  │  │
│  │  │SelectExecutor│ │JoinExecutor │ │GroupByExecutor│ │WhereEvaluator│ │  │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘  │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │                 ExpressionEvaluator                          │  │  │
│  │  │  (CASE WHEN, COALESCE, CAST, 算术表达式, 函数调用)            │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                     Functions Library                              │  │
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────────┐  │  │
│  │  │AggregateFunctions│ │ StringFunctions │ │  DateFunctions      │  │  │
│  │  │(COUNT,SUM,AVG...)│ │(UPPER,CONCAT...)│ │(YEAR,MONTH,DAY...) │  │  │
│  │  └─────────────────┘ └─────────────────┘ └─────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │                   MathFunctions                              │  │  │
│  │  │               (ROUND, ABS, SQRT, POWER...)                   │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            Data Layer                                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ │
│  │  CsvLoader  │  │ TypeInferer │  │ CsvTable    │  │  TableRegistry  │ │
│  │(文件加载)    │  │(类型推断)    │  │(内存表结构)  │  │  (表管理)        │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────┘ │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │                     ColumnType (列类型枚举)                          ││
│  │       INTEGER | LONG | DOUBLE | DATE | BOOLEAN | STRING             ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           Output Layer                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │                    OutputFormatter (接口)                            ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────┐  ││
│  │  │TableFormatter│  │ CsvFormatter│  │      JsonFormatter          │  ││
│  │  │(ASCII表格)   │  │(CSV导出)    │  │      (JSON输出)              │  ││
│  │  └─────────────┘  └─────────────┘  └─────────────────────────────┘  ││
│  │  ┌─────────────────────────────────────────────────────────────┐    ││
│  │  │                      ResultSet                               │    ││
│  │  │         (查询结果封装: 列名、行数据、元数据)                   │    ││
│  │  └─────────────────────────────────────────────────────────────┘    ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          Support Layer                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │                         Error Handling                               ││
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌───────────────────────┐  ││
│  │  │CsvSqlException  │ │SqlSyntaxException│ │UnsupportedFeatureEx. │  ││
│  │  └─────────────────┘ └─────────────────┘ └───────────────────────┘  ││
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌───────────────────────┐  ││
│  │  │ColumnNotFoundEx.│ │FileNotFoundException│ │   ErrorFormatter    │  ││
│  │  └─────────────────┘ └─────────────────┘ └───────────────────────┘  ││
│  └─────────────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │                          Utilities                                   ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────┐  ││
│  │  │ FileUtils   │  │ StringUtils │  │      MemoryUtils            │  ││
│  │  └─────────────┘  └─────────────┘  └─────────────────────────────┘  ││
│  └─────────────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │                     Logging (SLF4J + Logback)                        ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 分层架构说明

| 层级 | 职责 | 关键类 |
|------|------|--------|
| **CLI Layer** | 命令行接口，用户交互 | MainCommand, QueryCommand, ReplCommand |
| **Parser Layer** | SQL 解析和验证 | SqlParser, QueryPlanBuilder |
| **Engine Layer** | 查询执行引擎 | QueryExecutor, SelectExecutor |
| **Data Layer** | 数据加载和管理 | CsvLoader, TableRegistry |
| **Output Layer** | 结果格式化输出 | TableFormatter, JsonFormatter |
| **Support Layer** | 异常处理和工具类 | CsvSqlException, FileUtils |

---

## 3. 模块设计

### 3.1 CLI 模块 (`com.csvsql.parser.cli`)

**职责**: 处理命令行输入，路由到相应处理器

```
┌────────────────────────────────────────────────────┐
│                   MainCommand                       │
│  (主命令，路由到子命令)                              │
└────────────────────────────────────────────────────┘
          │              │              │
          ▼              ▼              ▼
   ┌────────────┐ ┌────────────┐ ┌────────────┐
   │QueryCommand│ │ReplCommand │ │BatchCommand│
   │(单次查询)   │ │(交互模式)   │ │(批量执行)   │
   └────────────┘ └────────────┘ └────────────┘
```

**关键设计**:
- 使用 Picocli 框架实现命令解析
- 支持三种执行模式: query、repl、batch
- 配置选项通过注解绑定

### 3.2 Parser 模块 (`com.csvsql.parser.parser`)

**职责**: 解析 SQL 语句并生成执行计划

```
SQL String
    │
    ▼
┌─────────────┐
│ SqlParser   │ ← 使用 JSqlParser 解析
└─────────────┘
    │
    ▼
┌─────────────────┐
│SupportedFeatures│ ← 验证功能支持
└─────────────────┘
    │
    ▼
┌─────────────────┐
│QueryPlanBuilder │ ← 构建执行计划
└─────────────────┘
    │
    ▼
QueryPlan
```

**QueryPlan 结构**:
```java
QueryPlan {
    String originalSql;                    // 原始 SQL
    List<TableReference> tables;           // 表引用
    List<ColumnReference> columns;         // 列引用
    List<JoinClause> joins;                // JOIN 子句
    List<FilterCondition> whereConditions; // WHERE 条件
    List<FilterCondition> havingConditions;// HAVING 条件
    List<String> groupByColumns;           // GROUP BY 列
    List<SortSpecification> orderByColumns;// ORDER BY 规格
    Integer limit;                         // LIMIT
    Integer offset;                        // OFFSET
}
```

### 3.3 Engine 模块 (`com.csvsql.parser.engine`)

**职责**: 执行查询计划，协调各执行器

**QueryExecutor 执行流程**:

```
┌──────────────────────────────────────────────────────────────────────┐
│                        QueryExecutor                                  │
│                                                                       │
│  1. loadTables()          → 加载 CSV 文件到内存                       │
│  2. whereEvaluator.evaluate() → 应用 WHERE 过滤 (早期优化)            │
│  3. joinExecutor.executeJoin() → 执行 JOIN 操作                       │
│  4. groupByExecutor.execute() → 执行 GROUP BY 和聚合                  │
│  5. whereEvaluator.evaluate() → 应用 HAVING 过滤                      │
│  6. selectExecutor.project() → 投影 SELECT 列                         │
│  7. selectExecutor.orderBy() → 应用 ORDER BY 排序                     │
│  8. selectExecutor.applyLimitOffset() → 应用 LIMIT/OFFSET            │
│                                                                       │
│  return ResultSet                                                     │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.4 Data 模块 (`com.csvsql.parser.data`)

**职责**: CSV 文件加载、类型推断、表管理

**类型推断流程**:
```
CSV String Value
       │
       ▼
┌──────────────────────────────────────────────┐
│              TypeInferer.inferType()          │
│                                               │
│  尝试顺序:                                     │
│  1. INTEGER (int 范围内的整数)                 │
│  2. LONG (超 int 范围的整数)                   │
│  3. DOUBLE (小数)                              │
│  4. DATE (常见日期格式)                        │
│  5. BOOLEAN (true/false/yes/no/1/0)          │
│  6. STRING (默认)                              │
└──────────────────────────────────────────────┘
       │
       ▼
ColumnType enum
```

**CsvTable 内存结构**:
```java
CsvTable {
    String name;                    // 表名
    String filePath;                // 文件路径
    List<String> columnNames;       // 列名列表
    Map<String, ColumnType> columnTypes; // 列类型映射
    List<Map<String, Object>> rows; // 行数据
}
```

### 3.5 Output 模块 (`com.csvsql.parser.output`)

**职责**: 格式化查询结果

**输出格式策略**:
```
ResultSet
    │
    ├── TableFormatter → ASCII 表格 (默认)
    │   ┌──────────┬───────┬─────────┐
    │   │ name     │ count │ total   │
    │   ├──────────┼───────┼─────────┤
    │   │ ProductA │   150 │ 4500.00 │
    │   └──────────┴───────┴─────────┘
    │
    ├── CsvFormatter → CSV 格式
    │   name,count,total
    │   ProductA,150,4500.00
    │
    └── JsonFormatter → JSON 格式
        {
          "columns": ["name", "count", "total"],
          "rows": [{"name": "ProductA", ...}],
          "rowCount": 1,
          "executionTimeMs": 23
        }
```

---

## 4. 数据流

### 4.1 查询执行数据流

```
┌─────────┐    ┌──────────┐    ┌──────────┐    ┌───────────┐
│ 用户输入 │───→│ CLI 解析  │───→│ SQL 解析 │───→│ 执行计划   │
└─────────┘    └──────────┘    └──────────┘    └───────────┘
                                                     │
                                                     ▼
┌─────────┐    ┌──────────┐    ┌──────────┐    ┌───────────┐
│ 输出结果 │←───│ 格式化   │←───│ 结果集   │←───│ 查询执行   │
└─────────┘    └──────────┘    └──────────┘    └───────────┘
                                     ▲
                                     │
                               ┌─────┴─────┐
                               │ CSV 文件  │
                               └───────────┘
```

### 4.2 CSV 加载数据流

```
CSV 文件
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│                        CsvLoader                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ 编码检测     │→│ 分隔符检测  │→│ 分块/标准加载        │  │
│  │(UTF-8,GBK..)│  │(,,;,\t,|)   │  │(根据文件大小选择)    │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│                       TypeInferer                             │
│  采样前 N 行 → 推断每列类型 → 创建 CsvTable                    │
└──────────────────────────────────────────────────────────────┘
    │
    ▼
CsvTable (内存表)
```

### 4.3 JOIN 操作数据流

```
┌──────────────┐                    ┌──────────────┐
│  Left Table  │                    │  Right Table │
└──────────────┘                    └──────────────┘
       │                                   │
       │                          ┌────────┴────────┐
       │                          │  Build Index    │
       │                          │  (HashMap)      │
       │                          └────────┬────────┘
       │                                   │
       ▼                                   ▼
┌────────────────────────────────────────────────────────┐
│                    Nested Loop Join                     │
│  for each leftRow:                                      │
│      matches = index.get(leftRow.joinColumn)           │
│      if matches: emit combined rows                     │
│      else if LEFT JOIN: emit leftRow with NULLs        │
└────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────┐
│ Result Rows  │
└──────────────┘
```

---

## 5. 核心组件

### 5.1 QueryExecutor (查询执行器)

**设计模式**: 策略模式 + 管道模式

```java
public class QueryExecutor {
    private final SelectExecutor selectExecutor;
    private final JoinExecutor joinExecutor;
    private final WhereEvaluator whereEvaluator;
    private final GroupByExecutor groupByExecutor;

    public ResultSet execute(QueryPlan plan) {
        // 管道式执行
        List<Map<String, Object>> rows = loadTables(plan);
        rows = whereEvaluator.evaluate(rows, plan.getWhereConditions());
        rows = joinExecutor.executeJoin(rows, plan.getJoins());
        rows = groupByExecutor.execute(rows, plan);
        rows = selectExecutor.project(rows, plan, columns);
        rows = selectExecutor.orderBy(rows, plan.getOrderByColumns());
        rows = selectExecutor.applyLimitOffset(rows, limit, offset);
        return new ResultSet(columns, rows, executionTime);
    }
}
```

### 5.2 ExpressionEvaluator (表达式评估器)

**设计模式**: 访问者模式

支持的表达式类型:
- CASE WHEN 条件表达式
- COALESCE / IFNULL / NULLIF NULL 处理
- CAST 类型转换
- 算术运算 (+, -, *, /, %)
- 比较运算 (=, !=, <, >, <=, >=)
- 逻辑运算 (AND, OR, NOT)
- 函数调用

### 5.3 TableRegistry (表注册中心)

**职责**: 管理加载的表，支持延迟加载

```java
public class TableRegistry {
    private final Map<String, CsvTable> tables;      // 已加载表
    private final Map<String, String> aliases;       // 别名映射
    private final Map<String, String> pendingLoads;  // 待延迟加载

    // 按需加载
    public CsvTable getTable(String nameOrAlias) {
        if (tables.containsKey(name)) return tables.get(name);
        if (pendingLoads.containsKey(name)) {
            // 触发延迟加载
            return lazyLoad(name);
        }
        return null;
    }
}
```

---

## 6. 技术选型

### 6.1 核心依赖

| 组件 | 技术 | 版本 | 选择理由 |
|------|------|------|----------|
| **SQL 解析** | JSqlParser | 4.6 | 专为 SQL 解析设计，支持 MySQL 语法，活跃维护 |
| **CSV 处理** | Apache Commons CSV | 1.10.0 | 成熟稳定，支持各种 CSV 格式变体 |
| **CLI 框架** | Picocli | 4.7.5 | 现代化 CLI 框架，支持 ANSI 颜色，自动生成帮助 |
| **JSON 处理** | Jackson | 2.15.2 | 高性能 JSON 库，功能全面 |
| **终端颜色** | Jansi | 2.4.0 | 跨平台 ANSI 颜色支持 |
| **日志** | SLF4J + Logback | 1.7.36 / 1.2.12 | 主流日志框架，Java 8 兼容 |
| **测试** | JUnit 5 + Mockito | 5.10.0 / 5.5.0 | 现代测试框架 |

### 6.2 Java 版本选择

**选择 Java 8**:
- 广泛部署，兼容性好
- 企业环境主流版本
- 足够支持所有功能需求

---

## 7. 性能设计

### 7.1 大文件处理策略

```
文件大小判断
    │
    ├── < 50MB → 标准加载 (一次性读取)
    │
    └── ≥ 50MB → 分块加载 (流式处理)
         │
         ├── 采样阶段: 读取前 N 行推断类型
         └── 加载阶段: 分块读取，减少内存峰值
```

### 7.2 JOIN 优化

**HashMap 索引优化**:
```java
// 为右表建立 HashMap 索引
Map<Object, List<Row>> buildIndex(List<Row> rightRows, String joinColumn) {
    Map<Object, List<Row>> index = new HashMap<>();
    for (Row row : rightRows) {
        Object key = row.get(joinColumn);
        index.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
    }
    return index;
}

// 查找复杂度从 O(n*m) 降到 O(n)
```

### 7.3 性能指标

| 场景 | 目标性能 |
|------|----------|
| 简单查询 (10K 行) | < 2 秒 |
| 复杂查询 (100K 行) | < 5 秒 |
| JOIN (50K 行/表) | < 30 秒 |
| 内存占用 (100MB 文件) | < 500MB |

---

## 8. 错误处理

### 8.1 异常层次结构

```
CsvSqlException (基类)
    │
    ├── SqlSyntaxException      → SQL 语法错误
    │   └── 包含: 行号、列号、错误 token、建议
    │
    ├── UnsupportedFeatureException → 不支持的功能
    │   └── 包含: 功能名、类型、替代方案
    │
    ├── ColumnNotFoundException  → 列未找到
    │   └── 包含: 列名、可用列列表、相似列建议
    │
    └── FileNotFoundException    → 文件未找到
        └── 包含: 文件路径、建议
```

### 8.2 错误消息设计

**示例: SQL 语法错误**
```
✗ SQL Syntax Error
Unexpected token 'SELEC' at line 1, column 1

  1 | SELEC * FROM data.csv
    | ^^^^^

💡 Did you mean 'SELECT'?
```

**示例: 列未找到**
```
✗ Column Not Found
Column 'nam' does not exist

Available columns: id, name, department, salary

💡 Did you mean: name?
```

---

## 9. 扩展性设计

### 9.1 新增输出格式

实现 `OutputFormatter` 接口:

```java
public interface OutputFormatter {
    String format(ResultSet resultSet);
    String getName();
}

// 新增 XML 格式示例
public class XmlFormatter implements OutputFormatter {
    @Override
    public String format(ResultSet resultSet) {
        // XML 格式化逻辑
    }

    @Override
    public String getName() {
        return "xml";
    }
}
```

### 9.2 新增 SQL 函数

在 `ExpressionEvaluator` 中添加函数处理:

```java
private Object evaluateFunction(Function func, Map<String, Object> row) {
    String funcName = func.getName().toUpperCase();
    switch (funcName) {
        // 现有函数...
        case "MY_NEW_FUNCTION":
            return evaluateMyNewFunction(func, row);
        // ...
    }
}
```

### 9.3 新增数据类型

扩展 `ColumnType` 枚举和 `TypeInferer`:

```java
public enum ColumnType {
    INTEGER, LONG, DOUBLE, DATE, BOOLEAN, STRING,
    TIMESTAMP  // 新增类型
}
```

---

## 10. 部署架构

### 10.1 构建产物

```
target/
├── csv-sql-parser-1.0.0-SNAPSHOT.jar     # 原始 JAR
└── csv-sql-parser-1.0.0-SNAPSHOT-shaded.jar  # 可执行 JAR (含依赖)
```

### 10.2 运行方式

```bash
# 直接运行
java -jar csv-sql-parser-1.0.0-SNAPSHOT.jar query "SELECT * FROM data.csv"

# 指定内存
java -Xmx2g -jar csv-sql-parser.jar query "SELECT * FROM large.csv"

# 创建别名
alias csv-sql='java -jar /path/to/csv-sql-parser.jar'
csv-sql query "SELECT * FROM data.csv"
```

### 10.3 系统要求

| 要求 | 规格 |
|------|------|
| **Java 版本** | Java 8 或更高 |
| **内存** | 最小 256MB，推荐 1GB+ |
| **操作系统** | Windows / macOS / Linux |
| **磁盘** | 足够存放 CSV 文件 |

---

## 附录 A: 包结构图

```
com.csvsql.parser
├── CsvSqlParserApplication.java     # 应用入口
├── cli/                             # 命令行接口
│   ├── MainCommand.java
│   ├── QueryCommand.java
│   ├── ReplCommand.java
│   ├── BatchCommand.java
│   └── ReplRunner.java
├── parser/                          # SQL 解析
│   ├── SqlParser.java
│   ├── QueryPlan.java
│   ├── QueryPlanBuilder.java
│   └── SupportedFeatures.java
├── engine/                          # 查询引擎
│   ├── QueryExecutor.java
│   ├── SelectExecutor.java
│   ├── JoinExecutor.java
│   ├── WhereEvaluator.java
│   ├── GroupByExecutor.java
│   ├── ExpressionEvaluator.java
│   └── functions/
│       ├── AggregateFunctions.java
│       ├── StringFunctions.java
│       ├── DateFunctions.java
│       └── MathFunctions.java
├── data/                            # 数据层
│   ├── CsvLoader.java
│   ├── CsvTable.java
│   ├── TableRegistry.java
│   ├── TypeInferer.java
│   └── ColumnType.java
├── output/                          # 输出格式化
│   ├── OutputFormatter.java
│   ├── TableFormatter.java
│   ├── CsvFormatter.java
│   ├── JsonFormatter.java
│   └── ResultSet.java
├── error/                           # 异常处理
│   ├── CsvSqlException.java
│   ├── SqlSyntaxException.java
│   ├── UnsupportedFeatureException.java
│   ├── ColumnNotFoundException.java
│   ├── FileNotFoundException.java
│   └── ErrorFormatter.java
└── util/                            # 工具类
    ├── FileUtils.java
    ├── StringUtils.java
    └── MemoryUtils.java
```

---

## 附录 B: 时序图

### 查询执行时序

```
User          CLI           Parser        Engine         Data          Output
  │            │              │             │              │              │
  │──SQL──────→│              │             │              │              │
  │            │──parse()────→│             │              │              │
  │            │              │──QueryPlan─→│              │              │
  │            │              │             │──load()─────→│              │
  │            │              │             │              │──CsvTable──→│
  │            │              │             │──execute()──→│              │
  │            │              │             │              │──ResultSet─→│
  │            │              │             │──format()─────────────────→│
  │←───────────────────────────────────────────────────────────Result────│
  │            │              │             │              │              │
```

---

## 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|----------|
| 1.0.0 | 2026-03-27 | 初始版本 |