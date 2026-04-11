## Why

当前 gray-trace Agent 已支持 Servlet 入口、RestTemplate、OkHttp、Apache HttpClient 等出口的灰度标签传递，但 **JDK HttpClient（java.net.http.HttpClient）** 和 **RocketMQ Consumer** 尚未实现。

- **JDK HttpClient**：Java 11+ 引入的现代 HTTP 客户端，越来越多项目采用，缺少出口注入会导致调用下游时灰度标签丢失
- **RocketMQ Consumer**：Producer 已支持注入消息属性，但 Consumer 端未实现从消息恢复 GrayContext，导致异步消息消费时灰度链路断裂

## What Changes

### JDK HttpClient 出口注入

- 新增 `JdkHttpClientOutboundAdvice`：拦截 `HttpClient#send()` 和 `HttpClient#sendAsync()` 方法
- 新增 `JdkHttpClientOutboundTransformer`：ByteBuddy Transformer 注册
- 在 `HttpRequest.Builder` 中自动注入 `x-gray-tag` Header

### RocketMQ Consumer 上下文恢复

- 新增 `RocketMqConsumerAdvice`：拦截消息消费方法
- 新增 `RocketMqConsumerTransformer`：ByteBuddy Transformer 注册
- 从消息 `UserProperty` 中提取 `grayTag` 并设置到 `GrayContext`

### Agent 入口更新

- 在 `GrayTraceAgent` 中注册两个新 Transformer
- 支持配置开关：`gray.trace.http-client.enabled` 和 `gray.trace.mq.consumer`

## Capabilities

### New Capabilities

- `jdk-httpclient-outbound`：JDK HttpClient 出口灰度标签注入
- `rocketmq-consumer-inbound`：RocketMQ 消费者灰度上下文恢复

### Modified Capabilities

无

## Impact

**新增文件**：
- `gray-trace-agent/src/main/java/io/tracemark/agent/advice/JdkHttpClientOutboundAdvice.java`
- `gray-trace-agent/src/main/java/io/tracemark/agent/transformer/JdkHttpClientOutboundTransformer.java`
- `gray-trace-agent/src/main/java/io/tracemark/agent/advice/RocketMqConsumerAdvice.java`
- `gray-trace-agent/src/main/java/io/tracemark/agent/transformer/RocketMqConsumerTransformer.java`

**修改文件**：
- `gray-trace-agent/src/main/java/io/tracemark/agent/GrayTraceAgent.java`：注册新 Transformer

**配置项**：
- `gray.trace.http-client.enabled`：JDK HttpClient 开关（默认 true）
- `gray.trace.mq.consumer`：RocketMQ Consumer 开关（默认 true，依赖 `gray.trace.mq.enabled`）

**兼容性**：
- 无破坏性变更
- JDK HttpClient 需要 Java 11+ 运行时
