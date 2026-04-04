## 1. 配置支持

- [x] 1.1 在 `GrayProperties.java` 新增 `CompletableFuture` 嵌套配置类，包含 `enabled` 属性（默认 true）
- [x] 1.2 在 `AgentConfigLoader.java` 新增读取 `gray.trace.completable-future.enabled` 配置项

## 2. Advice 实现

- [x] 2.1 新建 `CompletableFutureAsyncAdvice.java`：拦截 `CompletableFuture.supplyAsync` 静态方法，使用 TTL 包装 Supplier 参数
- [x] 2.2 拦截 `CompletableFuture.runAsync` 静态方法，使用 TTL 包装 Runnable 参数
- [x] 2.3 拦截 `thenApplyAsync` / `thenAcceptAsync` / `thenRunAsync` 实例方法，使用 TTL 包装 Function/Consumer/Runnable 参数
- [x] 2.4 处理带 Executor 参数的重载方法

## 3. Transformer 实现

- [x] 3.1 新建 `CompletableFutureAsyncTransformer.java`：将 Advice 绑定到 `java.util.concurrent.CompletableFuture` 类

## 4. Agent 注册

- [x] 4.1 在 `GrayTraceAgent.java` 新增 CompletableFuture Transformer 注册，读取 `config.getCompletableFuture().isEnabled()` 条件

## 5. 单元测试

- [x] 5.1 新建 `CompletableFutureAsyncAdviceTest.java`：测试 supplyAsync 灰度标传递
- [x] 5.2 测试 runAsync 灰度标传递
- [x] 5.3 测试 thenApplyAsync 灰度标传递
- [x] 5.4 测试指定 Executor 时灰度标传递
- [x] 5.5 测试无灰度标时不注入
- [x] 5.6 测试返回值和异常正确传递

## 6. 验证

- [x] 6.1 运行全量单元测试，确认所有测试通过
- [x] 6.2 执行 `mvn clean package` 构建 fat-jar
