# Implementation Plan: CSV SQL Parser

**Version**: 1.1
**Created**: 2026-03-26
**Updated**: 2026-03-26
**Feature**: specs/1-csv-sql-parser/spec.md

---

## Technical Context

### Project Type
- **Type**: 命令行工具 (CLI)
- **Primary Use**: 使用 SQL 语法对 CSV 文件进行数据探索和分析
- **Execution Modes**: 交互式 REPL 和批处理/脚本执行
- **Output Formats**: 控制台表格、CSV、JSON（通过标志选择）

### Technology Stack

| Component | Technology | Rationale |
|-----------|------------|-----------|
| **Language** | Java 8 | 稳定可靠，广泛使用，跨平台，丰富的生态 |
| **Build Tool** | Maven 3.9+ | 主流构建工具，依赖管理完善，插件丰富 |
| **SQL Parser** | JSqlParser 4.6+ | 专门解析 SQL，支持 MySQL 语法，活跃维护 |
| **CSV Processing** | Apache Commons CSV 1.10+ | 成熟稳定，支持各种 CSV 格式变体 |
| **CLI Framework** | Picocli 4.7+ | 现代化 CLI 框架，支持 ANSI 颜色，自动生成帮助 |
| **JSON Processing** | Jackson 2.15+ | 高性能 JSON 库，功能全面 |
| **Table Output** | 自实现 + Jansi | 灵活控制表格格式，支持 ANSI 颜色 |
| **Testing** | JUnit 5 + Mockito | 标准测试框架 + Mock 框架 |
| **Logging** | SLF4J + Logback | 主流日志框架 |

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        CLI Entry Point                       │
│                    (Picocli + Command Router)               │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ┌──────────┐   ┌──────────┐   ┌──────────┐
        │  REPL    │   │  Batch   │   │  Help    │
        │  Mode    │   │  Mode    │   │  System  │
        └──────────┘   └──────────┘   └──────────┘
              │               │
              └───────┬───────┘
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                     SQL Query Engine                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ SQL Parser  │→ │ Query Plan  │→ │  Execution Engine   │  │
│  │(JSqlParser) │  │  Generator  │  │   (In-Memory)       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                     CSV Data Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ CSV Loader  │  │ Type        │  │  Table Registry     │  │
│  │(Commons CSV)│  │ Inference   │  │  (Memory Mgmt)      │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                     Output Formatter                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Table       │  │ CSV         │  │  JSON               │  │
│  │ (Jansi)     │  │ Export      │  │  (Jackson)          │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Module Structure

