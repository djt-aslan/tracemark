# gray-trace

全链路流量灰度染色组件，通过 **Java Agent** 方式接入，零侵入地在线程、HTTP 调用、异步任务、消息队列之间传递灰度标。

## 功能

| 场景 | 支持框架 |
|------|---------|
| 入口染色 | Servlet（Spring Boot 2.x / 3.x）、WebFlux |
| HTTP 出口 | RestTemplate、OkHttp、JDK HttpClient、OpenFeign |
| 异步传递 | `@Async`、`ThreadPoolExecutor`、`CompletableFuture` |
| 消息队列 | RocketMQ 4.x 生产者 + 消费者（默认关闭） |

## 快速接入

无需修改代码或 pom，添加 JVM 启动参数：

```
-javaagent:/path/to/gray-trace-agent-1.0.0.jar -Dgray.trace.enabled=true
```

请求携带 Header `x-gray-tag: gray`，灰度标即自动在整条调用链中传递。

## 配置

所有配置均有默认值，不写配置即可正常工作。按需通过 JVM `-D` 参数覆盖：

```bash
java \
  -javaagent:/path/to/gray-trace-agent-1.0.0.jar \
  -Dgray.trace.enabled=true \
  -Dgray.trace.mq.enabled=false \
  -Dgray.trace.web-flux.enabled=false \
  -jar your-service.jar
```

完整配置项见 [docs/INTEGRATION.md](docs/INTEGRATION.md)。

## 模块结构

```
gray-trace-core     # GrayContext / GrayConstants / GrayProperties
gray-trace-agent    # ByteBuddy fat JAR，premain 入口
```

## 文档

- [接入文档](docs/INTEGRATION.md)
- [验证报告](docs/VERIFICATION.md)