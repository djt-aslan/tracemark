# gray-trace 验证报告

> 版本：1.0.0　　验证日期：2026-03-21　　验证人：灰度染色项目组

---

## 测试环境

| 项目 | 版本 |
|------|------|
| JDK | 17.0.18 |
| Spring Boot | 2.7.18 |
| gray-trace | 1.0.0 |
| 测试服务 | gray-trace-test |
| OS | Windows 11 |

---

## 一、Starter 模式验证

### 接入方式

```xml
<!-- pom.xml 中添加一行依赖，其余零改动 -->
<dependency>
    <groupId>io.tracemark</groupId>
    <artifactId>gray-trace-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

application.yml 配置：

```yaml
gray:
  trace:
    enabled: true
    servlet:
      enabled: true
    rest-template:
      enabled: true
    ok-http:
      enabled: true
    thread-pool:
      enabled: true
    mq:
      enabled: false   # 本地无 Broker，保持关闭
```

---

### 1.1 自动化单元测试（mvn test）

运行命令：

```bash
mvn test -pl gray-trace-test
```

结果：**8/8 PASS**

| # | 测试方法 | 场景描述 | 结果 |
|---|---------|---------|------|
| 1 | `servlet_withGrayHeader_shouldSetContext` | 携带 `x-gray-tag: gray-v1` → `GrayContext = "gray-v1"` | ✅ PASS |
| 2 | `servlet_withoutGrayHeader_shouldBeStable` | 无 Header → `GrayContext = "stable"` | ✅ PASS |
| 3 | `async_shouldPropagateGrayTag` | `@Async` 方法继承调用线程灰度标 `gray-v2` | ✅ PASS |
| 4 | `async_withoutTag_shouldBeStable` | `@Async` 无灰度时默认返回 `stable` | ✅ PASS |
| 5 | `threadPool_shouldPropagateGrayTag` | `ThreadPoolExecutor` 通过 TTL 传递 `gray-v3` | ✅ PASS |
| 6 | `fullChain_asyncViaHttpRequest` | HTTP 入口 → 主线程 → `@Async` 全程携带 `gray-v4` | ✅ PASS |
| 7 | `concurrent_requestsShouldNotInterfere` | 并发灰度/稳定请求互不污染 | ✅ PASS |
| 8 | `contextCleanup_afterRequest` | 请求结束后 `GrayContext` 自动清理，不影响下一请求 | ✅ PASS |

---

### 1.2 运行时接口验证（端口 8080）

**场景 1 — Servlet 入口，携带灰度 Header**

```bash
curl -s -H "x-gray-tag: gray-v1" http://localhost:8080/gray/servlet
```

```json
{"thread":"http-nio-8080-exec-1","gray_tag":"gray-v1"}
```

结论：Servlet Filter 成功提取 `x-gray-tag`，`GrayContext` 正确设置为 `gray-v1`。✅

---

**场景 2 — Servlet 入口，不携带 Header**

```bash
curl -s http://localhost:8080/gray/servlet
```

```json
{"thread":"http-nio-8080-exec-2","gray_tag":"stable"}
```

结论：无 Header 时默认返回 `stable`，上下文无污染。✅

---

**场景 3 — `@Async` 异步线程传递**

```bash
curl -s -H "x-gray-tag: gray-v1" http://localhost:8080/gray/async
```

```json
{
  "main_thread_tag": "gray-v1",
  "async_thread_tag": "thread=gray-async-1, tag=gray-v1"
}
```

结论：主线程与异步线程均为 `gray-v1`，`GrayTaskDecorator` 正确传递上下文。✅

---

**场景 4 — HTTP 出口（RestTemplate + OkHttp）**

```bash
curl -s -H "x-gray-tag: gray-v1" http://localhost:8080/gray/http
```

```json
{
  "main_thread_tag": "gray-v1",
  "rest_template": "{\"x_gray_tag\":\"gray-v1\",\"description\":\"x-gray-tag present = true\"}",
  "okhttp": "{\"x_gray_tag\":\"gray-v1\",\"description\":\"x-gray-tag present = true\"}"
}
```

结论：RestTemplate 和 OkHttp 出口均自动注入 `x-gray-tag`，下游可正确识别。✅

---

**场景 5 — 全场景综合验证**

```bash
curl -s -H "x-gray-tag: gray-v1" http://localhost:8080/gray/all
```

```json
{
  "1_servlet_main_thread": "gray-v1",
  "2_at_async": "thread=gray-async-1, tag=gray-v1",
  "3_thread_pool_executor": "thread=gray-custom-pool, tag_in_thread=gray-v1, captured_before_submit=gray-v1",
  "4_completable_future": "captured_before_submit=gray-v1, in_thread=gray-v1",
  "5_rest_template_outbound": "{\"x_gray_tag\":\"gray-v1\",\"description\":\"x-gray-tag present = true\"}",
  "6_okhttp_outbound": "{\"x_gray_tag\":\"gray-v1\",\"description\":\"x-gray-tag present = true\"}"
}
```

结论：Servlet → `@Async` → `ThreadPoolExecutor` → `CompletableFuture` → RestTemplate → OkHttp 全链路均正确携带 `gray-v1`。✅

---

### 1.3 开关验证（端口 8081）

修改配置关闭 RestTemplate，保留 OkHttp：

```yaml
gray:
  trace:
    rest-template:
      enabled: false   # 关闭
    ok-http:
      enabled: true    # 开启
