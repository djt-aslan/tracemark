# gray-trace 接入文档

> 版本：1.0.0　　更新：2026-04-10

## 概述

`gray-trace` 是全链路流量灰度染色组件，通过 **Java Agent** 方式接入：

| 接入方式 | 适用场景 | 改动量 |
|---------|---------|--------|
| **Agent 模式** | 无法改动代码/依赖，或使用 Spring Boot 3.x | 仅加 JVM 启动参数 |

Agent 模式可实现：

- **入口染色**：从 HTTP Header `x-gray-tag` 提取灰度标，写入线程上下文
- **同步传递**：RestTemplate / OkHttp / JDK HttpClient / Apache HttpClient 4.x & 5.x / Feign 出口自动注入 Header
- **异步传递**：`@Async` / `ThreadPoolExecutor` / `CompletableFuture` 自动传递
- **MQ 传递**：RocketMQ 生产者/消费者注入消息属性（默认关闭，按需开启）

---

## 一、Agent 模式

### 1. 获取 Agent JAR

```
dist/gray-trace-agent-1.0.0.jar   （5.4 MB fat JAR，含所有依赖，无需额外 pom）
```

将 JAR 上传到服务器，例如：

```bash
/opt/agents/gray-trace-agent-1.0.0.jar
```

### 2. 修改 JVM 启动参数

```bash
# 最简接入（其余全部默认）
java -javaagent:/opt/agents/gray-trace-agent-1.0.0.jar \
     -jar your-service.jar
```

**全量参数（按需选用）：**

```bash
java \
  -javaagent:/opt/agents/gray-trace-agent-1.0.0.jar \
  -Dgray.trace.enabled=true \
  -Dgray.trace.servlet.enabled=true \
  -Dgray.trace.rest-template.enabled=true \
  -Dgray.trace.ok-http.enabled=true \
  -Dgray.trace.http-client.enabled=true \
  -Dgray.trace.thread-pool.enabled=true \
  -Dgray.trace.mq.enabled=false \
  -Dgray.trace.mq.producer=true \
  -Dgray.trace.mq.consumer=true \
  -jar your-service.jar
```

> Agent 模式通过 **ByteBuddy 字节码插桩**在运行时改写目标类，无需改动 pom 或代码。

### 3. 各容器/平台接入

#### Docker / docker-compose

```yaml
services:
  your-service:
    image: your-service:latest
    volumes:
      - /opt/agents:/opt/agents
    environment:
      JAVA_TOOL_OPTIONS: >-
        -javaagent:/opt/agents/gray-trace-agent-1.0.0.jar
        -Dgray.trace.enabled=true
```

#### Kubernetes

```yaml
spec:
  template:
    spec:
      initContainers:
        - name: copy-agent
          image: busybox
          command: ["cp", "/agents/gray-trace-agent-1.0.0.jar", "/opt/agents/"]
          volumeMounts:
            - name: agent-volume
              mountPath: /opt/agents
      containers:
        - name: your-service
          env:
            - name: JAVA_TOOL_OPTIONS
              value: >-
                -javaagent:/opt/agents/gray-trace-agent-1.0.0.jar
                -Dgray.trace.enabled=true
          volumeMounts:
            - name: agent-volume
              mountPath: /opt/agents
      volumes:
        - name: agent-volume
          emptyDir: {}
```

> 推荐将 agent JAR 打入基础镜像，避免每次 initContainer 复制。

#### IDEA 本地调试

在 **Run/Debug Configurations → VM options** 中添加：

```
-javaagent:/opt/agents/gray-trace-agent-1.0.0.jar -Dgray.trace.enabled=true
```

#### Spring Boot Maven Plugin

```xml
<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <configuration>
    <jvmArguments>
      -javaagent:/opt/agents/gray-trace-agent-1.0.0.jar
      -Dgray.trace.enabled=true
    </jvmArguments>
  </configuration>
</plugin>
```

### 4. Agent 插桩目标

| 插桩点 | 目标类#方法 | 说明 |
|-------|-----------|------|
| Servlet 入口 (javax) | `javax.servlet.http.HttpServlet#service()` | 提取 `x-gray-tag` Header |
| Servlet 入口 (jakarta) | `jakarta.servlet.http.HttpServlet#service()` | Spring Boot 3.x 自动识别 |
| RestTemplate | `org.springframework.http.client.AbstractClientHttpRequest#execute()` | 注入 Header |
| OkHttp | `okhttp3.internal.connection.RealCall#execute()` | 注入 Header |
| 线程池 | `java.util.concurrent.ThreadPoolExecutor#execute()` | TTL 包装 Runnable |

---

## 二、配置参数速查

所有参数通过 JVM `-D` 系统属性传入。

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `gray.trace.enabled` | `true` | 全局总开关，`false` 时完全不生效 |
| `gray.trace.servlet.enabled` | `true` | Servlet 入口过滤 |
| `gray.trace.rest-template.enabled` | `true` | RestTemplate 出口注入 |
| `gray.trace.ok-http.enabled` | `true` | OkHttp 出口注入 |
| `gray.trace.http-client.enabled` | `true` | JDK HttpClient 出口注入 |
| `gray.trace.apache-http-client.enabled` | `true` | Apache HttpClient 4.x / 5.x 出口注入 |
| `gray.trace.feign.enabled` | `true` | OpenFeign 出口注入 |
| `gray.trace.thread-pool.enabled` | `true` | 线程池上下文传递 |
| `gray.trace.mq.enabled` | **`false`** | MQ 染色总开关（**默认关闭**） |
| `gray.trace.mq.producer` | `true` | RocketMQ 生产者注入消息属性 |
| `gray.trace.mq.consumer` | `true` | RocketMQ 消费者恢复上下文 |

