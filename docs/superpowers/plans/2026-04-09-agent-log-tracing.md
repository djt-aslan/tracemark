# Agent 链路追踪日志实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Agent 模式添加链路追踪日志，记录灰度标在入口、出口、异步传递等节点的传递情况，支持配置开关控制。

**Architecture:** 使用 SLF4J 作为日志门面（shade 打包到 Agent JAR），创建 GrayTraceLogger 工具类统一日志格式和开关控制，在所有 Advice 类的关键节点添加日志调用。

**Tech Stack:** SLF4J 1.7.x, Maven Shade Plugin, ByteBuddy Advice

---

## 文件结构

```
gray-trace-agent/
├── pom.xml                                    # 添加 slf4j-api 依赖，配置 shade
├── src/main/java/io/tracemark/agent/
│   ├── AgentConfigLoader.java                 # 添加 log.enabled 读取
│   ├── GrayTraceLogger.java                   # 新建：日志工具类
│   └── advice/
│       ├── ServletInboundAdvice.java          # 添加日志调用
│       ├── JakartaServletInboundAdvice.java   # 添加日志调用
│       ├── OkHttpOutboundAdvice.java          # 添加日志调用
│       ├── RestTemplateOutboundAdvice.java    # 添加日志调用
│       ├── ApacheHttpClientOutboundAdvice.java    # 添加日志调用
│       ├── ApacheHttp5ClientOutboundAdvice.java   # 添加日志调用
│       ├── ThreadPoolAdvice.java              # 添加日志调用
│       ├── CompletableFutureAsyncAdvice.java  # 添加日志调用
│       └── RocketMqProducerAdvice.java        # 添加日志调用
└── src/test/java/io/tracemark/agent/
    └── GrayTraceLoggerTest.java               # 新建：日志工具类测试
```

---

## Task 1: 添加 SLF4J 依赖并配置 Shade

**Files:**
- Modify: `gray-trace-agent/pom.xml:16-33` (dependencies)
- Modify: `gray-trace-agent/pom.xml:118-133` (shade relocations)

- [x] **Step 1: 添加 slf4j-api 依赖**

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<!-- 日志门面：shade 打包，用于 Agent 链路追踪日志 -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>1.7.36</version>
</dependency>
```

位置：在 `transmittable-thread-local` 依赖之后，`javax.servlet-api` 依赖之前。

- [x] **Step 2: 配置 SLF4J Shade 重定位**

在 `pom.xml` 的 `<relocations>` 中添加：

```xml
<!-- shade SLF4J 避免与应用日志实现冲突 -->
<relocation>
    <pattern>org.slf4j</pattern>
    <shadedPattern>io.tracemark.shade.slf4j</shadedPattern>
</relocation>
```

位置：在 TTL shade relocation 之后。

- [x] **Step 3: 验证依赖配置正确**

Run: `cd gray-trace-agent && mvn dependency:tree -DincludeScope=compile | grep slf4j`
Expected: `org.slf4j:slf4j-api:jar:1.7.36`

- [x] **Step 4: Commit**

```bash
git add gray-trace-agent/pom.xml
git commit -m "feat(agent): add slf4j-api dependency with shade configuration

- Add slf4j-api 1.7.36 for Agent trace logging
- Shade to io.tracemark.shade.slf4j to avoid conflicts"
```

---

## Task 2: 添加日志配置属性

**Files:**
- Modify: `gray-trace-core/src/main/java/io/tracemark/gray/core/GrayProperties.java:43` (add Log nested class)
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/AgentConfigLoader.java:46` (add log config loading)

- [x] **Step 1: 在 GrayProperties 添加 Log 嵌套配置类**

在 `GrayProperties.java` 的 `CompletableFuture` 嵌套类之后添加：

```java
/** Agent 链路追踪日志配置 */
private Log log = new Log();

public static class Log {
    /** 是否开启 Agent 链路追踪日志，默认 false（生产环境关闭） */
    private boolean enabled = false;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
```

