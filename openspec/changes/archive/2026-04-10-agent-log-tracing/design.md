## Context

当前 Agent 模式仅 `GrayTraceAgent` 入口处有少量日志（启动、安装成功/失败），所有 Advice 类均无日志输出。开发者在排查灰度标传递问题时，只能手动在业务代码中添加日志验证，效率低下。

**约束：**
- Agent JAR 为 fat JAR，所有依赖需 shade 打包
- 日志不能影响业务代码，需通过开关控制
- 日志框架需兼容目标应用的日志实现（SLF4J 可桥接到 Logback/Log4j2/Jul 等）

## Goals / Non-Goals

**Goals:**
- 为 Agent 模式添加链路追踪日志，覆盖入口、出口、异步传递等关键节点
- 日志包含上下文信息：tag 值、URL、线程名、操作类型
- 支持配置开关控制日志输出，默认关闭
- 日志级别为 DEBUG，不干扰业务日志

**Non-Goals:**
- 不为 Starter 模式添加日志（Starter 模式可通过业务代码自行控制）
- 不修改 `gray-trace-core` 模块
- 不添加 TRACE 级别日志（仅 DEBUG）

## Decisions

### 决策 1：使用 SLF4J 作为日志框架

**选择：** 使用 `slf4j-api`，通过 shade 打入 Agent JAR。

**理由：**
- SLF4J 是 Java 日志门面标准，可桥接到任意日志实现
- 目标应用通常已有 SLF4J 实现（Logback/Log4j2），无需额外配置
- 比 JUL（java.util.logging）API 更友好，支持参数化日志

**替代方案：**
- JUL：无依赖，但 API 较旧，不支持参数化日志，格式不统一
- 直接使用目标应用的日志实现：无法确定目标应用使用哪种实现

### 决策 2：统一日志工具类 `GrayTraceLogger`

**选择：** 创建 `GrayTraceLogger` 工具类，封装日志输出逻辑。

**理由：**
- 统一日志格式：`[GrayTrace] {操作类型} → {描述}, tag={tag}, {上下文信息}`
- 统一开关控制：读取 `gray.trace.log.enabled` 配置
- 避免每个 Advice 类重复判断开关

**接口设计：**
```java
public final class GrayTraceLogger {
    // 入口日志
    public static void logInbound(String tag, String uri, String thread);
    // 出口日志
    public static void logOutbound(String tag, String url, String thread);
    // 异步日志
    public static void logAsync(String tag, String pool, String taskType, String thread);
    // 清理日志
    public static void logClear(String tag, String thread);
}
```

### 决策 3：配置项 `gray.trace.log.enabled`

**选择：** 新增配置项，默认 `false`。

**理由：**
- 生产环境通常不需要此类日志，默认关闭避免日志洪流
- 开发/测试环境可通过 JVM 参数开启：`-Dgray.trace.log.enabled=true`

### 决策 4：日志内容格式

**选择：** 日志包含 tag、URL/URI、线程名、操作类型。

**示例输出：**
```
DEBUG [GrayTrace] 入口 → 提取Header, tag=gray-v1, uri=/api/order, thread=http-nio-8080-exec-1
DEBUG [GrayTrace] 出口 → 注入Header, tag=gray-v1, url=http://user-service/api/user, thread=http-nio-8080-exec-1
DEBUG [GrayTrace] 异步 → TTL包装, tag=gray-v1, pool=gray-async-1, task=Runnable, thread=http-nio-8080-exec-1
DEBUG [GrayTrace] 清理 → 上下文, tag=gray-v1, thread=http-nio-8080-exec-1
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 高 QPS 时日志量大 | 默认关闭，仅在需要时开启 |
| SLF4J shade 可能与目标应用冲突 | shade 到独立包名 `io.tracemark.shade.slf4j` |
| 日志输出影响性能 | 使用 SLF4J 参数化日志，关闭时几乎无开销 |
| 目标应用无 SLF4J 实现 | SLF4J 会 fallback 到 NOP，不影响运行 |
