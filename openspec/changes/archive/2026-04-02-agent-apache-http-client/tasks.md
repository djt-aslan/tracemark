## 1. HttpComponents 4.x Advice + Transformer

- [x] 1.1 在 `gray-trace-agent` 模块新建 `ApacheHttpClientOutboundAdvice.java`：实现 `@Advice.OnMethodEnter`，从方法参数中提取 `HttpRequest`（4.x），调用 `GrayContext.get()` 获取灰度标，若非 `stable` 且 Header 不存在则通过 `request.setHeader` 注入 `x-gray-tag`
- [x] 1.2 新建 `ApacheHttpClientOutboundTransformer.java`：实现 `AgentBuilder.Transformer`，使用 `ElementMatchers.named("org.apache.http.impl.client.CloseableHttpClient")` 匹配目标类，绑定 `ApacheHttpClientOutboundAdvice` 到 `execute` 方法

## 2. HttpComponents 5.x Advice + Transformer

- [x] 2.1 新建 `ApacheHttp5ClientOutboundAdvice.java`：与 1.1 逻辑相同，适配 5.x 的 `ClassicHttpRequest`（包路径 `org.apache.hc.core5.http.ClassicHttpRequest`），通过 `request.setHeader` 注入灰度标
- [x] 2.2 新建 `ApacheHttp5ClientOutboundTransformer.java`：目标类为 `org.apache.hc.client5.http.impl.classic.CloseableHttpClient`，绑定 `ApacheHttp5ClientOutboundAdvice`

## 3. GrayTraceAgent 注册

- [x] 3.1 在 `GrayTraceAgent#install()` 中读取 `config.getApacheHttpClient().isEnabled()` 条件分支
- [x] 3.2 条件为 `true` 时注册 `ApacheHttpClientOutboundTransformer`（4.x）
- [x] 3.3 条件为 `true` 时注册 `ApacheHttp5ClientOutboundTransformer`（5.x）

## 4. 验证

- [x] 4.1 在 `gray-trace-test` 模块增加一个使用 `CloseableHttpClient`（4.x）发起下游调用的测试用例，验证灰度标在 Agent 模式下正确透传
- [x] 4.2 验证 `gray.trace.apache-http-client.enabled=false` 时 Header 不被注入
- [x] 4.3 验证目标 classpath 上无 Apache HttpClient 时 Agent 启动不报错