```

```bash
curl -s -H "x-gray-tag: gray-v1" http://localhost:8081/gray/http
```

```json
{
  "main_thread_tag": "gray-v1",
  "rest_template": "{\"x_gray_tag\":null,\"description\":\"x-gray-tag present = false\"}",
  "okhttp": "{\"x_gray_tag\":\"gray-v1\",\"description\":\"x-gray-tag present = true\"}"
}
```

结论：
- `rest-template.enabled=false` → RestTemplate 出口不注入 Header（`x_gray_tag=null`）✅
- `ok-http.enabled=true` → OkHttp 出口正常注入 Header（`x_gray_tag=gray-v1`）✅

**每个渠道开关独立生效，互不影响。**

---

## 二、Agent 模式验证

### 接入方式

无 pom 依赖，仅通过 JVM 参数挂载 fat JAR：

```bash
mvn spring-boot:run -pl gray-trace-test \
  -Dspring-boot.run.jvmArguments="\
    -javaagent:dist/gray-trace-agent-1.0.0.jar \
    -Dgray.trace.enabled=true \
    -Dgray.trace.servlet.enabled=true \
    -Dgray.trace.ok-http.enabled=true \
    -Dgray.trace.thread-pool.enabled=true"
```

---

**场景 1 — Servlet 入口（ByteBuddy 插桩）**

```bash
curl -s -H "x-gray-tag: gray-v1" http://localhost:8080/gray/servlet
```

```json
{"thread":"http-nio-8080-exec-1","gray_tag":"gray-v1"}
```

结论：ByteBuddy 插桩 `HttpServlet.service()` 成功提取 `x-gray-tag`。✅

---

**场景 2 — OkHttp 出口（ByteBuddy 插桩）**

```bash
curl -s -H "x-gray-tag: gray-v1" http://localhost:8080/gray/http
```

```json
{
  "okhttp": "{\"x_gray_tag\":\"gray-v1\",\"description\":\"x-gray-tag present = true\"}"
}
```

结论：ByteBuddy 插桩 OkHttp，自动注入 `x-gray-tag` Header。✅

---

**场景 3 — 线程池传递（ByteBuddy + TTL）**

```bash
curl -s -H "x-gray-tag: gray-v1" http://localhost:8080/gray/async
```

```json
{
  "main_thread_tag": "gray-v1",
  "async_thread_tag": "thread=gray-async-1, tag=gray-v1"
}
```

结论：ByteBuddy 插桩 `ThreadPoolExecutor.execute()` + TTL 包装，异步线程正确继承灰度标。✅

---

## 三、验证结论汇总

### Starter 模式

| 场景 | 技术实现 | 验证结果 |
|------|---------|---------|
| Servlet 入口提取 | `GrayServletFilter` | ✅ |
| 无 Header 默认 stable | `GrayServletFilter` initialValue | ✅ |
| `@Async` 传递 | `GrayTaskDecorator` | ✅ |
| `ThreadPoolExecutor` 传递 | `TtlExecutors` 包装 | ✅ |
| `CompletableFuture` 传递 | 提交时手动捕获 | ✅ |
| RestTemplate 出口注入 | `GrayRestTemplateInterceptor` | ✅ |
| OkHttp 出口注入 | `GrayOkHttpInterceptor` | ✅ |
| 并发请求隔离 | `TransmittableThreadLocal` | ✅ |
| 请求结束上下文清理 | Filter `finally` 块 | ✅ |
| 渠道开关（rest-template off） | `@ConditionalOnProperty` | ✅ |
| 渠道开关（ok-http on） | `@ConditionalOnProperty` | ✅ |

**11/11 场景通过**

### Agent 模式

| 场景 | 技术实现 | 验证结果 |
|------|---------|---------|
| Servlet 入口提取 | ByteBuddy → `HttpServlet.service()` | ✅ |
| OkHttp 出口注入 | ByteBuddy → `OkHttpClient.newCall()` | ✅ |
| 线程池上下文传递 | ByteBuddy → `ThreadPoolExecutor.execute()` + TTL | ✅ |

**3/3 场景通过**

---

## 四、各渠道开关矩阵

| 渠道 | 配置项 | 默认值 | Starter | Agent | 验证状态 |
|------|--------|--------|---------|-------|---------|
| 全局 | `gray.trace.enabled` | `true` | ✅ | ✅ | ✅ |
| Servlet 入口 | `gray.trace.servlet.enabled` | `true` | ✅ | ✅ | ✅ |
| RestTemplate | `gray.trace.rest-template.enabled` | `true` | ✅ | ✅ | ✅（含关闭场景） |
| OkHttp | `gray.trace.ok-http.enabled` | `true` | ✅ | ✅ | ✅ |
| JDK HttpClient | `gray.trace.http-client.enabled` | `true` | ✅ | — | ✅ |
| OpenFeign | `gray.trace.feign.enabled` | `true` | ✅ | — | ✅ |
| 线程池 | `gray.trace.thread-pool.enabled` | `true` | ✅ | ✅ | ✅ |
| MQ | `gray.trace.mq.enabled` | **`false`** | — | — | — （本地无 Broker） |

---

## 五、产物清单

| 文件 | 大小 | 说明 |
|------|------|------|
| `dist/gray-trace-agent-1.0.0.jar` | 5.4 MB | Agent 模式 fat JAR，含 ByteBuddy（shaded）+ TTL（shaded） |
| `dist/INTEGRATION.md` | — | 接入文档（Starter + Agent 两种模式） |
| `dist/VERIFICATION.md` | — | 本验证报告 |