```
csv-sql-parser/
├── pom.xml                              # Maven 项目配置
├── README.md                            # 项目说明
├── LICENSE                              # 许可证
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/csvsql/parser/
│   │   │       ├── CsvSqlParserApplication.java    # 主入口
│   │   │       ├── cli/
│   │   │       │   ├── MainCommand.java            # 主命令
│   │   │       │   ├── QueryCommand.java           # query 子命令
│   │   │       │   ├── ReplCommand.java            # repl 子命令
│   │   │       │   ├── BatchCommand.java           # batch 子命令
│   │   │       │   └── ReplRunner.java             # REPL 运行器
│   │   │       ├── parser/
│   │   │       │   ├── SqlParser.java              # SQL 解析器
│   │   │       │   ├── QueryPlan.java              # 查询计划
│   │   │       │   ├── QueryPlanBuilder.java       # 查询计划构建器
│   │   │       │   └── SupportedFeatures.java      # 支持的功能列表
│   │   │       ├── engine/
│   │   │       │   ├── QueryExecutor.java          # 查询执行器
│   │   │       │   ├── SelectExecutor.java         # SELECT 执行
│   │   │       │   ├── JoinExecutor.java           # JOIN 执行
│   │   │       │   ├── WhereEvaluator.java         # WHERE 条件评估
│   │   │       │   ├── GroupByExecutor.java        # GROUP BY 执行
│   │   │       │   └── functions/
│   │   │       │       ├── AggregateFunctions.java # 聚合函数
│   │   │       │       ├── StringFunctions.java    # 字符串函数
│   │   │       │       ├── DateFunctions.java      # 日期函数
│   │   │       │       └── MathFunctions.java      # 数学函数
│   │   │       ├── data/
│   │   │       │   ├── CsvLoader.java              # CSV 加载器
│   │   │       │   ├── TypeInferer.java            # 类型推断
│   │   │       │   ├── TableRegistry.java          # 表注册中心
│   │   │       │   ├── CsvTable.java               # CSV 表数据结构
│   │   │       │   └── ColumnType.java             # 列类型枚举
│   │   │       ├── output/
│   │   │       │   ├── OutputFormatter.java        # 输出格式化接口
│   │   │       │   ├── TableFormatter.java         # 表格格式化
│   │   │       │   ├── CsvFormatter.java           # CSV 格式化
│   │   │       │   ├── JsonFormatter.java          # JSON 格式化
│   │   │       │   └── ResultSet.java              # 结果集
│   │   │       ├── error/
│   │   │       │   ├── CsvSqlException.java        # 基础异常
│   │   │       │   ├── SqlSyntaxException.java     # SQL 语法异常
│   │   │       │   ├── FileNotFoundException.java  # 文件未找到异常
│   │   │       │   ├── ColumnNotFoundException.java# 列未找到异常
│   │   │       │   ├── UnsupportedFeatureException.java # 不支持功能异常
│   │   │       │   └── ErrorFormatter.java         # 错误格式化
│   │   │       └── util/
│   │   │           ├── FileUtils.java              # 文件工具
│   │   │           ├── StringUtils.java            # 字符串工具
│   │   │           └── MemoryUtils.java            # 内存工具
│   │   └── resources/
│   │       ├── logback.xml                         # 日志配置
│   │       └── help/                               # 帮助文档
│   │           ├── sql-syntax.txt
│   │           └── examples.txt
│   │
│   └── test/
│       ├── java/
│       │   └── com/csvsql/parser/
│       │       ├── parser/
│       │       │   └── SqlParserTest.java
│       │       ├── engine/
│       │       │   ├── QueryExecutorTest.java
│       │       │   └── FunctionsTest.java
│       │       ├── data/
│       │       │   ├── CsvLoaderTest.java
│       │       │   └── TypeInfererTest.java
│       │       ├── output/
│       │       │   └── FormatterTest.java
│       │       └── integration/
│       │           └── EndToEndTest.java
│       └── resources/
│           └── test-data/                          # 测试数据
│               ├── employees.csv
│               ├── orders.csv
│               ├── customers.csv
│               └── large_file.csv
│
└── docs/                                           # 文档
    ├── user-guide.md
    ├── sql-reference.md
    └── performance-guide.md
```

---

## Implementation Phases

### Phase 1: 项目基础设施 (Foundation)

**Goal**: 建立基本项目结构和 CLI 框架

**Tasks**:
1. 创建 Maven 项目结构，配置 pom.xml
2. 实现 CLI 命令解析器 (Picocli)
3. 创建基本的 REPL 循环
4. 实现帮助系统和命令文档
5. 配置日志框架
6. 创建异常处理基础

**Deliverables**:
- 可运行的 CLI 接受命令
- REPL 具有基本命令 (help, exit, version)
- 错误消息框架
- 单元测试结构

**Entry Point Commands**:
```bash
# Query mode
java -jar csv-sql-parser.jar query "SELECT * FROM data.csv LIMIT 10"

# REPL mode
java -jar csv-sql-parser.jar repl

# Batch mode
java -jar csv-sql-parser.jar batch queries.sql --output results.csv

# With options
java -jar csv-sql-parser.jar query "SELECT name, COUNT(*) FROM sales.csv GROUP BY name" \
  --format json \
  --output results.json
```

---

### Phase 2: CSV 加载和类型推断

**Goal**: 启用带有自动类型检测的 CSV 文件加载

**Tasks**:
1. 使用 Apache Commons CSV 实现 CSV 加载器
2. 构建自动类型推断系统 (int, long, double, String, LocalDate)
3. 处理 CSV 变体 (引号字段、不同分隔符)
4. 实现大文件的内存高效加载
5. 创建表注册中心跟踪加载的文件
6. 支持加载多个 CSV 文件用于 JOIN

**Deliverables**:
- 带有自动类型检测的 CSV 加载
- 使用分块加载支持大文件 (>100MB)
- 用于多文件查询的表注册中心

