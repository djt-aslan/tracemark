# Agent Apache HttpClient 出口透传 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 为 Java Agent 补全 Apache HttpComponents 4.x / 5.x 出口灰度标透传，使其与 Starter 功能对等。

**Architecture:** 沿用 Agent 现有的 ByteBuddy Advice + Transformer 二层模式。Advice 是普通 Java 类，包含带 `@Advice.OnMethodEnter` 注解的静态方法；Transformer 将 Advice 绑定到目标类的 `execute` 方法上，再由 `GrayTraceAgent` 在 JVM 启动时统一注册。使用 `@Advice.AllArguments Object[] args` 扫描参数，以 `instanceof HttpRequest` 定位请求对象，兼容 `execute` 的所有重载签名。

**Tech Stack:** ByteBuddy 1.14.18（已 shade 进 fat-jar）、Apache HttpComponents 4.5.14 / 5.3.1（`provided`，来自目标应用 classpath）、JUnit 5、AssertJ。

---

## File Map

| 操作 | 路径 | 职责 |
|------|------|------|
| CREATE | `gray-trace-agent/src/main/java/io/tracemark/agent/advice/ApacheHttpClientOutboundAdvice.java` | 4.x Advice：扫描参数注入 Header |
| CREATE | `gray-trace-agent/src/main/java/io/tracemark/agent/transformer/ApacheHttpClientOutboundTransformer.java` | 4.x Transformer：绑定 Advice 到目标类 |
| CREATE | `gray-trace-agent/src/main/java/io/tracemark/agent/advice/ApacheHttp5ClientOutboundAdvice.java` | 5.x Advice：同上，适配 5.x 包路径 |
| CREATE | `gray-trace-agent/src/main/java/io/tracemark/agent/transformer/ApacheHttp5ClientOutboundTransformer.java` | 5.x Transformer |
| MODIFY | `gray-trace-agent/pom.xml` | 新增 2 个 `provided` 编译依赖 + 测试依赖 |
| MODIFY | `gray-trace-agent/src/main/java/io/tracemark/agent/AgentConfigLoader.java` | 读取 `gray.trace.apache-http-client.enabled` |
| MODIFY | `gray-trace-agent/src/main/java/io/tracemark/agent/GrayTraceAgent.java` | 注册 4.x / 5.x Transformer |
| CREATE | `gray-trace-agent/src/test/java/io/tracemark/agent/advice/ApacheHttpClientOutboundAdviceTest.java` | 4.x Advice 单元测试 |
| CREATE | `gray-trace-agent/src/test/java/io/tracemark/agent/advice/ApacheHttp5ClientOutboundAdviceTest.java` | 5.x Advice 单元测试 |

---

## Task 1：添加依赖并验证编译

**Files:**
- Modify: `gray-trace-agent/pom.xml`

- [x] **Step 1：在 `gray-trace-agent/pom.xml` 的 `<dependencies>` 末尾插入以下内容**

在 `</dependencies>` 之前、rocketmq-client 之后插入：

```xml
        <!-- 编译期依赖：4.x / 5.x Advice 类需要引用目标框架类型，provided 不打进 fat JAR -->
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
            <version>4.5.14</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.httpcomponents.client5</groupId>
            <artifactId>httpclient5</artifactId>
            <version>5.3.1</version>
            <scope>provided</scope>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
```

在 `<build><plugins>` 中，紧接 maven-shade-plugin 后追加（让 surefire 识别 JUnit 5）：

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.22.2</version>
            </plugin>
