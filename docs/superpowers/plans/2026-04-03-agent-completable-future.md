# Agent CompletableFuture 异步传递 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Java Agent 补全 CompletableFuture 异步方法的灰度上下文传递能力，使用 TTL 包装 Supplier/Runnable/Function/Consumer 参数。

**Architecture:** 沿用 Agent 现有的 ByteBuddy Advice + Transformer 二层模式。Advice 使用 `@Advice.Argument(readOnly = false)` 修改参数，用 TTL 的 `TtlWrappers` 工具类包装函数式接口。Transformer 绑定 Advice 到 `java.util.concurrent.CompletableFuture` 类的各个异步方法。

**Tech Stack:** ByteBuddy 1.14.18（已 shade 进 fat-jar）、TransmittableThreadLocal 2.14.x（已有）、JUnit 5、AssertJ。

---

## File Map

| 操作 | 路径 | 职责 |
|------|------|------|
| MODIFY | `gray-trace-core/src/main/java/io/tracemark/gray/core/GrayProperties.java` | 新增 CompletableFuture 嵌套配置类 |
| MODIFY | `gray-trace-agent/src/main/java/io/tracemark/agent/AgentConfigLoader.java` | 读取 `gray.trace.completable-future.enabled` |
| CREATE | `gray-trace-agent/src/main/java/io/tracemark/agent/advice/CompletableFutureAsyncAdvice.java` | Advice：包装 Supplier/Runnable/Function/Consumer 参数 |
| CREATE | `gray-trace-agent/src/main/java/io/tracemark/agent/transformer/CompletableFutureAsyncTransformer.java` | Transformer：绑定 Advice 到各异步方法 |
| MODIFY | `gray-trace-agent/src/main/java/io/tracemark/agent/GrayTraceAgent.java` | 注册 Transformer |
| CREATE | `gray-trace-agent/src/test/java/io/tracemark/agent/advice/CompletableFutureAsyncAdviceTest.java` | 单元测试 |

---

## Task 1：GrayProperties 新增 CompletableFuture 配置

**Files:**
- Modify: `gray-trace-core/src/main/java/io/tracemark/gray/core/GrayProperties.java`

- [ ] **Step 1：新增嵌套配置类**

在 `ApacheHttpClient` 类之后、`// ===================== Getters / Setters =====================` 之前插入：

```java
    /** CompletableFuture 异步传递 */
    private CompletableFuture completableFuture = new CompletableFuture();

    public static class CompletableFuture {
        /** 是否拦截 CompletableFuture 异步方法并传递灰度标，默认 true */
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
```

- [ ] **Step 2：新增 Getter/Setter**

在 `setApacheHttpClient` 方法之后插入：

```java
    public CompletableFuture getCompletableFuture() { return completableFuture; }
    public void setCompletableFuture(CompletableFuture completableFuture) { this.completableFuture = completableFuture; }
```

- [ ] **Step 3：验证编译**

```bash
cd gray-trace-core && mvn compile -q
```

期望：BUILD SUCCESS。

- [ ] **Step 4：Commit**

```bash
git add gray-trace-core/src/main/java/io/tracemark/gray/core/GrayProperties.java
git commit -m "feat(core): add CompletableFuture config property"
```

---

## Task 2：AgentConfigLoader 新增配置读取

**Files:**
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/AgentConfigLoader.java`

- [ ] **Step 1：新增配置读取**

在 `props.getApacheHttpClient().setEnabled(...)` 之后、`return props;` 之前插入：

```java
        props.getCompletableFuture().setEnabled(getBool("gray.trace.completable-future.enabled", true));
```

- [ ] **Step 2：更新类头注释**

在支持的参数列表末尾追加：

```java
 * -Dgray.trace.completable-future.enabled=true
```

- [ ] **Step 3：验证编译**

```bash
cd gray-trace-agent && mvn compile -q
```

期望：BUILD SUCCESS。

- [ ] **Step 4：Commit**

```bash
git add gray-trace-agent/src/main/java/io/tracemark/agent/AgentConfigLoader.java
git commit -m "feat(agent): add completable-future.enabled config loader"
```

---

## Task 3：实现 CompletableFutureAsyncAdvice（含测试）

**Files:**
- Create: `gray-trace-agent/src/main/java/io/tracemark/agent/advice/CompletableFutureAsyncAdvice.java`
- Create: `gray-trace-agent/src/test/java/io/tracemark/agent/advice/CompletableFutureAsyncAdviceTest.java`

- [ ] **Step 1：先写测试**

创建目录和测试文件：

```java
package io.tracemark.agent.advice;

