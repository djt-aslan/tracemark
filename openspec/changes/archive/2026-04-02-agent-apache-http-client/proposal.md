## Why

Java Agent 模式与 Starter 模式在 Apache HttpClient 出口透传上存在功能断层：Starter 已通过 `GrayApacheHttpClientBeanPostProcessor` 支持 HttpComponents 4.x/5.x 的灰度标注入，但 Agent 模块没有对应的 ByteBuddy Advice + Transformer，导致用 Agent 接入的服务在调用 Apache HttpClient 时灰度标静默丢失。

## What Changes

- 新增 `ApacheHttpClientOutboundAdvice`（HttpComponents 4.x）：在 `CloseableHttpClient#execute` 执行前拦截，从 `GrayContext` 读取灰度标并注入 `x-gray-tag` 请求头。
- 新增 `ApacheHttp5ClientOutboundAdvice`（HttpComponents 5.x）：同上，适配 `org.apache.hc.client5` 包路径。
- 新增 `ApacheHttpClientOutboundTransformer` 和 `ApacheHttp5ClientOutboundTransformer`：ByteBuddy Transformer，将对应 Advice 绑定到目标类。
- 修改 `GrayTraceAgent`：在 `install()` 中按配置条件注册两个新 Transformer，与 Starter 的 `gray.trace.apache-http-client.enabled` 开关保持一致。

## Capabilities

### New Capabilities

- `agent-apache-http-client-outbound`：Java Agent 模式下对 Apache HttpComponents 4.x / 5.x 出口请求的灰度标自动透传能力。

### Modified Capabilities

无需求层面变更（`apache-http-client-outbound` 规格不变，本次仅补全 Agent 侧实现）。

## Impact

- **新增文件**：`gray-trace-agent` 模块下新增 4 个 Java 文件（2 个 Advice + 2 个 Transformer）。
- **修改文件**：`GrayTraceAgent.java` 新增两段 Transformer 注册逻辑。
- **无破坏性变更**：不影响 Starter 接入方式；Agent 模式下仅当 `CloseableHttpClient` 类存在时才插桩。
- **依赖**：Agent fat-jar 已 shade ByteBuddy，无需新增外部依赖；`httpclient`/`httpclient5` 在目标应用 classpath 上按需存在。