**Type Inference Rules**:
```
1. 尝试解析为 Integer
2. 如果失败，尝试解析为 Long
3. 如果失败，尝试解析为 Double
4. 如果失败，尝试解析为 LocalDate (常见格式)
5. 如果全部失败，视为 String
```

---

### Phase 3: SQL 解析器实现

**Goal**: 解析和验证 MySQL SELECT 语句

**Tasks**:
1. 集成 JSqlParser 进行 SQL 解析
2. 构建自定义验证器检查支持的语法
3. 实现查询计划生成器
4. 创建带有行列信息的语法错误报告器
5. 记录支持与不支持的 SQL 功能

**Supported SQL Features**:
- SELECT 带列列表、* 和别名
- FROM 带表 (CSV 文件) 引用
- WHERE 带比较运算符 (=, !=, <, >, <=, >=)
- WHERE 带逻辑运算符 (AND, OR, NOT)
- ORDER BY 带 ASC/DESC
- GROUP BY 带 HAVING
- LIMIT 和 OFFSET
- JOIN (INNER, LEFT, RIGHT) 带 ON 子句
- 列和表别名 (AS 关键字)

**Unsupported Features** (清晰错误消息):
- INSERT, UPDATE, DELETE
- 子查询
- UNION
- 窗口函数
- 公用表表达式 (CTE)

---

### Phase 4: 查询执行引擎

**Goal**: 针对 CSV 数据执行解析后的 SQL 查询

**Tasks**:
1. 实现 SELECT 列投影
2. 实现 WHERE 子句过滤
3. 实现 ORDER BY 排序
4. 实现 GROUP BY 聚合
5. 实现 LIMIT/OFFSET
6. 实现 JOIN 操作
7. 实现 SQL 函数 (COUNT, SUM, AVG, MIN, MAX)
8. 实现字符串函数 (CONCAT, UPPER, LOWER 等)
9. 实现算术表达式

**Execution Strategy**:
```
1. 加载引用的 CSV 文件
2. 应用 WHERE 过滤器 (早期过滤以提高性能)
3. 如果存在则执行 JOIN
4. 应用 GROUP BY 和聚合
5. 应用 SELECT 投影
6. 应用 ORDER BY
7. 应用 LIMIT/OFFSET
8. 返回结果集
```

---

### Phase 5: 输出格式化

**Goal**: 格式化和导出查询结果

**Tasks**:
1. 使用 Jansi 实现控制台表格格式化器
2. 实现 CSV 导出器
3. 使用 Jackson 实现 JSON 导出器
4. 添加查询执行统计显示
5. 处理大型结果集 (分页/警告)
6. 支持输出重定向到文件

**Output Format Examples**:

**Table (default)**:
```
┌──────────┬───────┬─────────┐
│ name     │ count │ total   │
├──────────┼───────┼─────────┤
│ ProductA │   150 │ 4500.00 │
│ ProductB │    89 │ 2670.00 │
└──────────┴───────┴─────────┘
(2 rows, 23ms)
```

**CSV**:
```
name,count,total
ProductA,150,4500.00
ProductB,89,2670.00
```

**JSON**:
```json
{
  "columns": ["name", "count", "total"],
  "rows": [
    {"name": "ProductA", "count": 150, "total": 4500.00},
    {"name": "ProductB", "count": 89, "total": 2670.00}
  ],
  "rowCount": 2,
  "executionTimeMs": 23
}
```

---

### Phase 6: 错误处理和用户体验

**Goal**: 提供优秀的错误消息和用户指导

**Tasks**:
1. 实现详细的 SQL 语法错误报告
2. 创建带有建议的文件未找到处理
3. 实现带有建议的列未找到处理
4. 为大文件添加内存使用警告
5. 创建全面的帮助文档
6. 在帮助系统中添加查询示例

**Error Message Examples**:

**SQL Syntax Error**:
```
Error: SQL syntax error at line 1, column 25

  SELECT name, COUNT(*) FROM data.csv WHER id = 1
                          ^^^^^

Unexpected token 'WHER'. Did you mean 'WHERE'?
```

**Column Not Found**:
```
Error: Column 'nam' not found in table 'employees.csv'

Available columns:
  - id
  - name
  - department
  - salary

Did you mean 'name'?
```

