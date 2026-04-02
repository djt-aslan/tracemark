## 背景与原因

Apache HttpClient（HttpComponents 4.x / 5.x）是企业级 Spring Boot 项目中广泛使用的底层 HTTP 传输组件，常见于 Spring Security OAuth、Elasticsearch RestHighLevelClient 以及自定义 `CloseableHttpClient` Bean 等场景。目前 tracemark 已支持 RestTemplate、OkHttp、JDK HttpClient、Feign 的出口透传，但对 Apache HttpClient 尚无覆盖。当灰度标签请求到达某服务后，若该服务通过 Apache HttpClient 发起下游调用，`x-gray-tag` 请求头会被静默丢弃，破坏全链路染色的完整性。

## 变更内容

- 新增 `GrayApacheHttpClientInterceptor`，实现 `org.apache.http.HttpRequestInterceptor`（HttpComponents 4.x），在每次出口请求中将 `GrayContext` 中的 `x-gray-tag` 注入请求头。
- 新增 `GrayApacheHttp5ClientInterceptor`，实现 `org.apache.hc.core5.http.HttpRequestInterceptor`（HttpComponents 5.x），功能相同。
- 新增 `GrayApacheHttpClientBeanPostProcessor`，自动检测 Spring 容器中的 `CloseableHttpClient` Bean 并通过 `addInterceptorLast` 注入灰度拦截器（与 `GrayOkHttpBeanPostProcessor` 模式对齐）。
- 在 `GrayAutoConfiguration` 中通过 `@ConditionalOnClass` 条件注册上述拦截器 Bean。
- 在 `GrayProperties` 中新增 `ApacheHttpClient` 嵌套配置类，暴露 `gray.trace.apache-http-client.enabled` 属性（默认 `true`）。

## 能力说明

### 新增能力

- `apache-http-client-outbound`：通过 Apache HttpComponents 4.x / 5.x `CloseableHttpClient` 发起的出口请求中，自动透传灰度标签（`x-gray-tag` 请求头）。

### 变更现有能力

<!-- 无现有规格层行为变更 -->

## 影响范围

- **新增文件**：在 `gray-trace-spring-boot-autoconfigure` 模块中新增 `GrayApacheHttpClientInterceptor.java`、`GrayApacheHttp5ClientInterceptor.java`、`GrayApacheHttpClientBeanPostProcessor.java`。
- **修改文件**：`GrayAutoConfiguration.java`（新增 Bean 注册）、`GrayProperties.java`（新增 `ApacheHttpClient` 配置类）。
- **依赖项**：`httpclient`（4.x，`optional`）和 `httpclient5`（5.x，`optional`）均已是常见传递依赖，无需新增强制依赖。
- **无破坏性变更**：现有用户不受影响；仅在 `CloseableHttpClient` 位于类路径时，拦截逻辑才会生效。