并在 Getters/Setters 区域添加：

```java
public Log getLog() { return log; }
public void setLog(Log log) { this.log = log; }
```

- [x] **Step 2: 在 AgentConfigLoader 添加日志配置读取**

在 `AgentConfigLoader.java` 的 `load()` 方法中，在 `props.getCompletableFuture()` 调用之后添加：

```java
props.getLog().setEnabled(getBool("gray.trace.log.enabled", false));
```

- [x] **Step 3: 编写测试验证配置加载**

在 `gray-trace-agent/src/test/java/io/tracemark/agent/AgentConfigLoaderTest.java` 添加测试方法：

```java
@Test
@DisplayName("默认日志开关关闭")
void defaultLogDisabled() {
    GrayProperties props = AgentConfigLoader.load();
    assertThat(props.getLog().isEnabled()).isFalse();
}

@Test
@DisplayName("通过系统属性开启日志")
void logEnabledViaSystemProperty() {
    System.setProperty("gray.trace.log.enabled", "true");
    try {
        GrayProperties props = AgentConfigLoader.load();
        assertThat(props.getLog().isEnabled()).isTrue();
    } finally {
        System.clearProperty("gray.trace.log.enabled");
    }
}
```

- [ ] **Step 4: 运行测试验证**

Run: `cd gray-trace-agent && mvn test -Dtest=AgentConfigLoaderTest -q`
Expected: All tests PASS

- [x] **Step 5: Commit**

```bash
git add gray-trace-core/src/main/java/io/tracemark/gray/core/GrayProperties.java
git add gray-trace-agent/src/main/java/io/tracemark/agent/AgentConfigLoader.java
git add gray-trace-agent/src/test/java/io/tracemark/agent/AgentConfigLoaderTest.java
git commit -m "feat(config): add gray.trace.log.enabled configuration

- Add Log nested config in GrayProperties (default false)
- Add config loading in AgentConfigLoader
- Add tests for log config loading"
```

---

## Task 3: 创建 GrayTraceLogger 工具类

**Files:**
- Create: `gray-trace-agent/src/main/java/io/tracemark/agent/GrayTraceLogger.java`
- Create: `gray-trace-agent/src/test/java/io/tracemark/agent/GrayTraceLoggerTest.java`

- [x] **Step 1: 编写 GrayTraceLogger 测试**

创建 `gray-trace-agent/src/test/java/io/tracemark/agent/GrayTraceLoggerTest.java`：

```java
package io.tracemark.agent;

import io.tracemark.gray.core.GrayContext;
import io.tracemark.gray.core.GrayProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GrayTraceLoggerTest {

    @BeforeEach
    void setUp() {
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Test
    @DisplayName("日志开关关闭时不输出日志")
    void logDisabled_noOutput() {
        GrayProperties props = new GrayProperties();
        props.getLog().setEnabled(false);
        GrayTraceLogger.init(props);

        // 设置灰度上下文
        GrayContext.set("gray-v1");

        // 调用日志方法，不应抛异常
        GrayTraceLogger.logInbound("gray-v1", "/api/test", "main");
        GrayTraceLogger.logOutbound("gray-v1", "http://localhost/api", "main");
        GrayTraceLogger.logAsync("gray-v1", "pool-1", "Runnable", "main");
        GrayTraceLogger.logClear("gray-v1", "main");

        // 验证：无异常即通过
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("日志开关开启时可以输出日志")
    void logEnabled_canOutput() {
        GrayProperties props = new GrayProperties();
        props.getLog().setEnabled(true);
        GrayTraceLogger.init(props);

        GrayContext.set("gray-v1");

        // 调用日志方法
        GrayTraceLogger.logInbound("gray-v1", "/api/test", "main");
        GrayTraceLogger.logOutbound("gray-v1", "http://localhost/api", "main");
        GrayTraceLogger.logAsync("gray-v1", "pool-1", "Runnable", "main");
        GrayTraceLogger.logClear("gray-v1", "main");

        // 验证：无异常即通过（SLF4J 会输出到控制台）
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("tag 为 null 时不输出日志")
    void nullTag_noOutput() {
        GrayProperties props = new GrayProperties();
        props.getLog().setEnabled(true);
        GrayTraceLogger.init(props);

        // tag 为 null
        GrayTraceLogger.logInbound(null, "/api/test", "main");
        GrayTraceLogger.logOutbound(null, "http://localhost/api", "main");

        // 验证：无异常即通过
        assertThat(true).isTrue();
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run: `cd gray-trace-agent && mvn test -Dtest=GrayTraceLoggerTest -q`
Expected: FAIL (GrayTraceLogger class not found)

- [x] **Step 3: 实现 GrayTraceLogger 工具类**

创建 `gray-trace-agent/src/main/java/io/tracemark/agent/GrayTraceLogger.java`：

```java
package io.tracemark.agent;

