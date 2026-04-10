# gray-trace

全链路流量灰度染色组件，通过 **Java Agent** 方式接入，零侵入地在线程、HTTP 调用、异步任务、消息队列之间传递灰度标。

## 功能

### 入口染色（提取 Header）

| 类型 | 支持 | 说明 |
|------|------|------|
| javax Servlet | ✅ | Spring Boot 2.x |
| jakarta Servlet | ✅ | Spring Boot 3.x |
| WebFlux | ❌ | 待实现 |

### 出口染色（注入 Header）

| 类型 | 支持 | 说明 |
|------|------|------|
| RestTemplate | ✅ | Spring RestTemplate |
| OkHttp | ✅ | OkHttp 3.x / 4.x |
| Apache HttpClient 4.x | ✅ | org.apache.http |
| Apache HttpClient 5.x | ✅ | org.apache.hc.client5 |
| JDK HttpClient | ❌ | 待实现（Java 11+） |
| OpenFeign | ❌ | 待实现 |
| WebClient | ❌ | 待实现 |

### 异步传递

| 类型 | 支持 | 说明 |
|------|------|------|
| ThreadPoolExecutor | ✅ | TTL 包装 |
| CompletableFuture | ✅ | TTL 包装 |
| @Async | ✅ | Spring @Async |

### 消息队列

| 类型 | 支持 | 说明 |
|------|------|------|
| RocketMQ Producer | ✅ | 注入消息属性 |
| RocketMQ Consumer | ❌ | 待实现 |

## 快速接入

无需修改代码或 pom，添加 JVM 启动参数：

```bash
java -javaagent:/path/to/gray-trace-agent-1.0.0.jar \
     -Dgray.trace.enabled=true \
     -jar your-service.jar
```

请求携带 Header `x-gray-tag: gray-v1`，灰度标即自动在整条调用链中传递。

## 配置

所有配置均有默认值，不写配置即可正常工作。按需通过 JVM `-D` 参数覆盖：

```bash
java \
  -javaagent:/path/to/gray-trace-agent-1.0.0.jar \
  -Dgray.trace.enabled=true \
  -Dgray.trace.servlet.enabled=true \
  -Dgray.trace.rest-template.enabled=true \
  -Dgray.trace.ok-http.enabled=true \
  -Dgray.trace.apache-http-client.enabled=true \
  -Dgray.trace.thread-pool.enabled=true \
  -Dgray.trace.completable-future.enabled=true \
  -Dgray.trace.mq.enabled=false \
  -Dgray.trace.log.enabled=false \
  -jar your-service.jar
```

### 配置项说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `gray.trace.enabled` | `true` | 全局总开关 |
| `gray.trace.servlet.enabled` | `true` | Servlet 入口提取 Header |
| `gray.trace.rest-template.enabled` | `true` | RestTemplate 出口注入 |
| `gray.trace.ok-http.enabled` | `true` | OkHttp 出口注入 |
| `gray.trace.apache-http-client.enabled` | `true` | Apache HttpClient 出口注入 |
| `gray.trace.thread-pool.enabled` | `true` | 线程池上下文传递 |
| `gray.trace.completable-future.enabled` | `true` | CompletableFuture 异步传递 |
| `gray.trace.mq.enabled` | `false` | MQ 染色开关（默认关闭） |
| `gray.trace.log.enabled` | `false` | 链路追踪日志（默认关闭） |

### 链路追踪日志

开启后输出 DEBUG 级别日志，用于排查问题：

```bash
-Dgray.trace.log.enabled=true
```

日志格式：
```
[GrayTrace] 入口提取 → tag=gray-v1, uri=/api/test, thread=http-nio-8080-exec-1
[GrayTrace] 出口注入 → tag=gray-v1, url=http://下游服务/api, thread=http-nio-8080-exec-1
[GrayTrace] 线程池传递 → tag=gray-v1, pool=custom-pool, thread=pool-1
[GrayTrace] 上下文清理 → tag=gray-v1, thread=http-nio-8080-exec-1
```

## 模块结构

```
gray-trace-core     # GrayContext / GrayConstants / GrayProperties
gray-trace-agent    # ByteBuddy fat JAR，premain 入口
```

## 文档

- [接入文档](docs/INTEGRATION.md)
- [验证报告](docs/VERIFICATION.md)