---

### Phase 7: 性能优化

**Goal**: 确保性能满足需求

**Tasks**:
1. 实现 CSV 文件的延迟加载
2. 添加查询结果缓存 (可选)
3. 使用 HashMap 索引优化 JOIN 操作
4. 为大型结果集实现流式处理
5. 添加内存分析和限制
6. 性能基准测试套件

**Performance Targets**:
- 10,000 行上的简单查询: < 2 秒
- 100,000 行上的复杂查询: < 5 秒
- 50,000 行各表的 JOIN 操作: < 30 秒

**Memory Management**:
```java
// 使用流式处理大文件
try (CSVParser parser = CSVParser.parse(file, charset, format)) {
    for (CSVRecord record : parser) {
        // 逐行处理，避免全部加载到内存
    }
}

// JOIN 优化: 使用 HashMap 建立索引
Map<Object, List<Row>> buildIndex(List<Row> rows, String column) {
    Map<Object, List<Row>> index = new HashMap<>();
    for (Row row : rows) {
        Object key = row.get(column);
        index.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
    }
    return index;
}
```

---

### Phase 8: 测试和文档

**Goal**: 全面的测试覆盖和文档

**Tasks**:
1. 所有模块的单元测试
2. SQL 查询的集成测试
3. 性能基准测试
4. 用户文档
5. API 文档 (Javadoc)
6. 示例查询和用例

**Test Categories**:
- Parser tests: SQL 语法验证
- Engine tests: 查询执行正确性
- Data tests: CSV 加载和类型推断
- Output tests: 格式化正确性
- Integration tests: 端到端查询场景
- Performance tests: 基准测试套件

**Test Coverage Requirements**:
- 单元测试覆盖率: ≥ 80%
- 核心模块覆盖率: ≥ 90%
- 所有性能指标必须有对应测试

---

## CLI Command Reference

### Query Mode
```bash
csv-sql query <sql> [options]

Arguments:
  sql                   要执行的 SQL 查询

Options:
  --format, -f          输出格式: table, csv, json (默认: table)
  --output, -o          输出文件路径 (默认: stdout)
  --delimiter, -d       CSV 分隔符 (默认: ,)
  --encoding, -e        文件编码 (默认: UTF-8)
  --no-header           CSV 文件没有标题行
  --verbose, -v         显示执行详情

Examples:
  csv-sql query "SELECT * FROM data.csv LIMIT 10"
  csv-sql query "SELECT name, COUNT(*) FROM sales.csv GROUP BY name" -f json
  csv-sql query "SELECT * FROM large.csv WHERE id > 1000" -o results.csv
```

### REPL Mode
```bash
csv-sql repl [options]

Options:
  --history, -H         历史文件路径 (默认: ~/.csv_sql_history)
  --prompt              自定义提示符 (默认: csv-sql>)

REPL Commands:
  .help                 显示帮助
  .tables               列出已加载的表
  .schema <table>       显示表结构
  .load <file> [alias]  加载 CSV 文件并可选别名
  .format <type>        设置输出格式
  .output <file>        重定向输出到文件
  .quit, .exit          退出 REPL

Examples:
  csv-sql repl
  csv-sql> SELECT * FROM data.csv LIMIT 5
  csv-sql> .format json
  csv-sql> SELECT COUNT(*) FROM data.csv
  csv-sql> .quit
```

### Batch Mode
```bash
csv-sql batch <file> [options]

Arguments:
  file                  SQL 文件 (每行一个查询或用 ; 分隔)

Options:
  --format, -f          所有查询的输出格式
  --output-dir, -o      输出文件目录
  --stop-on-error       遇到第一个错误时停止
  --verbose, -v         显示进度

Examples:
  csv-sql batch queries.sql
  csv-sql batch analysis.sql --format csv --output-dir ./results/
```

---