> **为什么 MQ 默认关闭？**
> MQ 消息可能被延迟消费、重试、或路由到不同消费者，盲目透传灰度标可能导致消费者误路由到灰度版本。请在理解业务影响后手动开启。

---

## 三、工作原理

```
APISIX 网关
  │  注入 x-gray-tag: gray-v1
  ▼
Java 微服务（Agent 接入）
  │
  ├─ [ByteBuddy 插桩]
  │       提取 x-gray-tag Header → GrayContext.set("gray-v1")
  │
  ├─ [业务代码]  GrayContext.get() == "gray-v1"
  │
  ├─ [RestTemplate / OkHttp / Apache HttpClient / Feign]
  │       出口请求自动追加 x-gray-tag: gray-v1
  │       └─ 下游服务收到 x-gray-tag: gray-v1，重复以上流程
  │
  ├─ [@Async / ThreadPool / CompletableFuture]
  │       TTL 在任务提交时捕获，执行时恢复
  │       └─ 工作线程中 GrayContext.get() == "gray-v1"
  │
  └─ [RocketMQ Producer]（需开启）
          消息 UserProperty["grayTag"] = "gray-v1"
          └─ 消费端恢复 GrayContext.set("gray-v1")
```

---

## 四、Istio 路由配置

Agent 负责在服务间传递 `x-gray-tag`，Istio 根据该 Header 决定路由目标：

```yaml
# VirtualService：按 x-gray-tag 路由
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: your-service
spec:
  http:
    - match:
        - headers:
            x-gray-tag:
              exact: "gray-v1"
      route:
        - destination:
            host: your-service
            subset: gray-v1
    - route:
        - destination:
            host: your-service
            subset: stable
---
# DestinationRule：定义灰度/稳定子集
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: your-service
spec:
  host: your-service
  subsets:
    - name: gray-v1
      labels:
        version: gray-v1
    - name: stable
      labels:
        version: stable
```

---

## 五、验证接入是否成功

启动服务后，发送带 `x-gray-tag` 的请求：

```bash
# 验证 Servlet 入口
curl -H "x-gray-tag: gray-v1" http://your-service/gray/servlet
# 期望：{"gray_tag":"gray-v1"}

# 验证 HTTP 出口传递（RestTemplate + OkHttp）
curl -H "x-gray-tag: gray-v1" http://your-service/gray/http
# 期望：rest_template 和 okhttp 响应中均含 "x_gray_tag":"gray-v1"

# 验证异步传递
curl -H "x-gray-tag: gray-v1" http://your-service/gray/async
# 期望：main_thread_tag 和 async_thread_tag 均为 "gray-v1"
```

或在业务代码中临时加一行日志（验证完删除）：

```java
log.info("当前灰度标: {}", GrayContext.get()); // 期望输出 gray-v1
```

> **注意**：Agent 模式下 `GrayContext` 由 agent JAR 提供，业务代码无需引入任何依赖即可直接访问。
> 若 IDE 报编译错误，可加可选依赖：
> ```xml
> <dependency>
>     <groupId>io.tracemark</groupId>
>     <artifactId>gray-trace-core</artifactId>
>     <version>1.0.0</version>
>     <scope>provided</scope>
> </dependency>
> ```

---

## 六、常见问题

### Q：启动时报 `ClassNotFoundException: io.tracemark.agent.GrayTraceAgent`

检查 `-javaagent` 路径是否正确，JAR 是否存在且可读：

```bash
ls -la /opt/agents/gray-trace-agent-1.0.0.jar
```

### Q：`@Async` 方法中灰度标丢失

Agent 通过 ByteBuddy 插桩 `ThreadPoolExecutor.execute()`，但若使用了自定义线程工厂且绕过标准 `execute()`，可能失效。

### Q：多个 `-javaagent` 与 SkyWalking 冲突

两者可以共存，加载顺序为命令行顺序（先写先生效）：

```bash
java -javaagent:/opt/skywalking-agent/skywalking-agent.jar \
     -javaagent:/opt/agents/gray-trace-agent-1.0.0.jar \
     -jar your-service.jar
```

ByteBuddy 与 SkyWalking 插桩互不干扰。

### Q：如何验证某个开关是否生效

将该渠道开关设为 `false`，发送灰度请求，观察对应渠道的下游响应中是否不含 `x-gray-tag`：

```bash
-Dgray.trace.rest-template.enabled=false
```

```bash
curl -H "x-gray-tag: gray-v1" http://your-service/gray/http
# 期望：rest_template 响应中 x_gray_tag=null，okhttp 响应中 x_gray_tag=gray-v1
```

---

## 文件说明

```
dist/
├── gray-trace-agent-1.0.0.jar   ← Agent 模式直接使用（5.4 MB fat JAR）
├── INTEGRATION.md               ← 本文档
└── VERIFICATION.md              ← 验证报告
```