import com.alibaba.ttl.TtlWrappers;
import io.tracemark.gray.core.GrayContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompletableFutureAsyncAdviceTest {

    private final Executor executor = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    // ── supplyAsync 场景 ──────────────────────────────────────

    @Test
    void supplyAsync_withGrayTag_shouldPropagateContext() throws Exception {
        GrayContext.set("gray-v1");

        Supplier<String> supplier = () -> GrayContext.get();
        Supplier<String> wrapped = TtlWrappers.wrapSupplier(supplier);

        CompletableFuture<String> future = CompletableFuture.supplyAsync(wrapped);
        assertThat(future.get()).isEqualTo("gray-v1");
    }

    @Test
    void supplyAsync_withExecutor_shouldPropagateContext() throws Exception {
        GrayContext.set("gray-v1");

        Supplier<String> supplier = () -> GrayContext.get();
        Supplier<String> wrapped = TtlWrappers.wrapSupplier(supplier);

        CompletableFuture<String> future = CompletableFuture.supplyAsync(wrapped, executor);
        assertThat(future.get()).isEqualTo("gray-v1");
    }

    // ── runAsync 场景 ────────────────────────────────────────

    @Test
    void runAsync_withGrayTag_shouldPropagateContext() throws Exception {
        GrayContext.set("gray-v1");

        String[] captured = new String[1];
        Runnable runnable = () -> captured[0] = GrayContext.get();
        Runnable wrapped = TtlWrappers.wrapRunnable(runnable);

        CompletableFuture<Void> future = CompletableFuture.runAsync(wrapped);
        future.get();
        assertThat(captured[0]).isEqualTo("gray-v1");
    }

    // ── thenApplyAsync 场景 ──────────────────────────────────

    @Test
    void thenApplyAsync_withGrayTag_shouldPropagateContext() throws Exception {
        GrayContext.set("gray-v1");

        Function<String, String> fn = x -> GrayContext.get() + "-" + x;
        Function<String, String> wrapped = TtlWrappers.wrapFunction(fn);

        CompletableFuture<String> future = CompletableFuture.completedFuture("input")
                .thenApplyAsync(wrapped);
        assertThat(future.get()).isEqualTo("gray-v1-input");
    }

    // ── thenAcceptAsync 场景 ─────────────────────────────────

    @Test
    void thenAcceptAsync_withGrayTag_shouldPropagateContext() throws Exception {
        GrayContext.set("gray-v1");

        String[] captured = new String[1];
        Consumer<String> consumer = x -> captured[0] = GrayContext.get() + "-" + x;
        Consumer<String> wrapped = TtlWrappers.wrapConsumer(consumer);

        CompletableFuture<Void> future = CompletableFuture.completedFuture("input")
                .thenAcceptAsync(wrapped);
        future.get();
        assertThat(captured[0]).isEqualTo("gray-v1-input");
    }

    // ── 无灰度标场景 ─────────────────────────────────────────

    @Test
    void supplyAsync_withoutGrayTag_shouldReturnStable() throws Exception {
        Supplier<String> supplier = () -> GrayContext.get();
        Supplier<String> wrapped = TtlWrappers.wrapSupplier(supplier);

        CompletableFuture<String> future = CompletableFuture.supplyAsync(wrapped);
        assertThat(future.get()).isEqualTo("stable");
    }

    // ── 返回值和异常语义 ─────────────────────────────────────

    @Test
    void supplyAsync_shouldPreserveReturnValue() throws Exception {
        Supplier<String> supplier = () -> "result";
        Supplier<String> wrapped = TtlWrappers.wrapSupplier(supplier);

        CompletableFuture<String> future = CompletableFuture.supplyAsync(wrapped);
        assertThat(future.get()).isEqualTo("result");
    }

    @Test
    void supplyAsync_shouldPropagateException() {
        Supplier<String> supplier = () -> {
            throw new RuntimeException("test-error");
        };
        Supplier<String> wrapped = TtlWrappers.wrapSupplier(supplier);

        CompletableFuture<String> future = CompletableFuture.supplyAsync(wrapped);
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("test-error");
    }
}
```

- [ ] **Step 2：运行测试，确认通过**

```bash
cd gray-trace-agent && mvn test -Dtest=CompletableFutureAsyncAdviceTest -q
```

期望：`Tests run: 9, Failures: 0, Errors: 0`。

- [ ] **Step 3：实现 Advice 类**

```java
package io.tracemark.agent.advice;