```

- [x] **Step 2：验证依赖解析**

```bash
cd gray-trace-agent && mvn dependency:resolve -q
```

期望：BUILD SUCCESS，无 MISSING 报告。

---

## Task 2：实现 4.x Advice（含测试）

**Files:**
- Create: `gray-trace-agent/src/main/java/io/tracemark/agent/advice/ApacheHttpClientOutboundAdvice.java`
- Create: `gray-trace-agent/src/test/java/io/tracemark/agent/advice/ApacheHttpClientOutboundAdviceTest.java`

- [x] **Step 1：先写测试（目前会编译失败，因为 Advice 类不存在）**

新建目录 `gray-trace-agent/src/test/java/io/tracemark/agent/advice/`，创建文件：

```java
package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHttpRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ApacheHttpClientOutboundAdviceTest {

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    // ── 场景：有灰度标时注入 ──────────────────────────────────────

    @Test
    void onEnter_withGrayTag_shouldInjectHeader() {
        GrayContext.set("gray-v1");
        BasicHttpRequest request = new BasicHttpRequest("GET", "/api/test");

        ApacheHttpClientOutboundAdvice.onEnter(new Object[]{request});

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG)).isNotNull();
        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG).getValue())
                .isEqualTo("gray-v1");
    }

    // ── 场景：stable 标不注入 ─────────────────────────────────────

    @Test
    void onEnter_withStableTag_shouldNotInjectHeader() {
        // GrayContext 默认值为 "stable"，isGray() == false
        BasicHttpRequest request = new BasicHttpRequest("GET", "/api/test");

        ApacheHttpClientOutboundAdvice.onEnter(new Object[]{request});

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG)).isNull();
    }

    // ── 场景：Header 已存在时不覆盖 ──────────────────────────────

    @Test
    void onEnter_withExistingHeader_shouldNotOverwrite() {
        GrayContext.set("gray-v2");
        BasicHttpRequest request = new BasicHttpRequest("GET", "/api/test");
        request.setHeader(GrayConstants.HEADER_GRAY_TAG, "gray-v1");

        ApacheHttpClientOutboundAdvice.onEnter(new Object[]{request});

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG).getValue())
                .isEqualTo("gray-v1");  // 原值保持
    }

    // ── 场景：execute(HttpHost, HttpRequest) 重载 ─────────────────

    @Test
    void onEnter_withHostPlusRequestArgs_shouldInjectOnRequest() {
        GrayContext.set("gray-v1");
        BasicHttpRequest request = new BasicHttpRequest("GET", "/api/test");

        // 模拟 execute(HttpHost target, HttpRequest request) 签名
        ApacheHttpClientOutboundAdvice.onEnter(new Object[]{
                new HttpHost("example.com"),
                request
        });

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG).getValue())
                .isEqualTo("gray-v1");
    }

    // ── 场景：null / 无 HttpRequest 参数时静默跳过 ────────────────

    @Test
    void onEnter_withNullArgs_shouldNotThrow() {
        assertThatCode(() -> ApacheHttpClientOutboundAdvice.onEnter(null))
                .doesNotThrowAnyException();
    }

    @Test
    void onEnter_withNoHttpRequestInArgs_shouldNotThrow() {
        GrayContext.set("gray-v1");
        assertThatCode(() -> ApacheHttpClientOutboundAdvice.onEnter(new Object[]{"not-a-request", 42}))
                .doesNotThrowAnyException();
    }
}
```

- [x] **Step 2：运行测试，确认编译失败（类不存在）**

```bash
cd gray-trace-agent && mvn test -pl . -q 2>&1 | head -20
```

期望：COMPILATION ERROR，提示 `ApacheHttpClientOutboundAdvice` 不存在。

- [x] **Step 3：实现 Advice 类**

```java
package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;
import org.apache.http.HttpRequest;

