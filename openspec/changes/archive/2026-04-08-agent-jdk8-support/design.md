## Context

当前项目使用 JDK 11 作为编译目标（`pom.xml` 中 `java.version=11`）。项目是一个 Java Agent，通过 ByteBuddy 进行字节码插桩，在生产环境以 `-javaagent` 方式启动。

**约束条件：**
- 生产环境运行 JDK 8，Agent JAR 必须使用 JDK 8 字节码版本
- Agent 依赖 ByteBuddy 和 TTL（TransmittableThreadLocal）
- 代码已验证未使用 JDK 9+ 特有语法（var、List.of、String.strip 等）

**利益相关者：**
- 开发团队：需要在本地 JDK 11 环境开发，但构建产物兼容 JDK 8
- 运维团队：需要在 JDK 8 生产环境部署 Agent

## Goals / Non-Goals

**Goals:**
- 将编译目标从 JDK 11 降级到 JDK 8
- 确保 ByteBuddy 和 TTL 依赖版本兼容 JDK 8
- 添加 CI 构建验证，确保 JDK 8 编译通过
- 运行完整测试套件验证功能不受影响

**Non-Goals:**
- 不修改任何业务逻辑代码
- 不添加新的 Agent 功能
- 不修改现有的 spec 行为定义

## Decisions

### Decision 1: 编译目标设置为 JDK 8

**选择**: 将 `pom.xml` 中的 `java.version` 从 11 改为 8

**理由**:
- Agent JAR 的字节码版本必须 ≤ 目标 JVM 版本
- JDK 8 无法加载 JDK 11 编译的类文件（UnsupportedClassVersionError）
- 开发环境可继续使用 JDK 11，通过 `-source 8 -target 8` 编译

**替代方案**:
- 使用 JDK 8 开发环境 → 可行，但开发者已习惯 JDK 11 工具链
- 使用 `--release 8` 标志 → JDK 9+ 特性，需要 JDK 9+ 编译器

### Decision 2: ByteBuddy 版本保持 1.14.18

**选择**: 不降级 ByteBuddy 版本

**理由**:
- ByteBuddy 1.14.x 支持 JDK 8+（官方文档明确声明）
- 1.14.18 是当前稳定版本，包含重要 bug 修复
- 降级会失去新特性，且无必要

### Decision 3: TTL 版本保持 2.14.5

**选择**: 不降级 TTL 版本

**理由**:
- TTL 2.14.x 支持 JDK 8+
- 当前版本稳定，无需降级

### Decision 4: Maven Compiler Plugin 配置

**选择**: 使用 `source` 和 `target` 属性而非 `--release`

**理由**:
- `--release` 是 JDK 9+ 标志
- 使用 `source`/`target` 更通用，兼容 JDK 8 编译器
- 配置已在父 pom 中统一管理

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 开发者误用 JDK 9+ API | CI 构建使用 JDK 8 编译器，编译失败会立即发现 |
| IDE 使用 JDK 11 运行测试可能掩盖问题 | CI 测试在 JDK 8 环境运行 |
| 部分 maven 插件可能需要 JDK 9+ | 验证所有插件版本兼容 JDK 8 |
