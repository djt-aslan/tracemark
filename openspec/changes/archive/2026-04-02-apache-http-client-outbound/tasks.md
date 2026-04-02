## 1. 配置属性扩展

- [x] 1.1 在 `GrayProperties` 中新增内部类 `ApacheHttpClient`，包含 `enabled`（默认 `true`）字段及对应 getter/setter
- [x] 1.2 在 `GrayProperties` 中声明 `apacheHttpClient` 字段并初始化，添加 getter/setter

## 2. Apache HttpClient 4.x 拦截器

- [x] 2.1 在 `gray-trace-spring-boot-autoconfigure` 的 `http` 包中新建 `GrayApacheHttpClientInterceptor.java`，实现 `org.apache.http.HttpRequestInterceptor`
- [x] 2.2 在 `process` 方法中读取 `GrayContext.get()`，若非空则通过 `request.addHeader(GrayConstants.HEADER_GRAY_TAG, tag)` 注入请求头
- [x] 2.3 新建 `GrayApacheHttpClientBeanPostProcessor.java`，实现 `BeanPostProcessor`
- [x] 2.4 在 `postProcessAfterInitialization` 中检测 `org.apache.http.impl.client.CloseableHttpClient` 实例，检查 properties 开关
- [x] 2.5 检测拦截器列表中是否已含 `GrayApacheHttpClientInterceptor`，若未包含则通过 `HttpClientBuilder.create()` / `copy()` 注入并重建客户端

## 3. Apache HttpClient 5.x 拦截器

- [x] 3.1 在 `http` 包中新建 `GrayApacheHttp5ClientInterceptor.java`，实现 `org.apache.hc.core5.http.HttpRequestInterceptor`
- [x] 3.2 在 `process` 方法中读取 `GrayContext.get()`，若非空则注入 `x-gray-tag` 请求头
- [x] 3.3 新建 `GrayApacheHttp5ClientBeanPostProcessor.java`，实现 `BeanPostProcessor`
- [x] 3.4 在 `postProcessAfterInitialization` 中检测 `org.apache.hc.client5.http.impl.classic.CloseableHttpClient` 实例，检查 properties 开关
- [x] 3.5 检测拦截器列表中是否已含 `GrayApacheHttp5ClientInterceptor`，若未包含则通过 HttpClient5 Builder 注入并重建客户端

## 4. 自动配置注册

- [x] 4.1 在 `GrayAutoConfiguration` 中新增 `grayApacheHttpClientBeanPostProcessor` Bean，添加 `@ConditionalOnClass(name = "org.apache.http.impl.client.CloseableHttpClient")` 及 `@ConditionalOnProperty(prefix = "gray.trace.apache-http-client", name = "enabled", matchIfMissing = true)`
- [x] 4.2 在 `GrayAutoConfiguration` 中新增 `grayApacheHttp5ClientBeanPostProcessor` Bean，添加 `@ConditionalOnClass(name = "org.apache.hc.client5.http.impl.classic.CloseableHttpClient")` 及对应 `@ConditionalOnProperty`

## 5. 依赖声明

- [x] 5.1 在 `gray-trace-spring-boot-autoconfigure/pom.xml` 中添加 `httpclient`（4.x）`optional` 依赖
- [x] 5.2 在 `gray-trace-spring-boot-autoconfigure/pom.xml` 中添加 `httpclient5`（5.x）`optional` 依赖

## 6. 测试

- [x] 6.1 在 `gray-trace-test` 模块中新增 `GrayApacheHttpClientInterceptorTest`，验证有灰度标签时请求头被正确注入
- [x] 6.2 新增测试用例：灰度标签为空时不注入请求头
- [x] 6.3 新增测试用例：`enabled=false` 时 BeanPostProcessor 不注入拦截器
- [x] 6.4 新增测试用例：拦截器不重复注入（幂等性）
- [x] 6.5 新增 `GrayApacheHttp5ClientInterceptorTest`，对 5.x 重复上述测试场景
