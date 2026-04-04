## Why

Agent 模式下 `CompletableFuture` 异步编排场景缺乏灰度上下文传递能力。当业务使用 `CompletableFuture.supplyAsync()` 或 `thenApplyAsync()` 等异步方法时，灰度标在线程切换后丢失，导致下游调用无法携带 `x-gray-tag`。

Starter 模式已通过 `TtlCompletableFuture` 包装解决此问题，Agent 模式需补齐同等能力。

## What Changes

- Agent 模式新增 `CompletableFuture` 相关方法的字节码插桩
- 拦截 `CompletableFuture.supplyAsync` / `runAsync` / `thenApplyAsync` 等异步方法
- 使用 TTL（TransmittableThreadLocal）包装 Runnable/Callable，保证灰度上下文跨线程传递
- 新增配置项 `gray.trace.completable-future.enabled`（默认 true）

## Capabilities

### New Capabilities

- `completable-future-async`: Agent 模式下 CompletableFuture 异步方法的灰度上下文传递

### Modified Capabilities

- 无（此为新增能力，不修改现有 spec）

## Impact

- **新增文件**：
  - `CompletableFutureAsyncAdvice.java`：拦截 CompletableFuture 静态方法
  - `CompletableFutureAsyncTransformer.java`：Transformer 绑定
  - `CompletableFutureAsyncAdviceTest.java`：单元测试
- **修改文件**：
  - `GrayTraceAgent.java`：注册 Transformer
  - `AgentConfigLoader.java`：读取配置项
  - `GrayProperties.java`：新增 CompletableFuture 配置嵌套类
- **依赖**：已有 `transmittable-thread-local`（TTL），无需新增依赖