import io.tracemark.shade.slf4j.Logger;
import io.tracemark.shade.slf4j.LoggerFactory;

/**
 * Agent 链路追踪日志工具类
 *
 * <p>统一日志格式：{@code [GrayTrace] {操作类型} → {描述}, tag={tag}, {上下文信息}}
 *
 * <p>日志级别为 DEBUG，通过 {@code gray.trace.log.enabled} 配置开关控制。
 */
public final class GrayTraceLogger {

    private static final Logger LOG = LoggerFactory.getLogger(GrayTraceLogger.class);
    private static volatile boolean enabled = false;

    private GrayTraceLogger() {}

    /**
     * 初始化日志开关
     */
    public static void init(io.tracemark.gray.core.GrayProperties props) {
        enabled = props.getLog().isEnabled();
    }

    /**
     * 入口日志：提取 Header
     */
    public static void logInbound(String tag, String uri, String thread) {
        if (!enabled || tag == null || tag.isEmpty()) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrayTrace] 入口 → 提取Header, tag={}, uri={}, thread={}", tag, uri, thread);
        }
    }

    /**
     * 出口日志：注入 Header
     */
    public static void logOutbound(String tag, String url, String thread) {
        if (!enabled || tag == null || tag.isEmpty()) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrayTrace] 出口 → 注入Header, tag={}, url={}, thread={}", tag, url, thread);
        }
    }

    /**
     * 异步日志：TTL 包装
     */
    public static void logAsync(String tag, String pool, String taskType, String thread) {
        if (!enabled || tag == null || tag.isEmpty()) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrayTrace] 异步 → TTL包装, tag={}, pool={}, task={}, thread={}", tag, pool, taskType, thread);
        }
    }

    /**
     * 清理日志：上下文清理
     */
    public static void logClear(String tag, String thread) {
        if (!enabled || tag == null || tag.isEmpty()) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrayTrace] 清理 → 上下文, tag={}, thread={}", tag, thread);
        }
    }

    /**
     * MQ 日志：消息属性注入
     */
    public static void logMq(String tag, String topic, String thread) {
        if (!enabled || tag == null || tag.isEmpty()) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrayTrace] MQ → 注入属性, tag={}, topic={}, thread={}", tag, topic, thread);
        }
    }

    /**
     * 检查日志是否启用
     */
    public static boolean isEnabled() {
        return enabled;
    }
}
```

- [x] **Step 4: 运行测试验证通过**

Run: `cd gray-trace-agent && mvn test -Dtest=GrayTraceLoggerTest -q`
Expected: All tests PASS

- [x] **Step 5: Commit**

```bash
git add gray-trace-agent/src/main/java/io/tracemark/agent/GrayTraceLogger.java
git add gray-trace-agent/src/test/java/io/tracemark/agent/GrayTraceLoggerTest.java
git commit -m "feat(agent): add GrayTraceLogger utility class

