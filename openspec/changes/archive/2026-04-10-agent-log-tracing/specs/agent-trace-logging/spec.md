## ADDED Requirements

### Requirement: Agent 链路追踪日志开关

Agent 模式 SHALL 提供 `gray.trace.log.enabled` 配置项控制日志输出，默认值为 `false`。

#### Scenario: 日志开关默认关闭
- **WHEN** 未配置 `gray.trace.log.enabled`
- **THEN** Agent 不输出链路追踪日志

#### Scenario: 日志开关开启
- **WHEN** 配置 `gray.trace.log.enabled=true`
- **THEN** Agent 输出 DEBUG 级别的链路追踪日志

### Requirement: 入口日志输出

Servlet 入口 Advice SHALL 在提取 Header 和清理上下文时输出日志。

#### Scenario: 入口提取 Header 日志
- **WHEN** 请求携带 `x-gray-tag` Header 且日志开关开启
- **THEN** 输出 DEBUG 日志，包含 tag 值、请求 URI、线程名

#### Scenario: 入口清理上下文日志
- **WHEN** 请求处理完成且日志开关开启
- **THEN** 输出 DEBUG 日志，包含 tag 值、线程名

### Requirement: 出口日志输出

HTTP 出口 Advice SHALL 在注入 Header 时输出日志。

#### Scenario: OkHttp 出口注入 Header 日志
- **WHEN** OkHttp 请求发起且日志开关开启
- **THEN** 输出 DEBUG 日志，包含 tag 值、请求 URL、线程名

#### Scenario: RestTemplate 出口注入 Header 日志
- **WHEN** RestTemplate 请求发起且日志开关开启
- **THEN** 输出 DEBUG 日志，包含 tag 值、请求 URL、线程名

#### Scenario: Apache HttpClient 出口注入 Header 日志
- **WHEN** Apache HttpClient 请求发起且日志开关开启
- **THEN** 输出 DEBUG 日志，包含 tag 值、请求 URL、线程名

### Requirement: 异步传递日志输出

异步传递 Advice SHALL 在 TTL 包装时输出日志。

#### Scenario: 线程池 TTL 包装日志
- **WHEN** 任务提交到线程池且日志开关开启
- **THEN** 输出 DEBUG 日志，包含 tag 值、线程池名、任务类型、线程名

#### Scenario: CompletableFuture TTL 包装日志
- **WHEN** CompletableFuture 异步方法调用且日志开关开启
- **THEN** 输出 DEBUG 日志，包含 tag 值、任务类型、线程名

### Requirement: MQ 传递日志输出

RocketMQ Producer Advice SHALL 在注入消息属性时输出日志。

#### Scenario: RocketMQ 生产者注入消息属性日志
- **WHEN** RocketMQ 消息发送且日志开关开启
- **THEN** 输出 DEBUG 日志，包含 tag 值、消息 Topic、线程名

### Requirement: 日志格式统一

所有链路追踪日志 SHALL 使用统一格式。

#### Scenario: 日志格式包含必要信息
- **WHEN** 输出链路追踪日志
- **THEN** 日志格式为 `[GrayTrace] {操作类型} → {描述}, tag={tag}, {上下文信息}`

#### Scenario: 日志级别为 DEBUG
- **WHEN** 输出链路追踪日志
- **THEN** 日志级别为 DEBUG