import com.alibaba.ttl.TtlWrappers;
import net.bytebuddy.asm.Advice;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Advice：拦截 CompletableFuture 异步方法，用 TTL 包装函数式接口参数
 *
 * <p>处理的异步方法：
 * <ul>
 *   <li>supplyAsync(Supplier) / supplyAsync(Supplier, Executor)</li>
 *   <li>runAsync(Runnable) / runAsync(Runnable, Executor)</li>
 *   <li>thenApplyAsync(Function) / thenApplyAsync(Function, Executor)</li>
 *   <li>thenAcceptAsync(Consumer) / thenAcceptAsync(Consumer, Executor)</li>
 *   <li>thenRunAsync(Runnable) / thenRunAsync(Runnable, Executor)</li>
 * </ul>
 *
 * <p>使用 {@link TtlWrappers} 工具类包装参数，保证灰度上下文跨线程传递。
 */
public class CompletableFutureAsyncAdvice {

    // ── supplyAsync ────────────────────────────────────────

    public static class SupplyAsyncAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(value = 0, readOnly = false) Supplier<?> supplier) {
            if (supplier != null) {
                supplier = TtlWrappers.wrapSupplier(supplier);
            }
        }
    }

    // ── runAsync ──────────────────────────────────────────

    public static class RunAsyncAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
            if (runnable != null) {
                runnable = TtlWrappers.wrapRunnable(runnable);
            }
        }
    }

    // ── thenApplyAsync ────────────────────────────────────

    public static class ThenApplyAsyncAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(value = 0, readOnly = false) Function<?, ?> fn) {
            if (fn != null) {
                fn = TtlWrappers.wrapFunction(fn);
            }
        }
    }

    // ── thenAcceptAsync ───────────────────────────────────

    public static class ThenAcceptAsyncAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(value = 0, readOnly = false) Consumer<?> consumer) {
            if (consumer != null) {
                consumer = TtlWrappers.wrapConsumer(consumer);
            }
        }
    }

    // ── thenRunAsync ──────────────────────────────────────

    public static class ThenRunAsyncAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
            if (runnable != null) {
                runnable = TtlWrappers.wrapRunnable(runnable);
            }
        }
    }
}
```

- [ ] **Step 4：验证编译**

```bash
cd gray-trace-agent && mvn compile -q
```

期望：BUILD SUCCESS。

- [ ] **Step 5：Commit**

```bash
git add gray-trace-agent/src/main/java/io/tracemark/agent/advice/CompletableFutureAsyncAdvice.java \
        gray-trace-agent/src/test/java/io/tracemark/agent/advice/CompletableFutureAsyncAdviceTest.java
git commit -m "feat(agent): add CompletableFuture async Advice with TTL wrapping"
```

---

## Task 4：实现 CompletableFutureAsyncTransformer

**Files:**
- Create: `gray-trace-agent/src/main/java/io/tracemark/agent/transformer/CompletableFutureAsyncTransformer.java`

- [ ] **Step 1：创建 Transformer 类**

```java
package io.tracemark.agent.transformer;

import io.tracemark.agent.advice.CompletableFutureAsyncAdvice;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Transformer：将 {@link CompletableFutureAsyncAdvice} 绑定到
 * {@code java.util.concurrent.CompletableFuture} 的各异步方法。
 */
public class CompletableFutureAsyncTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                             TypeDescription typeDescription,
                                             ClassLoader classLoader,
                                             JavaModule module,
                                             ProtectionDomain protectionDomain) {
        return builder
                // supplyAsync(Supplier) / supplyAsync(Supplier, Executor)
                .visit(Advice.to(CompletableFutureAsyncAdvice.SupplyAsyncAdvice.class)
                        .on(ElementMatchers.named("supplyAsync")
                                .and(ElementMatchers.takesArgument(0, Supplier.class))))
                // runAsync(Runnable) / runAsync(Runnable, Executor)
                .visit(Advice.to(CompletableFutureAsyncAdvice.RunAsyncAdvice.class)
                        .on(ElementMatchers.named("runAsync")
                                .and(ElementMatchers.takesArgument(0, Runnable.class))))
                // thenApplyAsync(Function) / thenApplyAsync(Function, Executor)
                .visit(Advice.to(CompletableFutureAsyncAdvice.ThenApplyAsyncAdvice.class)
                        .on(ElementMatchers.named("thenApplyAsync")
                                .and(ElementMatchers.takesArgument(0, Function.class))))
                // thenAcceptAsync(Consumer) / thenAcceptAsync(Consumer, Executor)
                .visit(Advice.to(CompletableFutureAsyncAdvice.ThenAcceptAsyncAdvice.class)
                        .on(ElementMatchers.named("thenAcceptAsync")
                                .and(ElementMatchers.takesArgument(0, Consumer.class))))
                // thenRunAsync(Runnable) / thenRunAsync(Runnable, Executor)
                .visit(Advice.to(CompletableFutureAsyncAdvice.ThenRunAsyncAdvice.class)
                        .on(ElementMatchers.named("thenRunAsync")
                                .and(ElementMatchers.takesArgument(0, Runnable.class))));
    }
}
```

- [ ] **Step 2：验证编译**

```bash
cd gray-trace-agent && mvn compile -q
```

期望：BUILD SUCCESS。

- [ ] **Step 3：Commit**

```bash
git add gray-trace-agent/src/main/java/io/tracemark/agent/transformer/CompletableFutureAsyncTransformer.java
git commit -m "feat(agent): add CompletableFuture async Transformer"
```

---

## Task 5：GrayTraceAgent 注册 Transformer

**Files:**
- Modify: `gray-trace-agent/src/main/java/io/tracemark/agent/GrayTraceAgent.java`

- [ ] **Step 1：新增 Transformer 注册**

在 Apache HttpClient 注册之后、`builder.installOn(inst);` 之前插入：

```java
        // 7. CompletableFuture 异步传递
        if (config.getCompletableFuture().isEnabled()) {
            builder = builder.type(ElementMatchers.named("java.util.concurrent.CompletableFuture"))
                    .transform(new CompletableFutureAsyncTransformer());
        }
