## Context

当前 gray-trace Agent 通过 ByteBuddy 字节码插桩实现灰度标签的全链路传递。已支持的出口渠道包括：
- RestTemplate（Spring）
- OkHttp 3.x
- Apache HttpClient 4.x / 5.x

入口渠道支持：
- javax.servlet.http.HttpServlet
- jakarta.servlet.http.HttpServlet

异步传递支持：
- ThreadPoolExecutor
- CompletableFuture
- RocketMQ Producer（注入消息属性）

**缺失能力**：
1. **JDK HttpClient**：Java 11+ 标准库的现代 HTTP 客户端，无灰度标签注入
2. **RocketMQ Consumer**：消息消费时无法从消息属性恢复灰度上下文

## Goals / Non-Goals

**Goals:**
- 实现 JDK HttpClient 出口自动注入 `x-gray-tag` Header
- 实现 RocketMQ Consumer 从消息 UserProperty 恢复 GrayContext
- 保持与现有实现风格一致（Advice + Transformer 模式）
- 支持配置开关控制

**Non-Goals:**
- 不修改 gray-trace-core 模块（GrayContext、GrayConstants 已满足需求）
- 不支持 WebSocket 或其他非 HTTP 协议
- 不处理 RocketMQ 顺序消息的特殊语义

## Decisions

### D1: JDK HttpClient 拦截点选择

**决策**：拦截 `java.net.http.HttpClient#send()` 和 `sendAsync()` 方法

**理由**：
- `send()` 是同步发送入口，`sendAsync()` 是异步发送入口
- 两个方法都接收 `HttpRequest` 参数，可以在调用前修改 Request
- 通过 `@Advice.OnMethodEnter` 修改 `HttpRequest` 参数

**替代方案**：
- 拦截 `HttpRequest.Builder`：需要追踪 Builder 实例，复杂度高
- 拦截 `HttpClient#send(HttpRequest, HttpResponse.BodyHandler)`：这是实际签名，直接拦截

**风险**：JDK HttpClient 是 Java 9+ 模块，ByteBuddy 需要处理模块系统

### D2: JDK HttpClient Header 注入方式

**决策**：通过重建 `HttpRequest` 实现 Header 注入

**理由**：
- `HttpRequest` 是不可变对象，无法直接修改
- 需要从原 Request 提取方法、URI、Body 等，重建带新 Header 的 Request
- 使用 `HttpRequest.newBuilder()` 链式构建

**实现**：
```java
@Advice.OnMethodEnter
public static void onEnter(
    @Advice.Argument(value = 0, readOnly = false) HttpRequest request) {
    String tag = GrayContext.get();
    if (tag != null && !tag.isEmpty()) {
        String existingHeader = request.headers().firstValue(GrayConstants.HEADER_GRAY_TAG).orElse(null);
        if (existingHeader == null) {
            request = HttpRequest.newBuilder(request)
                .header(GrayConstants.HEADER_GRAY_TAG, tag)
                .build();
        }
    }
}
```

### D3: RocketMQ Consumer 拦截点选择

**决策**：拦截 `org.apache.rocketmq.client.impl.consumer.PullAPIWrapper#processPullResult`

**理由**：
- 这是消息拉取后处理的统一入口
- 在此处恢复上下文，覆盖所有消费模式（Push/Pull）
- 消息对象 `MessageExt` 包含 UserProperty

**替代方案**：
- 拦截 `MessageListenerConcurrently#consumeMessage`：只覆盖并发消费，不覆盖顺序消费
- 拦截 `DefaultMQPushConsumerImpl#pullMessage`：太底层，时机不对

### D4: RocketMQ Consumer 上下文生命周期

**决策**：在消息处理前设置上下文，处理后清理

**理由**：
- 使用 `@Advice.OnMethodEnter` 设置 GrayContext
- 使用 `@Advice.OnMethodExit` 清理 GrayContext（防止线程池复用时污染）
- 与 Servlet 入口处理模式一致

**实现**：
```java
@Advice.OnMethodEnter
public static void onEnter(@Advice.Argument(...) MessageExt msg) {
    String tag = msg.getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG);
    if (tag != null && !tag.isEmpty()) {
        GrayContext.set(tag);
    } else {
        GrayContext.set(GrayConstants.TAG_STABLE);
    }
}

@Advice.OnMethodExit
public static void onExit() {
    GrayContext.clear();
}
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|-----|---------|
| JDK HttpClient 模块系统兼容性 | ByteBuddy 默认支持模块系统，测试覆盖 Java 11+ |
| HttpRequest 重建性能开销 | 仅在灰度标签存在时重建，正常流量无影响 |
| RocketMQ Consumer 拦截点变更 | RocketMQ 客户端版本锁定 4.9.x，监控 API 兼容性 |
| Consumer 上下文清理时机 | OnMethodExit 确保清理，与 Servlet 模式一致 |
| 异步消息顺序语义 | 文档说明 MQ 默认关闭的原因，用户按需开启 |
