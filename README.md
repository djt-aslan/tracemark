# gray-trace

全链路流量灰度染色组件，支持 **Starter** 和 **Java Agent** 两种接入方式，零侵入地在线程、HTTP 调用、异步任务、消息队列之间传递灰度标。

## 功能

| 场景 | 支持框架 |
|------|---------|
| 入口染色 | Servlet（Spring Boot 2.x / 3.x）、WebFlux |
| HTTP 出口 | RestTemplate、OkHttp、JDK HttpClient、OpenFeign |
| 异步传递 | `@Async`、`ThreadPoolExecutor`、`CompletableFuture` |
| 消息队列 | RocketMQ 4.x 生产者 + 消费者（默认关闭） |

## 快速接入

### 方式一：Starter（推荐）

```xml
<dependency>
    <groupId>io.tracemark</groupId>
    <artifactId>gray-trace-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

仅此一行，Spring Boot 自动装配完成所有初始化，无需任何代码改动。

请求携带 Header `x-gray-tag: gray`，灰度标即自动在整条调用链中传递。

### 方式二：Java Agent

无需修改代码或 pom，添加 JVM 启动参数：

```
-javaagent:/path/to/gray-trace-agent-1.0.0.jar -Dgray.trace.enabled=true
```

## 配置

所有配置均有默认值，不写配置即可正常工作。按需在 `application.yml` 中覆盖：

```yaml
gray:
  trace:
    enabled: true          # 全局开关
    mq:
      enabled: false       # RocketMQ 染色（默认关闭，需显式开启）
    web-flux:
      enabled: false       # WebFlux 支持（MVC 项目不需要）
```

完整配置项见 [docs/INTEGRATION.md](docs/INTEGRATION.md)。

## 模块结构

```
gray-trace-core                      # GrayContext / GrayConstants / GrayProperties
gray-trace-spring-boot-autoconfigure # 各框架拦截器 + 自动装配
gray-trace-spring-boot-starter       # 依赖聚合（用户只需引这一个）
gray-trace-agent                     # ByteBuddy fat JAR，premain 入口
gray-trace-test                      # 集成测试服务
```

## 文档

- [接入文档](docs/INTEGRATION.md)
- [验证报告](docs/VERIFICATION.md)