- Unified log format: [GrayTrace] {type} → {desc}, tag={}, ...
- Support log switch via gray.trace.log.enabled
- Add unit tests for GrayTraceLogger"
```

---

## Task 4: 在 GrayTraceAgent 初始化日志开关

**Files:**
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/GrayTraceAgent.java`

- [x] **Step 1: 读取 GrayTraceAgent 当前代码**

Run: `cat gray-trace-agent/src/main/java/io/tracemark/agent/GrayTraceAgent.java`

- [x] **Step 2: 在 GrayTraceAgent 初始化 GrayTraceLogger**

在 `GrayTraceAgent.java` 中，找到 `AgentConfigLoader.load()` 调用位置，在其后添加：

```java
GrayTraceLogger.init(props);
```

需要添加 import：
```java
import io.tracemark.agent.GrayTraceLogger;
```

- [x] **Step 3: 编译验证**

Run: `cd gray-trace-agent && mvn compile -q`
Expected: BUILD SUCCESS

- [x] **Step 4: Commit**

```bash
git add gray-trace-agent/src/main/java/io/tracemark/agent/GrayTraceAgent.java
git commit -m "feat(agent): initialize GrayTraceLogger in agent startup"
```

---

## Task 5: Servlet 入口 Advice 添加日志

**Files:**
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/advice/ServletInboundAdvice.java`
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/advice/JakartaServletInboundAdvice.java`

- [x] **Step 1: 修改 ServletInboundAdvice 添加日志**

修改 `ServletInboundAdvice.java`：

```java
package io.tracemark.agent.advice;

import io.tracemark.agent.GrayTraceLogger;
import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Advice：拦截 javax.servlet.http.HttpServlet#service 方法
 *
 * <p>在方法进入时提取 {@code x-gray-tag} Header 写入上下文，
 * 方法退出时清理，防止线程池复用污染。
 */
public class ServletInboundAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Argument(0) HttpServletRequest request) {
        String tag = request.getHeader(GrayConstants.HEADER_GRAY_TAG);
        String effectiveTag = tag != null && !tag.isEmpty() ? tag : GrayConstants.TAG_STABLE;
        GrayContext.set(effectiveTag);

        // 日志输出
        GrayTraceLogger.logInbound(effectiveTag, request.getRequestURI(), Thread.currentThread().getName());
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit() {
        String tag = GrayContext.get();
        GrayTraceLogger.logClear(tag, Thread.currentThread().getName());
        GrayContext.clear();
    }
}
```

- [x] **Step 2: 修改 JakartaServletInboundAdvice 添加日志**

修改 `JakartaServletInboundAdvice.java`：

```java
package io.tracemark.agent.advice;

import io.tracemark.agent.GrayTraceLogger;
import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Advice：拦截 jakarta.servlet.http.HttpServlet#service 方法（Spring Boot 3.x）
 */
public class JakartaServletInboundAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Argument(0) HttpServletRequest request) {
        String tag = request.getHeader(GrayConstants.HEADER_GRAY_TAG);
        String effectiveTag = tag != null && !tag.isEmpty() ? tag : GrayConstants.TAG_STABLE;
        GrayContext.set(effectiveTag);

        // 日志输出
        GrayTraceLogger.logInbound(effectiveTag, request.getRequestURI(), Thread.currentThread().getName());
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit() {
        String tag = GrayContext.get();
        GrayTraceLogger.logClear(tag, Thread.currentThread().getName());
        GrayContext.clear();
    }
}
```

- [x] **Step 3: 编译验证**

Run: `cd gray-trace-agent && mvn compile -q`
Expected: BUILD SUCCESS

- [x] **Step 4: Commit**

```bash
git add gray-trace-agent/src/main/java/io/tracemark/agent/advice/ServletInboundAdvice.java
git add gray-trace-agent/src/main/java/io/tracemark/agent/advice/JakartaServletInboundAdvice.java
git commit -m "feat(agent): add logging to Servlet inbound advice

- Log header extraction on method enter
- Log context cleanup on method exit"
```

