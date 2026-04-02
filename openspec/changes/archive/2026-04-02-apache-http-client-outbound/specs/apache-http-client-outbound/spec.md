## ADDED Requirements

### Requirement: Apache HttpClient 4.x 出口自动透传灰度标签
当 Spring 容器中存在 `org.apache.http.impl.client.CloseableHttpClient` Bean，且 `gray.trace.apache-http-client.enabled` 为 `true`（默认）时，系统 SHALL 自动在该客户端的每次出口 HTTP 请求中将当前 `GrayContext` 中的 `x-gray-tag` 值注入请求头。

#### Scenario: 灰度上下文存在时自动注入请求头
- **WHEN** 当前线程的 `GrayContext` 中存在非空的灰度标签值
- **THEN** 通过 Apache HttpClient 4.x `CloseableHttpClient` 发出的请求中 SHALL 包含值与上下文一致的 `x-gray-tag` 请求头

#### Scenario: 灰度上下文为空时不注入请求头
- **WHEN** 当前线程的 `GrayContext` 中灰度标签为 `null` 或空字符串
- **THEN** 通过 Apache HttpClient 4.x `CloseableHttpClient` 发出的请求中 SHALL NOT 包含 `x-gray-tag` 请求头

#### Scenario: 配置关闭时不注入请求头
- **WHEN** `gray.trace.apache-http-client.enabled=false`
- **THEN** Apache HttpClient 4.x BeanPostProcessor SHALL NOT 为 `CloseableHttpClient` Bean 注入拦截器，出口请求中不包含 `x-gray-tag`

#### Scenario: 拦截器不重复注入
- **WHEN** 同一个 `CloseableHttpClient` Bean 被多次经过 BeanPostProcessor
- **THEN** 系统 SHALL 检测拦截器列表中是否已存在 `GrayApacheHttpClientInterceptor`，若已存在则跳过注入，避免重复

### Requirement: Apache HttpClient 5.x 出口自动透传灰度标签
当 Spring 容器中存在 `org.apache.hc.client5.http.impl.classic.CloseableHttpClient` Bean，且 `gray.trace.apache-http-client.enabled` 为 `true`（默认）时，系统 SHALL 自动在该客户端的每次出口 HTTP 请求中将当前 `GrayContext` 中的 `x-gray-tag` 值注入请求头。

#### Scenario: 灰度上下文存在时自动注入请求头（5.x）
- **WHEN** 当前线程的 `GrayContext` 中存在非空的灰度标签值，且使用 Apache HttpComponents 5.x 客户端
- **THEN** 通过该客户端发出的请求中 SHALL 包含值与上下文一致的 `x-gray-tag` 请求头

#### Scenario: 4.x 与 5.x 共存时互不干扰
- **WHEN** Spring 容器中同时存在 4.x 和 5.x 的 `CloseableHttpClient` Bean
- **THEN** 系统 SHALL 分别为两者注入对应版本的拦截器，各自独立工作，互不影响

### Requirement: Apache HttpClient 配置属性支持
系统 SHALL 通过 `gray.trace.apache-http-client.enabled` 属性提供对 Apache HttpClient 出口透传功能的独立开关控制，默认值为 `true`。

#### Scenario: 默认启用
- **WHEN** 应用配置中未显式设置 `gray.trace.apache-http-client.enabled`
- **THEN** Apache HttpClient 出口透传功能 SHALL 默认启用

#### Scenario: 显式禁用
- **WHEN** 应用配置中设置 `gray.trace.apache-http-client.enabled=false`
- **THEN** Apache HttpClient 出口透传功能 SHALL 被禁用，相关 BeanPostProcessor 不注入拦截器

#### Scenario: 全局开关优先
- **WHEN** `gray.trace.enabled=false`（全局总开关关闭）
- **THEN** Apache HttpClient 出口透传功能 SHALL 也随之禁用，无论 `apache-http-client.enabled` 的值为何
