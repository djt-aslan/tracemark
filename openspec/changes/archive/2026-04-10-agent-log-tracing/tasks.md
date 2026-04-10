## 1. 依赖与基础设施

- [x] 1.1 在 `gray-trace-agent/pom.xml` 添加 `slf4j-api` 依赖
- [x] 1.2 配置 maven-shade-plugin 将 SLF4J shade 到 `io.tracemark.shade.slf4j` 包名
- [x] 1.3 在 `GrayProperties` 添加 `log.enabled` 配置属性（默认 false）
- [x] 1.4 在 `AgentConfigLoader` 添加日志开关读取逻辑

## 2. 日志工具类

- [x] 2.1 创建 `GrayTraceLogger` 工具类，封装日志输出逻辑
- [x] 2.2 实现 `logInbound` 方法（入口日志）
- [x] 2.3 实现 `logOutbound` 方法（出口日志）
- [x] 2.4 实现 `logAsync` 方法（异步日志）
- [x] 2.5 实现 `logClear` 方法（清理日志）
- [x] 2.6 实现日志开关判断逻辑

## 3. 入口 Advice 日志

- [x] 3.1 在 `ServletInboundAdvice` 入口添加日志调用
- [x] 3.2 在 `ServletInboundAdvice` 出口添加日志调用
- [x] 3.3 在 `JakartaServletInboundAdvice` 入口添加日志调用
- [x] 3.4 在 `JakartaServletInboundAdvice` 出口添加日志调用

## 4. 出口 Advice 日志

- [x] 4.1 在 `OkHttpOutboundAdvice` 添加日志调用
- [x] 4.2 在 `RestTemplateOutboundAdvice` 添加日志调用
- [x] 4.3 在 `ApacheHttpClientOutboundAdvice` 添加日志调用
- [x] 4.4 在 `ApacheHttp5ClientOutboundAdvice` 添加日志调用

## 5. 异步 Advice 日志

- [x] 5.1 在 `ThreadPoolAdvice` 添加日志调用
- [x] 5.2 在 `CompletableFutureAsyncAdvice` 添加日志调用

## 6. MQ Advice 日志

- [x] 6.1 在 `RocketMqProducerAdvice` 添加日志调用

## 7. 测试验证

- [x] 7.1 编写 `GrayTraceLoggerTest` 单元测试
- [x] 7.2 验证日志开关关闭时不输出日志
- [x] 7.3 验证日志开关开启时输出正确格式日志
- [x] 7.4 运行完整测试套件确保无回归