/**
 * Advice：拦截 Apache HttpClient 4.x CloseableHttpClient#execute，注入 x-gray-tag Header
 *
 * <p>{@code execute} 有多个重载签名（首参可能是 HttpUriRequest 或 HttpHost），
 * 使用 {@code @Advice.AllArguments} 遍历参数，以 {@code instanceof HttpRequest} 定位请求对象，
 * 兼容全部重载，失败静默跳过，不抛异常。
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
                    request.setHeader(GrayConstants.HEADER_GRAY_TAG, GrayContext.get());
                }
                return;
            }
        }
    }
}
```

- [x] **Step 4：运行测试，确认全部通过**

```bash
cd gray-trace-agent && mvn test -Dtest=ApacheHttpClientOutboundAdviceTest -pl . -q
```

期望：`Tests run: 6, Failures: 0, Errors: 0`

- [x] **Step 5：Commit**

```bash
git add gray-trace-agent/pom.xml \
        gray-trace-agent/src/main/java/io/tracemark/agent/advice/ApacheHttpClientOutboundAdvice.java \
        gray-trace-agent/src/test/java/io/tracemark/agent/advice/ApacheHttpClientOutboundAdviceTest.java
git commit -m "feat(agent): add Apache HttpClient 4.x outbound gray-tag advice"
```

---

## Task 3：实现 4.x Transformer

**Files:**
- Create: `gray-trace-agent/src/main/java/io/tracemark/agent/transformer/ApacheHttpClientOutboundTransformer.java`

- [x] **Step 1：创建 Transformer 类**

```java
package io.tracemark.agent.transformer;

import io.tracemark.agent.advice.ApacheHttpClientOutboundAdvice;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;

/**
 * Transformer：将 {@link ApacheHttpClientOutboundAdvice} 绑定到
 * {@code org.apache.http.impl.client.CloseableHttpClient} 的所有 {@code execute} 重载。
 */
public class ApacheHttpClientOutboundTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                             TypeDescription typeDescription,
                                             ClassLoader classLoader,
                                             JavaModule module,
                                             ProtectionDomain protectionDomain) {
        return builder.visit(
                Advice.to(ApacheHttpClientOutboundAdvice.class)
                        .on(ElementMatchers.named("execute")));
    }
}
```

- [x] **Step 2：确认模块编译通过**

```bash
cd gray-trace-agent && mvn compile -q
```

期望：BUILD SUCCESS。

- [x] **Step 3：Commit**

```bash
git add gray-trace-agent/src/main/java/io/tracemark/agent/transformer/ApacheHttpClientOutboundTransformer.java
git commit -m "feat(agent): add Apache HttpClient 4.x outbound transformer"
```

---

## Task 4：实现 5.x Advice（含测试）

**Files:**
- Create: `gray-trace-agent/src/main/java/io/tracemark/agent/advice/ApacheHttp5ClientOutboundAdvice.java`
- Create: `gray-trace-agent/src/test/java/io/tracemark/agent/advice/ApacheHttp5ClientOutboundAdviceTest.java`

- [x] **Step 1：先写测试**

```java
package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ApacheHttp5ClientOutboundAdviceTest {

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Test
    void onEnter_withGrayTag_shouldInjectHeader() {
        GrayContext.set("gray-v1");
        HttpGet request = new HttpGet("/api/test");

        ApacheHttp5ClientOutboundAdvice.onEnter(new Object[]{request});

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG)).isNotNull();
        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG).getValue())
                .isEqualTo("gray-v1");
    }

    @Test
    void onEnter_withStableTag_shouldNotInjectHeader() {
        HttpGet request = new HttpGet("/api/test");

        ApacheHttp5ClientOutboundAdvice.onEnter(new Object[]{request});

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG)).isNull();
    }

    @Test
    void onEnter_withExistingHeader_shouldNotOverwrite() {
        GrayContext.set("gray-v2");
        HttpGet request = new HttpGet("/api/test");
        request.setHeader(GrayConstants.HEADER_GRAY_TAG, "gray-v1");

        ApacheHttp5ClientOutboundAdvice.onEnter(new Object[]{request});

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG).getValue())
                .isEqualTo("gray-v1");
    }

    @Test
    void onEnter_withHostPlusRequestArgs_shouldInjectOnRequest() {
        GrayContext.set("gray-v1");
        HttpGet request = new HttpGet("/api/test");

        ApacheHttp5ClientOutboundAdvice.onEnter(new Object[]{
                new HttpHost("example.com", 80),
                request
        });

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG).getValue())
                .isEqualTo("gray-v1");
    }

    @Test
    void onEnter_withNullArgs_shouldNotThrow() {
        assertThatCode(() -> ApacheHttp5ClientOutboundAdvice.onEnter(null))
                .doesNotThrowAnyException();
    }
}
```

- [x] **Step 2：运行测试，确认编译失败**

```bash
cd gray-trace-agent && mvn test -Dtest=ApacheHttp5ClientOutboundAdviceTest -pl . -q 2>&1 | head -10
```

期望：COMPILATION ERROR。

- [x] **Step 3：实现 5.x Advice**

```java
package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;
import org.apache.hc.core5.http.HttpRequest;