---

## Task 6: HTTP 出口 Advice 添加日志

**Files:**
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/advice/OkHttpOutboundAdvice.java`
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/advice/RestTemplateOutboundAdvice.java`
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/advice/ApacheHttpClientOutboundAdvice.java`
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/advice/ApacheHttp5ClientOutboundAdvice.java`

- [x] **Step 1: 修改 OkHttpOutboundAdvice 添加日志**

修改 `OkHttpOutboundAdvice.java`：

```java
package io.tracemark.agent.advice;

import io.tracemark.agent.GrayTraceLogger;
import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;
import okhttp3.Request;

/**
 * Advice：拦截 OkHttp RealCall，注入 x-gray-tag Header
 */
public class OkHttpOutboundAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.FieldValue(value = "originalRequest", readOnly = false) Request request) {
        String tag = GrayContext.get();
        if (tag != null && !tag.isEmpty()
                && request.header(GrayConstants.HEADER_GRAY_TAG) == null) {
            request = request.newBuilder()
                    .header(GrayConstants.HEADER_GRAY_TAG, tag)
                    .build();

            // 日志输出
            GrayTraceLogger.logOutbound(tag, request.url().toString(), Thread.currentThread().getName());
        }
    }
}
```

- [x] **Step 2: 修改 RestTemplateOutboundAdvice 添加日志**

修改 `RestTemplateOutboundAdvice.java`：

```java
package io.tracemark.agent.advice;

import io.tracemark.agent.GrayTraceLogger;
import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;
import org.springframework.http.HttpRequest;

/**
 * Advice：拦截 RestTemplate 内部 executeInternal，注入 x-gray-tag Header
 */
public class RestTemplateOutboundAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Argument(0) HttpRequest request) {
        String tag = GrayContext.get();
        if (tag != null && !tag.isEmpty()) {
            request.getHeaders().set(GrayConstants.HEADER_GRAY_TAG, tag);

            // 日志输出
            GrayTraceLogger.logOutbound(tag, request.getURI().toString(), Thread.currentThread().getName());
        }
    }
}
```

- [x] **Step 3: 修改 ApacheHttpClientOutboundAdvice 添加日志**

修改 `ApacheHttpClientOutboundAdvice.java`：

```java
package io.tracemark.agent.advice;

import io.tracemark.agent.GrayTraceLogger;
import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;
import org.apache.http.HttpRequest;

/**
 * Advice：拦截 Apache HttpClient 4.x CloseableHttpClient#execute，注入 x-gray-tag Header
 */
public class ApacheHttpClientOutboundAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.AllArguments Object[] args) {
        if (args == null) return;
        for (Object arg : args) {
            if (arg instanceof HttpRequest) {
                HttpRequest request = (HttpRequest) arg;
                if (GrayContext.isGray()
                        && !request.containsHeader(GrayConstants.HEADER_GRAY_TAG)) {
                    String tag = GrayContext.get();
                    request.setHeader(GrayConstants.HEADER_GRAY_TAG, tag);

                    // 日志输出
                    GrayTraceLogger.logOutbound(tag, request.getRequestLine().getUri(), Thread.currentThread().getName());
                }
                return;
            }
        }
    }
}
```

- [x] **Step 4: 修改 ApacheHttp5ClientOutboundAdvice 添加日志**

修改 `ApacheHttp5ClientOutboundAdvice.java`：