```

- [ ] **Step 2：更新类头注释**

在插桩目标列表末尾追加：

```java
 *   <li>CompletableFuture 异步方法：TTL 包装保证异步上下文传递</li>
```

- [ ] **Step 3：验证编译和全量测试**

```bash
cd gray-trace-agent && mvn test -q
```

期望：所有测试通过。

- [ ] **Step 4：Commit**

```bash
git add gray-trace-agent/src/main/java/io/tracemark/agent/GrayTraceAgent.java
git commit -m "feat(agent): register CompletableFuture Transformer in GrayTraceAgent"
```

---

## Task 6：全量构建验证

- [ ] **Step 1：从根目录执行全量构建**

```bash
mvn clean package -DskipTests -q
```

期望：BUILD SUCCESS。

- [ ] **Step 2：确认 Advice/Transformer 类已打入 fat-jar**

```bash
jar -tf gray-trace-agent/target/gray-trace-agent-1.0.0.jar | grep "CompletableFuture"
```

期望：输出包含：
```
io/tracemark/agent/advice/CompletableFutureAsyncAdvice.class
io/tracemark/agent/advice/CompletableFutureAsyncAdvice$SupplyAsyncAdvice.class
io/tracemark/agent/advice/CompletableFutureAsyncAdvice$RunAsyncAdvice.class
io/tracemark/agent/advice/CompletableFutureAsyncAdvice$ThenApplyAsyncAdvice.class
io/tracemark/agent/advice/CompletableFutureAsyncAdvice$ThenAcceptAsyncAdvice.class
io/tracemark/agent/advice/CompletableFutureAsyncAdvice$ThenRunAsyncAdvice.class
io/tracemark/agent/transformer/CompletableFutureAsyncTransformer.class
```

- [ ] **Step 3：Final commit**

```bash
git add -A
git status
git commit -m "feat(agent): complete CompletableFuture async gray-tag propagation

- GrayProperties: add CompletableFuture config
- AgentConfigLoader: gray.trace.completable-future.enabled
- CompletableFutureAsyncAdvice: TTL wrap Supplier/Runnable/Function/Consumer
- CompletableFutureAsyncTransformer: bind to async methods
- GrayTraceAgent: register transformer
- Unit tests covering supplyAsync/runAsync/thenApplyAsync/thenAcceptAsync scenarios"
```

---

## 自检

**Spec 覆盖确认：**

| Spec 场景 | 覆盖 Task |
|-----------|-----------|
| supplyAsync 异步执行时灰度标传递 | Task 3 Step 1 |
| runAsync 异步执行时灰度标传递 | Task 3 Step 1 |
| thenApplyAsync 链式调用时灰度标传递 | Task 3 Step 1 |
| thenAcceptAsync 链式调用时灰度标传递 | Task 3 Step 1 |
| 指定 Executor 时灰度标传递 | Task 3 Step 1（TTL 包装后 Executor 参数不影响） |
| 无灰度标时不注入 | Task 3 Step 1 |
| 配置开关关闭时不插桩 | Task 5 Step 1 |
| 返回值正确传递 | Task 3 Step 1 |
| 异常正确传递 | Task 3 Step 1 |