/**
 * Advice：拦截 Apache HttpClient 5.x CloseableHttpClient#execute，注入 x-gray-tag Header
 *
 * <p>5.x 包路径为 {@code org.apache.hc.core5.http.HttpRequest}（与 4.x 不同），
 * 其余逻辑与 {@link ApacheHttpClientOutboundAdvice} 完全一致。
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
                    request.setHeader(GrayConstants.HEADER_GRAY_TAG, GrayContext.get());
                }
                return;
            }
        }
    }
}
```

- [x] **Step 4：运行测试，确认全部通过**

```bash
cd gray-trace-agent && mvn test -Dtest=ApacheHttp5ClientOutboundAdviceTest -pl . -q
```

期望：`Tests run: 5, Failures: 0, Errors: 0`

- [x] **Step 5：Commit**

```bash
git add gray-trace-agent/src/main/java/io/tracemark/agent/advice/ApacheHttp5ClientOutboundAdvice.java \
        gray-trace-agent/src/test/java/io/tracemark/agent/advice/ApacheHttp5ClientOutboundAdviceTest.java
git commit -m "feat(agent): add Apache HttpClient 5.x outbound gray-tag advice"
```

---

## Task 5：实现 5.x Transformer

**Files:**
- Create: `gray-trace-agent/src/main/java/io/tracemark/agent/transformer/ApacheHttp5ClientOutboundTransformer.java`

- [x] **Step 1：创建 Transformer 类**

```java
package io.tracemark.agent.transformer;

import io.tracemark.agent.advice.ApacheHttp5ClientOutboundAdvice;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;

/**
 * Transformer：将 {@link ApacheHttp5ClientOutboundAdvice} 绑定到
 * {@code org.apache.hc.client5.http.impl.classic.CloseableHttpClient} 的所有 {@code execute} 重载。
 */
public class ApacheHttp5ClientOutboundTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                             TypeDescription typeDescription,
                                             ClassLoader classLoader,
                                             JavaModule module,
                                             ProtectionDomain protectionDomain) {
        return builder.visit(
                Advice.to(ApacheHttp5ClientOutboundAdvice.class)
                        .on(ElementMatchers.named("execute")));
    }
}
```

- [x] **Step 2：确认编译**

```bash
cd gray-trace-agent && mvn compile -q
```

期望：BUILD SUCCESS。

- [x] **Step 3：Commit**

```bash
git add gray-trace-agent/src/main/java/io/tracemark/agent/transformer/ApacheHttp5ClientOutboundTransformer.java
git commit -m "feat(agent): add Apache HttpClient 5.x outbound transformer"
```

---

## Task 6：接入 AgentConfigLoader 和 GrayTraceAgent

**Files:**
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/AgentConfigLoader.java`
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/GrayTraceAgent.java`

> **背景：** `AgentConfigLoader` 目前支持的系统属性列表可在文件头注释中看到，需补充 `gray.trace.apache-http-client.enabled`。`GrayTraceAgent.install()` 目前在第 76-109 行注册 5 个 Transformer（Servlet 4.x/5.x、RestTemplate、OkHttp、ThreadPool、RocketMQ），本次在最后追加 Apache HttpClient 两条。

- [x] **Step 1：修改 `AgentConfigLoader.java`**

在 `props.getMq().setConsumer(...)` 这一行之后、`return props;` 之前插入：

```java
        props.getApacheHttpClient().setEnabled(
                getBool("gray.trace.apache-http-client.enabled", true));
```

同时更新类头注释，追加一行（保持对齐格式）：

```
 * -Dgray.trace.apache-http-client.enabled=true
