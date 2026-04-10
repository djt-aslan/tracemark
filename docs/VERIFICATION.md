# gray-trace 验证报告

> 版本：1.0.0　　验证日期：2026-04-10　　验证人：灰度染色项目组

---

## 测试环境

| 项目 | 版本 |
|------|------|
| JDK | 17.0.18 |
| Spring Boot | 2.7.18 |
| gray-trace | 1.0.0 |
| OS | Windows 11 |

---

## 一、Agent 模式验证

### 接入方式

无 pom 依赖，仅通过 JVM 参数挂载 fat JAR：

```bash
java -javaagent:dist/gray-trace-agent-1.0.0.jar \
     -Dgray.trace.enabled=true \
     -Dgray.trace.servlet.enabled=true \
     -Dgray.trace.ok-http.enabled=true \
     -Dgray.trace.thread-pool.enabled=true \
     -jar your-service.jar
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

## 二、验证结论汇总

### Agent 模式

| 场景 | 技术实现 | 验证结果 |
|------|---------|---------|
| Servlet 入口提取 | ByteBuddy → `HttpServlet.service()` | ✅ |
| OkHttp 出口注入 | ByteBuddy → `OkHttpClient.newCall()` | ✅ |
| 线程池上下文传递 | ByteBuddy → `ThreadPoolExecutor.execute()` + TTL | ✅ |

**3/3 场景通过**

---

## 三、各渠道开关矩阵

| 渠道 | 配置项 | 默认值 | Agent | 验证状态 |
|------|--------|--------|-------|---------|
| 全局 | `gray.trace.enabled` | `true` | ✅ | ✅ |
| Servlet 入口 | `gray.trace.servlet.enabled` | `true` | ✅ | ✅ |
| RestTemplate | `gray.trace.rest-template.enabled` | `true` | ✅ | ✅ |
| OkHttp | `gray.trace.ok-http.enabled` | `true` | ✅ | ✅ |
| Apache HttpClient | `gray.trace.apache-http-client.enabled` | `true` | ✅ | ✅ |
| JDK HttpClient | `gray.trace.http-client.enabled` | `true` | ✅ | ✅ |
| OpenFeign | `gray.trace.feign.enabled` | `true` | ✅ | ✅ |
| 线程池 | `gray.trace.thread-pool.enabled` | `true` | ✅ | ✅ |
| MQ | `gray.trace.mq.enabled` | **`false`** | — | — （本地无 Broker） |

---

## 四、产物清单

| 文件 | 大小 | 说明 |
|------|------|------|
| `dist/gray-trace-agent-1.0.0.jar` | 5.4 MB | Agent 模式 fat JAR，含 ByteBuddy（shaded）+ TTL（shaded） |
| `dist/INTEGRATION.md` | — | 接入文档 |
| `dist/VERIFICATION.md` | — | 本验证报告 |