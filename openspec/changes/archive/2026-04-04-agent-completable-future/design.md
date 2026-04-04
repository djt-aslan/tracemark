## Context

当前 Agent 模式已支持 ThreadPoolExecutor 的 TTL 包装（通过 `ThreadPoolTransformer`），但 `CompletableFuture` 的异步方法使用 `ForkJoinPool.commonPool()` 或传入的 Executor，未经过 TTL 包装，导致灰度上下文丢失。

Starter 模式通过 `GrayAutoConfiguration` 注册 `TtlCompletableFuture` 包装，但 Agent 模式无法使用 Spring 机制，需通过字节码插桩实现。

**约束：**
- 目标 JDK 8+（CompletableFuture 从 JDK 8 引入）
- 已有 TTL 依赖（`transmittable-thread-local`）
- 不能破坏 CompletableFuture 的原有语义

## Goals / Non-Goals

**Goals:**
- 拦截 `CompletableFuture.supplyAsync` / `runAsync` / `thenApplyAsync` / `thenAcceptAsync` / `thenRunAsync` 等异步方法
- 将传入的 Runnable/Callable/Function 用 TTL 包装后再执行
- 保证灰度上下文在异步线程间正确传递
- 支持配置开关控制启用/禁用

**Non-Goals:**
- 不处理同步方法（`thenApply` / `thenAccept` 等，它们在同一线程执行）
- 不处理用户已手动 TTL 包装的情况
- 不修改 `ForkJoinPool` 本身

## Decisions

### Decision 1：插桩目标选择

**选择：** 拦截 `CompletableFuture` 的静态方法和实例方法

**理由：**
- `supplyAsync(Supplier)` / `runAsync(Runnable)` 使用 `ForkJoinPool.commonPool()`
- `supplyAsync(Supplier, Executor)` / `runAsync(Runnable, Executor)` 使用传入 Executor
- `thenApplyAsync(Function)` / `thenAcceptAsync(Consumer)` 等实例方法同样需要处理

**替代方案：**
- 包装 `ForkJoinPool.commonPool()`：影响面太大，可能影响其他 ForkJoinTask
- 仅拦截静态方法：无法覆盖 `thenApplyAsync` 等实例方法

### Decision 2：TTL 包装策略

**选择：** 在方法入口拦截，用 `TtlWrappers.wrapRunnable()` / `wrapCallable()` 包装参数

**理由：**
- TTL 提供标准的 `TtlWrappers` 工具类
- 在 Advice 中直接包装参数，无需修改 CompletableFuture 内部

**实现示例：**
```java
@Advice.OnMethodEnter
public static void onEnter(@Advice.Argument(value = 0, readOnly = false) Supplier<?> supplier) {
    supplier = TtlWrappers.wrapSupplier(supplier);
}
```

### Decision 3：配置项设计

**选择：** 新增 `gray.trace.completable-future.enabled`，默认 `true`

**理由：**
- 与其他拦截器配置保持一致
- 默认启用，用户无需额外配置即可获得能力

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| TTL 包装引入额外对象分配 | 性能影响可忽略（TTL 包装仅创建薄包装对象） |
| 与用户手动 TTL 包装冲突 | TTL 的 `unwrap` 机制可处理重复包装 |
| JDK 版本兼容性 | 仅在 JDK 8+ 环境启用插桩 |