```

- [x] **Step 2：修改 `GrayTraceAgent.java`**

在 `builder.installOn(inst);` 之前，紧接 RocketMQ 分支之后插入：

```java
        // 6. Apache HttpClient 4.x 出口
        if (config.getApacheHttpClient().isEnabled()) {
            builder = builder.type(ElementMatchers.named(
                            "org.apache.http.impl.client.CloseableHttpClient"))
                    .transform(new ApacheHttpClientOutboundTransformer());

            // 7. Apache HttpClient 5.x 出口
            builder = builder.type(ElementMatchers.named(
                            "org.apache.hc.client5.http.impl.classic.CloseableHttpClient"))
                    .transform(new ApacheHttp5ClientOutboundTransformer());
        }
```

同时更新类头注释的插桩目标列表，追加两条：

```
 *   <li>Apache HttpClient 4.x CloseableHttpClient：出口注入 Header</li>
 *   <li>Apache HttpClient 5.x CloseableHttpClient：出口注入 Header</li>
```

- [x] **Step 3：确认全量编译通过**

```bash
cd gray-trace-agent && mvn compile -q
```

期望：BUILD SUCCESS。

- [x] **Step 4：运行全部 Agent 单元测试**

```bash
cd gray-trace-agent && mvn test -q
```

期望：所有测试通过（含 Task 2 / Task 4 创建的测试），无 FAIL / ERROR。

- [x] **Step 5：Commit**

```bash
git add gray-trace-agent/src/main/java/io/tracemark/agent/AgentConfigLoader.java \
        gray-trace-agent/src/main/java/io/tracemark/agent/GrayTraceAgent.java
git commit -m "feat(agent): register Apache HttpClient 4.x/5.x transformers in GrayTraceAgent"
```

---

## Task 7：全量构建验证

- [x] **Step 1：从根目录执行全量构建**

```bash
mvn clean package -q
```

期望：BUILD SUCCESS，所有模块编译、测试通过；`gray-trace-agent/target/gray-trace-agent-1.0.0.jar` 生成（fat-jar）。

- [x] **Step 2：确认 fat-jar 不含 Apache HttpClient 类（provided 未打入）**

```bash
jar -tf gray-trace-agent/target/gray-trace-agent-1.0.0.jar | grep "org/apache/http" | head -5
```

期望：**无输出**（`provided` 依赖不应打进 fat-jar）。

- [x] **Step 3：确认 Advice 类已打入 fat-jar**

```bash
jar -tf gray-trace-agent/target/gray-trace-agent-1.0.0.jar | grep "ApacheHttp"
```

期望：输出类似：

```
io/tracemark/agent/advice/ApacheHttpClientOutboundAdvice.class
io/tracemark/agent/advice/ApacheHttp5ClientOutboundAdvice.class
io/tracemark/agent/transformer/ApacheHttpClientOutboundTransformer.class
io/tracemark/agent/transformer/ApacheHttp5ClientOutboundTransformer.class
```

- [x] **Step 4：Final commit**

```bash
git add -A
git status   # 确认没有遗漏文件
git commit -m "feat(agent): complete Apache HttpClient 4.x/5.x outbound gray-tag propagation

- ApacheHttpClientOutboundAdvice + Transformer (4.x)
- ApacheHttp5ClientOutboundAdvice + Transformer (5.x)
- AgentConfigLoader: gray.trace.apache-http-client.enabled
- GrayTraceAgent: register new transformers
- Unit tests covering gray/stable/no-overwrite/multi-arg scenarios"
```

---

## 自检

**Spec 覆盖确认：**

| Spec 场景 | 覆盖 Task |
|-----------|-----------|
| 有灰度标时注入 x-gray-tag | Task 2 Step 1（4.x）、Task 4 Step 1（5.x） |
| stable 标不注入 | Task 2 Step 1、Task 4 Step 1 |
| Header 已存在不覆盖 | Task 2 Step 1、Task 4 Step 1 |
| 配置开关关闭跳过插桩 | Task 6 Step 1-2（条件分支） |
| 依赖不存在时静默跳过 | ByteBuddy onError 已有全局日志（Task 7 Step 2 间接验证） |
| execute(HttpHost, HttpRequest) 重载兼容 | Task 2 Step 1（4.x hostPlusRequest 测试）、Task 4 Step 1（5.x） |