## Dependencies (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.csvsql</groupId>
    <artifactId>csv-sql-parser</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>CSV SQL Parser</name>
    <description>A tool to query CSV files using MySQL SQL syntax</description>

    <properties>
        <java.version>1.8</java.version>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Dependency versions -->
        <jsqlparser.version>4.6</jsqlparser.version>
        <commons-csv.version>1.10.0</commons-csv.version>
        <picocli.version>4.7.5</picocli.version>
        <jackson.version>2.15.2</jackson.version>
        <jansi.version>2.4.0</jansi.version>
        <junit.version>5.10.0</junit.version>
        <mockito.version>5.5.0</mockito.version>
    </properties>

    <dependencies>
        <!-- SQL Parser -->
        <dependency>
            <groupId>com.github.jsqlparser</groupId>
            <artifactId>jsqlparser</artifactId>
            <version>${jsqlparser.version}</version>
        </dependency>

        <!-- CSV Processing -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-csv</artifactId>
            <version>${commons-csv.version}</version>
        </dependency>

        <!-- CLI Framework -->
        <dependency>
            <groupId>info.picocli</groupId>
            <artifactId>picocli</artifactId>
            <version>${picocli.version}</version>
        </dependency>

        <!-- JSON Processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>

        <!-- ANSI Colors -->
        <dependency>
            <groupId>org.fusesource.jansi</groupId>
            <artifactId>jansi</artifactId>
            <version>${jansi.version}</version>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.9</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.4.11</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>

            <!-- JAR with dependencies -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.csvsql.parser.CsvSqlParserApplication</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <!-- Test Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
            </plugin>

            <!-- Coverage Plugin -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.10</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| SQL 解析器可能无法处理所有 MySQL 语法 | 清晰记录支持的功能；为不支持的功能提供有用的错误提示 |
| 大文件可能导致内存问题 | 实现分块加载；添加内存限制；警告用户 |
| CSV 格式变体 | 支持常见方言；在错误中提供清晰的格式要求 |
| 复杂 JOIN 性能下降 | 使用 HashMap 索引优化；在文档中设置预期；如有需要限制 JOIN 复杂度 |

---

## Success Metrics

| Metric | Target | Verification |
|--------|--------|--------------|
| SQL 执行准确性 | 支持的语法 100% 正确 | 自动化测试套件 |
| 性能 | 10K 行上 95% 的查询 < 2s | 基准测试 |
| 错误清晰度 | 85% 的错误可自行解决 | 用户测试 |
| 文档覆盖率 | 100% 的命令有文档 | 审查清单 |
| 测试覆盖率 | ≥ 80% 代码覆盖率 | JaCoCo 报告 |

---

## Milestones

| Milestone | Phase | Description | Target |
|-----------|-------|-------------|--------|
| M1: MVP | Phase 1-3 | CLI 框架 + CSV 加载 + SQL 解析 | 第 2 周 |
| M2: Core | Phase 4 | 完整的查询执行引擎 | 第 4 周 |
| M3: Feature Complete | Phase 5-6 | 输出格式化 + 错误处理 | 第 6 周 |
| M4: Release Ready | Phase 7-8 | 性能优化 + 测试 + 文档 | 第 8 周 |

---

## Next Steps

1. **设置项目结构**: 初始化 Maven 项目
2. **实现 Phase 1**: CLI 框架和 REPL 基础
3. **迭代各个阶段**: 通过测试增量构建
4. **持续集成**: 设置 CI/CD 进行自动化测试

---

## Appendix: Example SQL Queries

```sql
-- Basic SELECT
SELECT * FROM employees.csv LIMIT 10;

-- Filtered query
SELECT name, salary FROM employees.csv
WHERE department = 'Engineering' AND salary > 75000
ORDER BY salary DESC;

-- Aggregation
SELECT department, COUNT(*) as count, AVG(salary) as avg_salary
FROM employees.csv
GROUP BY department
HAVING count > 5
ORDER BY avg_salary DESC;

-- JOIN
SELECT o.order_id, c.name, o.total
FROM orders.csv o
JOIN customers.csv c ON o.customer_id = c.customer_id
WHERE o.total > 1000
ORDER BY o.total DESC
LIMIT 20;

-- String functions
SELECT UPPER(name), LENGTH(description)
FROM products.csv
WHERE category = 'Electronics';

-- Date functions
SELECT YEAR(order_date) as year, MONTH(order_date) as month, SUM(total)
FROM orders.csv
GROUP BY YEAR(order_date), MONTH(order_date)
ORDER BY year DESC, month DESC;
```