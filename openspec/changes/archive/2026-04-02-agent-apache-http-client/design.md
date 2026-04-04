## Context

当前项目提供 Starter 和 Java Agent 两种接入方式，功能上应保持对等。Starter 已在前一个变更（`apache-http-client-outbound`）中实现了 HttpComponents 4.x / 5.x 的灰度标出口透传，通过 `BeanPostProcessor` 拦截 Spring 容器管理的 `CloseableHttpClient` Bean。

Java Agent 模式下没有 Spring 容器，必须通过 ByteBuddy 字节码插桩直接拦截目标类的方法调用。Agent 目前已有 OkHttp 的 `OkHttpOutboundTransformer`，本次变更对齐 Apache HttpClient 支持，采用相同的 Advice + Transformer 模式。

## Goals / Non-Goals

**Goals:**
- 在 Agent 模式下拦截 `org.apache.http.impl.client.CloseableHttpClient#execute`（4.x），注入 `x-gray-tag` 请求头。
- 在 Agent 模式下拦截 `org.apache.hc.client5.http.impl.classic.CloseableHttpClient#execute`（5.x），注入 `x-gray-tag` 请求头。
- 与 `gray.trace.apache-http-client.enabled` 配置开关联动，保持与 Starter 侧行为一致。

**Non-Goals:**
- 不覆盖异步 Apache HttpClient（`CloseableHttpAsyncClient`）——与现有 OkHttp/RestTemplate 的范围保持一致，异步客户端单独立项。
- 不修改 Starter 侧任何已有实现。
- 不增加新的配置项，复用 `gray.trace.apache-http-client.enabled`。

## Decisions

### 决策 1：插桩目标类选择 `CloseableHttpClient` 而非接口 `HttpClient`

**选项 A**：插桩抽象基类 `CloseableHttpClient`（4.x：`org.apache.http.impl.client.CloseableHttpClient`；5.x：`org.apache.hc.client5.http.impl.classic.CloseableHttpClient`）。

**选项 B**：插桩接口 `HttpClient` 的所有实现子类（`isSubTypeOf`）。

**选择 A，理由**：
- `CloseableHttpClient` 是所有实际客户端实例的公共抽象基类，覆盖率等同于选项 B 但范围更精确，减少无关类被插桩的风险。
- 与 OkHttp Transformer 插桩 `RealCall` 的策略一致——锁定具体执行路径而非宽泛的接口层。

### 决策 2：在 `execute` 方法入口注入 Header，不修改返回值

Advice 在 `@Advice.OnMethodEnter` 阶段操作 `HttpRequest` 参数，添加 Header。相比修改返回值（`@Advice.OnMethodExit`）侵入性更低，且灰度标是请求属性，不需要感知响应。

### 决策 3：Header 已存在时不覆盖

若请求中已存在 `x-gray-tag`，保留原值，不覆盖。这与 Starter 侧各拦截器的行为一致，避免链路中游节点意外覆盖上游设定的标签。

## Risks / Trade-offs

- **[风险] `execute` 方法重载多**：`CloseableHttpClient` 有多个 `execute` 重载签名，ByteBuddy `ElementMatchers.named("execute")` 会绑定所有重载；Advice 需要从参数中安全提取 `HttpRequest`/`ClassicHttpRequest`，需做类型判断，不能假定固定参数位置。→ **缓解**：Advice 中用 `instanceof` 检查参数类型，匹配失败静默跳过，不抛异常。
- **[风险] 目标应用未引入 Apache HttpClient**：Agent 在 JVM 启动时安装，若 classpath 上没有目标类，ByteBuddy 会触发 `onError` 回调。→ **缓解**：`GrayTraceAgent` 已有全局 `onError` 警告日志，不影响启动；同时用 `ElementMatchers.named` 而非 `isSubTypeOf` 减少扫描开销。
- **[取舍] Agent 无法感知 Spring `@Bean` 生命周期**：Starter 通过 `BeanPostProcessor` 只包装 Spring 管理的 Bean，Agent 会插桩 JVM 内所有 `CloseableHttpClient` 实例，范围略宽。在实践中这是可接受的，因为灰度标注入是幂等操作。

## Migration Plan

本变更为纯新增，无迁移步骤：
- 已在用 Agent 模式的服务重启后自动生效（若 `gray.trace.apache-http-client.enabled=true`）。
- 回滚：设置 `-Dgray.trace.apache-http-client.enabled=false` 或替换为旧版 Agent jar。
