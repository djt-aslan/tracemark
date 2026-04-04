## ADDED Requirements

### Requirement: Agent 模式下 CompletableFuture 异步方法灰度上下文传递
Java Agent 安装后，`CompletableFuture` 的异步方法（`supplyAsync`、`runAsync`、`thenApplyAsync`、`thenAcceptAsync`、`thenRunAsync` 等）SHALL 在任务执行前捕获当前线程的 `GrayContext`，并在异步线程中恢复，保证灰度标正确传递。

#### Scenario: supplyAsync 异步执行时灰度标传递
- **WHEN** 当前线程 `GrayContext` 包含灰度标 `gray-v1`
- **AND** 调用 `CompletableFuture.supplyAsync(() -> {...})`
- **THEN** 异步任务执行时 `GrayContext.get()` SHALL 返回 `gray-v1`

#### Scenario: runAsync 异步执行时灰度标传递
- **WHEN** 当前线程 `GrayContext` 包含灰度标 `gray-v1`
- **AND** 调用 `CompletableFuture.runAsync(() -> {...})`
- **THEN** 异步任务执行时 `GrayContext.get()` SHALL 返回 `gray-v1`

#### Scenario: thenApplyAsync 链式调用时灰度标传递
- **WHEN** 当前线程 `GrayContext` 包含灰度标 `gray-v1`
- **AND** 调用 `future.thenApplyAsync(x -> {...})`
- **THEN** 异步任务执行时 `GrayContext.get()` SHALL 返回 `gray-v1`

#### Scenario: 指定 Executor 时灰度标传递
- **WHEN** 当前线程 `GrayContext` 包含灰度标 `gray-v1`
- **AND** 调用 `CompletableFuture.supplyAsync(supplier, executor)`
- **THEN** 异步任务在 executor 的线程中执行时 `GrayContext.get()` SHALL 返回 `gray-v1`

#### Scenario: 无灰度标时不注入
- **WHEN** 当前线程 `GrayContext.get()` 返回 `stable`
- **AND** 调用任意 CompletableFuture 异步方法
- **THEN** 异步任务执行时 `GrayContext.get()` SHALL 返回 `stable`

### Requirement: CompletableFuture 配置开关支持
系统 SHALL 通过 `gray.trace.completable-future.enabled` 属性提供对 CompletableFuture 异步传递功能的独立开关控制，默认值为 `true`。

#### Scenario: 默认启用
- **WHEN** 未显式设置 `gray.trace.completable-future.enabled`
- **THEN** CompletableFuture 异步传递功能 SHALL 默认启用

#### Scenario: 显式禁用
- **WHEN** 设置 `gray.trace.completable-future.enabled=false`
- **THEN** Agent SHALL 跳过对 CompletableFuture 的字节码插桩

### Requirement: CompletableFuture 拦截不影响原有语义
Agent 插桩后，CompletableFuture 的返回值、异常处理、链式调用语义 SHALL 保持不变。

#### Scenario: 返回值正确传递
- **WHEN** 调用 `CompletableFuture.supplyAsync(() -> "result")`
- **THEN** `future.get()` SHALL 返回 `"result"`

#### Scenario: 异常正确传递
- **WHEN** 异步任务抛出 `RuntimeException`
- **THEN** `future.get()` SHALL 抛出 `ExecutionException` 包装原异常