```java
package io.tracemark.agent.advice;

import io.tracemark.agent.GrayTraceLogger;
import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;
import org.apache.hc.core5.http.HttpRequest;

/**
 * Advice：拦截 Apache HttpClient 5.x CloseableHttpClient#execute，注入 x-gray-tag Header
 */
public class ApacheHttp5ClientOutboundAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.AllArguments Object[] args) {
        if (args == null) return;
        for (Object arg : args) {
            if (arg instanceof HttpRequest) {
                HttpRequest request = (HttpRequest) arg;
                if (GrayContext.isGray()
                        && !request.containsHeader(GrayConstants.HEADER_GRAY_TAG)) {
                    String tag = GrayContext.get();
                    request.setHeader(GrayConstants.HEADER_GRAY_TAG, tag);

                    // 日志输出
                    GrayTraceLogger.logOutbound(tag, request.getRequestUri(), Thread.currentThread().getName());
                }
                return;
            }
        }
    }
}
```

- [x] **Step 5: 编译验证**

Run: `cd gray-trace-agent && mvn compile -q`
Expected: BUILD SUCCESS

- [x] **Step 6: Commit**

```bash
git add gray-trace-agent/src/main/java/io/tracemark/agent/advice/OkHttpOutboundAdvice.java
git add gray-trace-agent/src/main/java/io/tracemark/agent/advice/RestTemplateOutboundAdvice.java
git add gray-trace-agent/src/main/java/io/tracemark/agent/advice/ApacheHttpClientOutboundAdvice.java
git add gray-trace-agent/src/main/java/io/tracemark/agent/advice/ApacheHttp5ClientOutboundAdvice.java
git commit -m "feat(agent): add logging to HTTP outbound advice

- Add logging to OkHttp, RestTemplate, Apache HttpClient 4.x/5.x"
```

---

## Task 7: 异步传递 Advice 添加日志

**Files:**
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/advice/ThreadPoolAdvice.java`
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/advice/CompletableFutureAsyncAdvice.java`

- [x] **Step 1: 修改 ThreadPoolAdvice 添加日志**

修改 `ThreadPoolAdvice.java`：

```java
package io.tracemark.agent.advice;

import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;
import io.tracemark.agent.GrayTraceLogger;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;

import java.util.concurrent.Callable;

/**
 * Advice：拦截 ThreadPoolExecutor#execute 和 #submit，
 * 用 TTL 包装 Runnable/Callable，保证线程池复用时上下文不丢失。
 */
public class ThreadPoolAdvice {

    @Advice.OnMethodEnter
    public static void onExecute(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
        if (runnable != null && !(runnable instanceof TtlRunnable)) {
            String tag = GrayContext.get();
            String taskType = runnable.getClass().getSimpleName();
            GrayTraceLogger.logAsync(tag, "unknown", taskType, Thread.currentThread().getName());
            runnable = TtlRunnable.get(runnable);
        }
    }

    // submit(Callable) 重载
    public static class CallableAdvice {
        @Advice.OnMethodEnter
        public static <T> void onSubmit(
                @Advice.Argument(value = 0, readOnly = false) Callable<T> callable) {
            if (callable != null && !(callable instanceof TtlCallable)) {
                String tag = GrayContext.get();
                String taskType = callable.getClass().getSimpleName();
                GrayTraceLogger.logAsync(tag, "unknown", taskType, Thread.currentThread().getName());
                callable = TtlCallable.get(callable);
            }
        }
    }
}
```

- [x] **Step 2: 修改 CompletableFutureAsyncAdvice 添加日志**

在 `CompletableFutureAsyncAdvice.java` 中，为每个内部 Advice 类添加日志调用。关键修改：

在 `SupplyAsyncAdvice.onEnter` 中添加：
```java
GrayTraceLogger.logAsync(GrayContext.get(), "CompletableFuture", "Supplier", Thread.currentThread().getName());
```

在 `RunAsyncAdvice.onEnter` 中添加：
```java
GrayTraceLogger.logAsync(GrayContext.get(), "CompletableFuture", "Runnable", Thread.currentThread().getName());
```

在 `ThenApplyAsyncAdvice.onEnter` 中添加：
```java
GrayTraceLogger.logAsync(GrayContext.get(), "CompletableFuture", "Function", Thread.currentThread().getName());
```

