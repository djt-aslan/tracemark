## 背景

tracemark 的出口透传层已覆盖 RestTemplate、OkHttp、JDK HttpClient 和 OpenFeign，但 Apache HttpComponents（4.x `httpclient` / 5.x `httpclient5`）尚无支持。该库常作为 Spring Security OAuth2 client、Elasticsearch RestHighLevelClient 及自定义 `CloseableHttpClient` Bean 的底层传输，在企业项目中使用广泛。

现有模式（以 OkHttp 为例）：拦截器实现框架接口 → BeanPostProcessor 检测对应 Bean 并注入拦截器 → `GrayAutoConfiguration` 通过 `@ConditionalOnClass` 条件注册 BeanPostProcessor → `GrayProperties` 持有对应开关。本次变更遵循完全相同的模式，为 Apache HttpComponents 4.x 和 5.x 各增加一套实现。

## 目标 / 非目标

**目标：**
- 在通过 Apache HttpComponents 4.x `CloseableHttpClient` 发起的出口请求中自动透传 `x-gray-tag`
- 在通过 Apache HttpComponents 5.x `CloseableHttpClient` 发起的出口请求中自动透传 `x-gray-tag`
- 通过 `gray.trace.apache-http-client.enabled` 属性支持单独开关控制
- 与现有模块的设计风格保持一致，避免引入强制依赖

**非目标：**
- 不支持通过 `HttpClient`（非 `CloseableHttpClient`）创建的裸请求
- 不覆盖 Java Agent 模式（已有独立 Advice 机制）
- 不对 Apache HttpAsyncClient 提供支持（异步客户端，超出本次范围）

## 决策

### 决策 1：使用 `addInterceptorLast` 注入，而非包装器（Wrapper）

**结论：** 使用 `HttpClientBuilder.addInterceptorLast(interceptor)` 注入。

**原因：** Apache HttpClient 4.x 的 `CloseableHttpClient` 提供 `addInterceptorLast` API，可直接在 Builder 上注入拦截器，无需继承或代理，与 OkHttp 的 `newBuilder().addInterceptor()` 模式完全对称。5.x 同理。不需要像 JDK HttpClient 那样使用包装器（Wrapper），因为 Apache HttpClient 的 Builder 模式本身支持拦截器链。

**备选方案：** 继承 `CloseableHttpClient` 创建包装器类 —— 代码量更多，且 Apache HttpClient 的内部方法过多，维护成本高，舍弃。

### 决策 2：4.x 与 5.x 各自独立实现

**结论：** 分别创建 `GrayApacheHttpClientInterceptor`（4.x）和 `GrayApacheHttp5ClientInterceptor`（5.x）。

**原因：** 4.x 的 `HttpRequestInterceptor` 位于 `org.apache.http` 包，5.x 位于 `org.apache.hc.core5.http` 包，两者接口不兼容。分别实现可通过 `@ConditionalOnClass` 精确控制，避免将两个版本的依赖都引入编译路径。

### 决策 3：BeanPostProcessor 检测 `CloseableHttpClient` Bean

**结论：** 复用 `GrayOkHttpBeanPostProcessor` 的模式，在 `postProcessAfterInitialization` 中检测 `CloseableHttpClient` 实例，并通过 `HttpClientBuilder` 重建注入拦截器。

**备选方案：** 使用 Spring AOP 代理 —— 引入额外复杂性且不易控制拦截时机，舍弃。

### 决策 4：配置属性键命名

**结论：** 使用 `gray.trace.apache-http-client.enabled`，`GrayProperties` 新增内部类 `ApacheHttpClient`，字段名 `apacheHttpClient`。

**原因：** 与已有的 `ok-http`、`http-client`、`rest-template` 命名风格一致，均采用 kebab-case。

## 风险 / 权衡

- **`CloseableHttpClient` 不可变性**：Apache HttpClient 4.x 的 `CloseableHttpClient` 一旦构建后不能直接修改拦截器列表。BeanPostProcessor 只能对通过 Builder 方式创建的 Bean 进行重建。若用户直接 `new CloseableHttpClient(...)` 子类，无法注入。→ 文档说明此限制，建议通过 Spring Bean 方式注册客户端。
- **重复注入**：多次调用 BeanPostProcessor 可能造成拦截器重复添加。→ 在 `postProcessAfterInitialization` 中检查拦截器列表，若已包含同类实例则跳过（与 OkHttp 处理方式相同）。
- **5.x 与 4.x 共存**：同一项目中可能同时引入两个版本。→ 通过独立的 `@ConditionalOnClass` 分别注册，互不干扰。

## 迁移计划

无破坏性变更。`httpclient` / `httpclient5` 均以 `optional` 方式声明，现有用户不受影响。升级 tracemark 版本后，若类路径中存在对应依赖，拦截自动生效；如需关闭，设置 `gray.trace.apache-http-client.enabled=false` 即可。

## 待定问题

- Apache HttpClient 4.x BeanPostProcessor 重建客户端时，是否需要保留原有的连接池、超时等配置？需在实现阶段确认 Builder 重建是否能完整复制原始 Bean 的配置（当前 OkHttp 模式使用 `newBuilder()` 可复制所有配置，Apache HttpClient 需要单独验证）。
