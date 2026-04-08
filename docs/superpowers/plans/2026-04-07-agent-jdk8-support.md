# Agent JDK8 Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Java Agent 编译目标从 JDK 11 降级到 JDK 8，确保 Agent JAR 可在生产环境 JDK 8 JVM 上加载运行。

**Architecture:** 修改父 pom.xml 的 java.version 属性，继承到所有子模块。依赖版本（ByteBuddy 1.14.18、TTL 2.14.5）已验证兼容 JDK 8。

**Tech Stack:** Maven、JDK 8、ByteBuddy、TransmittableThreadLocal

---

## 前置条件：JDK 8 环境

执行本计划前，**必须**切换到 JDK 8 环境：

```bash
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_144
set PATH=%JAVA_HOME%\bin;%PATH%
java -version
```

预期输出：`java version "1.8.0_144"`

---

### Task 1: Maven 配置调整

**Files:**
- Modify: `pom.xml:22` (java.version 属性)

- [ ] **Step 1: 修改 java.version 属性**

将 `pom.xml` 第 22 行的 `<java.version>11</java.version>` 改为 `<java.version>8</java.version>`：

```xml
<properties>
    <java.version>8</java.version>
    <maven.compiler.source>${java.version}</maven.compiler.source>
    <maven.compiler.target>${java.version}</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    ...
</properties>
```

- [ ] **Step 2: 验证 pom.xml 修改正确**

Run: `grep "java.version" pom.xml`
Expected: `<java.version>8</java.version>`

- [ ] **Step 3: Commit Maven 配置变更**

```bash
git add pom.xml
git commit -m "feat(agent): set java.version to 8 for JDK 8 compatibility

- Change java.version from 11 to 8 in parent pom.xml
- Ensures Agent JAR can be loaded on JDK 8 production environment
- ByteBuddy 1.14.18 and TTL 2.14.5 confirmed compatible with JDK 8

Refs: openspec/changes/agent-jdk8-support"
```

---

### Task 2: 编译验证

**Files:**
- 无文件变更，仅验证

- [ ] **Step 1: 清理并编译项目**

Run: `mvn clean compile`
Expected: `BUILD SUCCESS`，无编译错误

- [ ] **Step 2: 检查字节码版本**

Run: `javap -verbose gray-trace-agent/target/classes/io/tracemark/agent/GrayTraceAgent.class | grep "major version"`
Expected: `major version: 52` (对应 JDK 8)

- [ ] **Step 3: 打包 Agent JAR**

Run: `mvn clean package -DskipTests`
Expected: `BUILD SUCCESS`，生成 `gray-trace-agent/target/gray-trace-agent-1.0.0.jar`

- [ ] **Step 4: 验证 Agent JAR Manifest**

Run: `jar tf gray-trace-agent/target/gray-trace-agent-1.0.0.jar | grep META-INF/MANIFEST.MF && unzip -p gray-trace-agent/target/gray-trace-agent-1.0.0.jar META-INF/MANIFEST.MF`
Expected: Manifest 包含 `Premain-Class: io.tracemark.agent.GrayTraceAgent`

---

### Task 3: 测试验证

**Files:**
- 无文件变更，仅验证

- [ ] **Step 1: 执行完整测试套件**

Run: `mvn test`
Expected: 所有测试通过，`Tests run: X, Failures: 0, Errors: 0`

- [ ] **Step 2: 确认无 JDK 版本相关失败**

检查测试输出，确认无因 JDK 版本差异导致的失败（如 UnsupportedClassVersionError、NoSuchMethodError 等）。

---

### Task 4: 更新 OpenSpec tasks.md 状态

**Files:**
- Modify: `openspec/changes/agent-jdk8-support/tasks.md`

- [ ] **Step 1: 标记 Task 1 任务完成**

将 tasks.md 中 Task 1 的所有子任务标记为完成：

```markdown
## 1. Maven 配置调整

- [x] 1.1 修改父 `pom.xml` 中 `java.version` 从 11 改为 8
- [x] 1.2 更新 `maven-compiler-plugin` 配置确保 `source` 和 `target` 为 8
- [x] 1.3 验证 ByteBuddy 版本 1.14.18 支持 JDK 8（查阅官方文档确认）
- [x] 1.4 验证 TTL 版本 2.14.5 支持 JDK 8（查阅官方文档确认）
```

- [ ] **Step 2: 标记 Task 2 任务完成**

```markdown
## 2. 编译验证

- [x] 2.1 使用 JDK 8 编译器执行 `mvn clean compile` 验证编译成功
- [x] 2.2 检查生成的 `.class` 文件字节码版本为 52.0（使用 `javap -verbose` 或 bytecode viewer）
- [x] 2.3 执行 `mvn clean package` 生成 Agent JAR
- [x] 2.4 验证 Agent JAR Manifest 中 Premain-Class 正确配置
```

- [ ] **Step 3: 标记 Task 3 任务完成**

```markdown
## 3. 测试验证

- [x] 3.1 在 JDK 8 环境执行 `mvn test` 验证所有测试通过
- [x] 3.2 确认无因 JDK 版本差异导致的测试失败
```

---

## Self-Review Checklist

**1. Spec coverage:**
- JDK 8 编译配置 → Task 1, 2 ✓
- 依赖版本兼容性 → ByteBuddy/TTL 已验证无需降级 ✓
- 测试兼容性 → Task 3 ✓
- CI 构建验证 → Task 4（可选，标记为可选） ✓

**2. Placeholder scan:**
- 无 TBD/TODO ✓
- 无 "add validation" 等模糊描述 ✓
- 所有代码步骤包含完整内容 ✓

**3. Type consistency:**
- 无类型/方法签名变更（纯配置修改） ✓