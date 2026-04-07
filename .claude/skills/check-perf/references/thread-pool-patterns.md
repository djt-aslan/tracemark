# 线程池风险模式库

本文档定义线程池相关的性能风险检测规则。

---

## 1. 线程池创建风险

### 1.1 使用 Executors 工厂方法

**风险等级：** 🔴 高危

**模式：**
```java
// 危险：可能 OOM
ExecutorService executor = Executors.newFixedThreadPool(10);
ExecutorService executor = Executors.newCachedThreadPool();
ExecutorService executor = Executors.newSingleThreadExecutor();
ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
```

**问题：**
- `newFixedThreadPool` / `newSingleThreadExecutor`：使用无界队列 `LinkedBlockingQueue`，任务堆积时可能 OOM
- `newCachedThreadPool`：线程数无上限，高并发时可能创建大量线程导致 OOM
- `newScheduledThreadPool`：同样使用无界队列

**修复：**
```java
// 推荐：使用 ThreadPoolExecutor 并明确配置
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10,                          // corePoolSize
    20,                          // maximumPoolSize
    60L, TimeUnit.SECONDS,       // keepAliveTime
    new LinkedBlockingQueue<>(1000),  // 有界队列
    new ThreadFactoryBuilder().setNameFormat("worker-%d").build(),
    new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
);
```

### 1.2 线程池未正确关闭

**风险等级：** 🔴 高危

**模式：**
```java
// 危险：线程池未关闭
public class MyService {
    private ExecutorService executor = Executors.newFixedThreadPool(10);

    public void process() {
        executor.submit(() -> { ... });
    }
    // 缺少 shutdown
}
```

**修复：**
```java
public class MyService implements DisposableBean {
    private ExecutorService executor = new ThreadPoolExecutor(...);

    @Override
    public void destroy() {
        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }
}
```

---

## 2. 拒绝策略风险

### 2.1 默认拒绝策略不当

**风险等级：** 🟡 中危

**模式：**
```java
// 默认使用 AbortPolicy，直接抛异常
ThreadPoolExecutor executor = new ThreadPoolExecutor(...);
// 任务被拒绝时抛 RejectedExecutionException
```

**问题：** 生产环境可能因任务拒绝导致业务中断

**修复：**
```java
// 根据业务场景选择合适的拒绝策略
new ThreadPoolExecutor.CallerRunsPolicy()   // 调用者执行，起到削峰作用
new ThreadPoolExecutor.DiscardOldestPolicy() // 丢弃最老任务
new ThreadPoolExecutor.DiscardPolicy()       // 静默丢弃

// 或自定义策略
class LogAndStorePolicy implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        log.warn("Task rejected, queue size: {}", e.getQueue().size());
        // 存储到持久化队列，后续重试
        taskStore.save(r);
    }
}
```

---

## 3. 队列配置风险

### 3.1 无界队列

**风险等级：** 🔴 高危

**模式：**
```java
// 危险：无界队列
new LinkedBlockingQueue<>()  // 默认 Integer.MAX_VALUE
new LinkedBlockingQueue<>()  // 无容量限制
```

**修复：**
```java
// 有界队列
new LinkedBlockingQueue<>(1000)
new ArrayBlockingQueue<>(1000)
```

### 3.2 队列容量与线程数不匹配

**风险等级：** 🟡 中危

**检测逻辑：**
- 队列容量过小 + 线程数过少 = 频繁拒绝
- 队列容量过大 + 任务处理慢 = 响应延迟高

**建议配置：**
```
CPU 密集型：线程数 = CPU 核心数 + 1
IO 密集型：线程数 = CPU 核心数 * (1 + 等待时间/计算时间)
队列容量 = 峰值 QPS * 平均处理时间 * 安全系数
```

---

## 4. 线程池监控缺失

**风险等级：** 🟢 低危

**模式：**
```java
// 缺少监控
ThreadPoolExecutor executor = new ThreadPoolExecutor(...);
// 无法知道线程池状态
```

**修复：**
```java
// 添加监控
@Bean
public ThreadPoolExecutor myExecutor(MeterRegistry registry) {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(...);

    // Micrometer 监控
    registry.gauge("thread.pool.active", executor, ThreadPoolExecutor::getActiveCount);
    registry.gauge("thread.pool.queue.size", executor, e -> e.getQueue().size());
    registry.gauge("thread.pool.completed", executor, ThreadPoolExecutor::getCompletedTaskCount);

    return executor;
}
```

---

## 5. Spring 线程池配置

### 5.1 @Async 线程池未配置

**风险等级：** 🟡 中危

**模式：**
```java
@Async
public void asyncMethod() { ... }
```

**问题：** 使用默认 `SimpleAsyncTaskExecutor`，每次创建新线程

**修复：**
```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

---

## 检测正则表达式

```regex
# Executors 使用
Executors\.(newFixedThreadPool|newCachedThreadPool|newSingleThreadExecutor|newScheduledThreadPool)

# 无界队列
new LinkedBlockingQueue<>\(\)

# 线程池未关闭（类中有 ExecutorService 字段但无 shutdown）
private\s+(final\s+)?ExecutorService\s+\w+

# @Async 无自定义线程池
@Async\s*\n\s*public\s+\w+\s+\w+\s*\(
```