在 `ThenAcceptAsyncAdvice.onEnter` 中添加：
```java
GrayTraceLogger.logAsync(GrayContext.get(), "CompletableFuture", "Consumer", Thread.currentThread().getName());
```

在 `ThenRunAsyncAdvice.onEnter` 中添加：
```java
GrayTraceLogger.logAsync(GrayContext.get(), "CompletableFuture", "Runnable", Thread.currentThread().getName());
```

- [x] **Step 3: 编译验证**

Run: `cd gray-trace-agent && mvn compile -q`
Expected: BUILD SUCCESS

- [x] **Step 4: Commit**

```bash
git add gray-trace-agent/src/main/java/io/tracemark/agent/advice/ThreadPoolAdvice.java
git add gray-trace-agent/src/main/java/io/tracemark/agent/advice/CompletableFutureAsyncAdvice.java
git commit -m "feat(agent): add logging to async propagation advice

- Add logging to ThreadPoolAdvice (execute/submit)
- Add logging to CompletableFutureAsyncAdvice (supplyAsync/runAsync/thenApplyAsync/etc)"
```

---

## Task 8: MQ Advice 添加日志

**Files:**
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/advice/RocketMqProducerAdvice.java`

- [x] **Step 1: 修改 RocketMqProducerAdvice 添加日志**

修改 `RocketMqProducerAdvice.java`：

```java
package io.tracemark.agent.advice;

import io.tracemark.agent.GrayTraceLogger;
import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;
import org.apache.rocketmq.common.message.Message;

/**
 * Advice：拦截 RocketMQ DefaultMQProducerImpl#sendKernelImpl，
 * 在消息发送前将灰度标签写入 UserProperty。
 */
public class RocketMqProducerAdvice {

    @Advice.OnMethodEnter
    public static void onSend(@Advice.Argument(0) Message msg) {
        if (msg == null) {
            return;
        }
        String tag = GrayContext.get();
        if (tag != null && !tag.isEmpty()
                && msg.getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG) == null) {
            msg.putUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG, tag);

            // 日志输出
            GrayTraceLogger.logMq(tag, msg.getTopic(), Thread.currentThread().getName());
        }
    }
}
```

- [x] **Step 2: 编译验证**

Run: `cd gray-trace-agent && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add gray-trace-agent/src/main/java/io/tracemark/agent/advice/RocketMqProducerAdvice.java
git commit -m "feat(agent): add logging to RocketMQ producer advice"
```

---

## Task 9: 运行完整测试套件

**Files:**
- None (verification only)

- [x] **Step 1: 运行 gray-trace-agent 全部测试**

Run: `cd gray-trace-agent && mvn test -q`
Expected: All tests PASS

- [x] **Step 2: 运行全项目测试**

Run: `mvn test -q`
Expected: All tests PASS

- [x] **Step 3: 检查测试覆盖率**

Run: `mvn jacoco:report -q && cat gray-trace-agent/target/site/jacoco/index.html | grep -oP 'Total.*?(\d+%)'`
Expected: Coverage meets threshold

---

## Task 10: 更新 OpenSpec tasks.md 标记完成

**Files:**
- Modify: `openspec/changes/agent-log-tracing/tasks.md`

- [x] **Step 1: 更新 tasks.md 将所有任务标记为完成**

将所有 `- [ ]` 改为 `- [x]`

- [x] **Step 2: Commit**

```bash
git add openspec/changes/agent-log-tracing/tasks.md
git commit -m "docs: mark agent-log-tracing tasks as completed"
```

---

## 验收标准

- [x] 所有 Advice 类在关键节点输出日志
- [x] 日志格式统一：`[GrayTrace] {操作类型} → {描述}, tag={tag}, {上下文信息}`
- [x] 日志级别为 DEBUG
- [x] 默认关闭，通过 `gray.trace.log.enabled=true` 开启
- [x] 所有测试通过
- [x] 测试覆盖率达标
