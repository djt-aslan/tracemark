## ADDED Requirements

### Requirement: Agent 模式下 Apache HttpClient 4.x 出口灰度标透传
Java Agent 安装后，`org.apache.http.impl.client.CloseableHttpClient` 的所有 `execute` 方法调用 SHALL 在请求发出前自动注入 `x-gray-tag` 请求头，值来自当前线程的 `GrayContext`。

#### Scenario: 当前线程携带灰度标时自动注入
- **WHEN** 当前线程 `GrayContext.get()` 返回非 `stable` 的灰度标（例如 `gray-v1`）
- **THEN** 出口 HTTP 请求的 `x-gray-tag` Header 值 SHALL 等于该灰度标

#### Scenario: 当前线程无灰度标时不注入
- **WHEN** 当前线程 `GrayContext.get()` 返回 `stable`
- **THEN** 出口请求 SHALL 不添加 `x-gray-tag` Header（或保持原有值不变）

#### Scenario: 请求已携带 x-gray-tag 时不覆盖
- **WHEN** 出口请求已包含 `x-gray-tag` Header
- **THEN** Agent SHALL 保留原有 Header 值，不覆盖

#### Scenario: 配置开关关闭时不插桩
- **WHEN** `gray.trace.apache-http-client.enabled=false`
- **THEN** Agent SHALL 跳过对 `CloseableHttpClient` 的字节码插桩

### Requirement: Agent 模式下 Apache HttpClient 5.x 出口灰度标透传
Java Agent 安装后，`org.apache.hc.client5.http.impl.classic.CloseableHttpClient` 的所有 `execute` 方法调用 SHALL 在请求发出前自动注入 `x-gray-tag` 请求头，值来自当前线程的 `GrayContext`。

#### Scenario: 5.x 客户端灰度标注入
- **WHEN** 当前线程 `GrayContext.isGray()` 为 `true`
- **THEN** 通过 5.x API 构造的出口请求 SHALL 携带正确的 `x-gray-tag` Header

#### Scenario: 5.x 配置开关关闭时不插桩
- **WHEN** `gray.trace.apache-http-client.enabled=false`
- **THEN** Agent SHALL 跳过对 5.x `CloseableHttpClient` 的字节码插桩

### Requirement: Agent 插桩失败不影响业务
若目标应用 classpath 上不存在 Apache HttpClient 相关类，Agent SHALL 静默忽略插桩错误，不抛出异常，不阻止应用启动。

#### Scenario: 依赖不存在时静默跳过
- **WHEN** `org.apache.http.impl.client.CloseableHttpClient` 或 `org.apache.hc.client5.*` 类不在 classpath 上
- **THEN** Agent 启动日志 SHALL 输出警告，应用 SHALL 正常启动
