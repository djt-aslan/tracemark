## Why

Agent 模式缺少日志输出，排查灰度标传递问题时只能手动在业务代码中加日志验证。需要为 Agent 模式添加链路追踪日志，帮助开发者快速定位灰度标在入口、出口、异步传递等节点的传递情况，同时支持生产环境关闭以避免日志洪流。

## What Changes

- 在 `gray-trace-agent` 模块添加 SLF4J 日志依赖（shade 打包）
- 新增 `GrayTraceLogger` 工具类，统一日志输出格式和开关控制
- 为所有 Agent Advice 类添加日志输出：
  - `ServletInboundAdvice` / `JakartaServletInboundAdvice`：入口提取 Header、清理上下文
  - `OkHttpOutboundAdvice` / `RestTemplateOutboundAdvice`：出口注入 Header
  - `ApacheHttpClientOutboundAdvice` / `ApacheHttp5ClientOutboundAdvice`：出口注入 Header
  - `ThreadPoolAdvice`：线程池 TTL 包装
  - `CompletableFutureAsyncAdvice`：CompletableFuture TTL 包装
  - `RocketMqProducerAdvice`：MQ 生产者注入消息属性
- 新增配置项 `gray.trace.log.enabled`（默认 false）
- 日志级别为 DEBUG，包含 tag、URL、线程名、操作类型等上下文信息

## Capabilities

### New Capabilities

- `agent-trace-logging`: Agent 模式链路追踪日志，记录灰度标在入口、出口、异步传递等节点的传递情况

### Modified Capabilities

无修改的现有 capability。

## Impact

- **依赖变更**：`gray-trace-agent` 添加 `slf4j-api` 依赖，通过 shade 打入 fat JAR
- **代码变更**：所有 Agent Advice 类添加日志调用
- **配置变更**：新增 `gray.trace.log.enabled` 配置项
- **运行时影响**：开启日志时会产生 DEBUG 级别日志，建议仅在开发/测试环境开启