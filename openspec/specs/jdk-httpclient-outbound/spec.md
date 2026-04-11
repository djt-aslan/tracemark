## ADDED Requirements

### Requirement: JDK HttpClient 出口自动注入灰度标签

当使用 `java.net.http.HttpClient` 发送 HTTP 请求时，Agent SHALL 自动将当前线程的灰度标签注入到请求 Header `x-gray-tag` 中。

#### Scenario: 灰度标签存在时注入 Header
- **WHEN** GrayContext 中存在灰度标签 "gray-v1"
- **AND** 使用 HttpClient.send() 或 HttpClient.sendAsync() 发送请求
- **AND** 请求中未包含 x-gray-tag Header
- **THEN** 请求 SHALL 包含 Header `x-gray-tag: gray-v1`

#### Scenario: 灰度标签不存在时不注入
- **WHEN** GrayContext 中灰度标签为 null 或空
- **AND** 使用 HttpClient 发送请求
- **THEN** 请求 SHALL NOT 被修改

#### Scenario: Header 已存在时不覆盖
- **WHEN** GrayContext 中存在灰度标签 "gray-v1"
- **AND** 请求已包含 Header `x-gray-tag: existing-value`
- **THEN** 请求 SHALL 保持原有 Header 值 "existing-value"

#### Scenario: 配置开关关闭时不注入
- **WHEN** 配置 `gray.trace.http-client.enabled` 为 false
- **THEN** Agent SHALL NOT 拦截 HttpClient 请求

#### Scenario: 同步和异步方法均支持
- **WHEN** 灰度标签存在
- **THEN** HttpClient.send() 和 HttpClient.sendAsync() SHALL 均注入 Header

### Requirement: JDK HttpClient 日志输出

当成功注入灰度标签时，Agent SHALL 输出链路追踪日志（若日志开关开启）。

#### Scenario: 日志开关开启时输出
- **WHEN** 配置 `gray.trace.log.enabled` 为 true
- **AND** 成功注入灰度标签
- **THEN** SHALL 输出包含灰度标签、目标 URL、线程名的日志
