# 同步阻塞风险模式库

本文档定义同步阻塞相关的性能风险检测规则。

---

## 1. 异步上下文中的阻塞调用

### 1.1 @Async 方法中使用阻塞 IO

**风险等级：** 🟡 中危

**模式：**
```java
// 危险：@Async 方法中使用阻塞 HTTP 调用
@Async
public CompletableFuture<Data> fetchData() {
    // 阻塞调用，浪费线程池线程
    String result = restTemplate.getForObject(url, String.class);
    return CompletableFuture.completedFuture(result);
}

// 危险：@Async 方法中 sleep
@Async
public void process() {
    Thread.sleep(5000);  // 阻塞线程池线程
    doWork();
}
```

**问题：** 阻塞线程池线程，降低并发能力

**修复：**
```java
// 方案 1：使用异步客户端
@Async
public CompletableFuture<Data> fetchData() {
    return webClient.get()
        .uri(url)
        .retrieve()
        .bodyToMono(Data.class)
        .toFuture();
}

// 方案 2：使用合理的超时
@Async
public CompletableFuture<Data> fetchData() {
    try {
        String result = restTemplate.getForObject(url, String.class);
        return CompletableFuture.completedFuture(result);
    } catch (ResourceAccessException e) {
        return CompletableFuture.failedFuture(e);
    }
}
```

### 1.2 CompletableFuture.get() 无超时

**风险等级：** 🔴 高危

**模式：**
```java
// 危险：无限等待
CompletableFuture<Data> future = service.fetchAsync();
Data data = future.get();  // 可能永远阻塞

// 危险：join() 无超时
Data data = future.join();  // 可能永远阻塞
```

**修复：**
```java
// 使用超时
CompletableFuture<Data> future = service.fetchAsync();
Data data = future.get(30, TimeUnit.SECONDS);  // 30秒超时

// 或使用 orTimeout (Java 9+)
Data data = future
    .orTimeout(30, TimeUnit.SECONDS)
    .join();

// 或使用 completeOnTimeout
Data data = future
    .completeOnTimeout(defaultValue, 30, TimeUnit.SECONDS)
    .join();
```

---

## 2. HTTP 客户端阻塞

### 2.1 无超时配置

**风险等级：** 🔴 高危

**模式：**
```java
// 危险：RestTemplate 无超时
RestTemplate restTemplate = new RestTemplate();

// 危险：OkHttp 无超时
OkHttpClient client = new OkHttpClient.Builder().build();

// 危险：Apache HttpClient 无超时
CloseableHttpClient client = HttpClients.createDefault();
```

**修复：**
```java
// RestTemplate 超时配置
@Bean
public RestTemplate restTemplate() {
    HttpComponentsClientHttpRequestFactory factory =
        new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(5000);      // 连接超时 5s
    factory.setReadTimeout(30000);        // 读取超时 30s
    return new RestTemplate(factory);
}

// OkHttp 超时配置
OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build();

// Apache HttpClient 超时配置
RequestConfig config = RequestConfig.custom()
    .setConnectTimeout(5000)
    .setSocketTimeout(30000)
    .build();
CloseableHttpClient client = HttpClients.custom()
    .setDefaultRequestConfig(config)
    .build();
```

### 2.2 同步 HTTP 调用在高并发路径

**风险等级：** 🟡 中危

**模式：**
```java
// 危险：在请求处理路径中同步调用外部服务
@GetMapping("/data")
public Data getData() {
    // 同步阻塞调用外部服务
    Data data = externalService.fetch();  // 可能阻塞 Tomcat 线程
    return data;
}
```

**修复：**
```java
// 方案 1：使用异步处理
@GetMapping("/data")
public CompletableFuture<Data> getData() {
    return CompletableFuture.supplyAsync(() ->
        externalService.fetch(),
        asyncExecutor
    );
}

// 方案 2：使用 WebClient（响应式）
@GetMapping("/data")
public Mono<Data> getData() {
    return webClient.get()
        .uri(externalUrl)
        .retrieve()
        .bodyToMono(Data.class);
}

// 方案 3：添加缓存
@Cacheable("external-data")
@GetMapping("/data")
public Data getData() {
    return externalService.fetch();
}
```

---

## 3. 数据库阻塞

### 3.1 慢查询未设置超时

**风险等级：** 🟡 中危

**模式：**
```java
// 危险：无查询超时
@Query("SELECT * FROM large_table WHERE ...")
List<Data> findAll();

// 危险：事务时间过长
@Transactional
public void processLargeBatch() {
    List<Data> all = repository.findAll();  // 可能加载大量数据
    // 长时间处理...
}
```

**修复：**
```java
// 设置查询超时
@QueryHints(@QueryHint(name = org.hibernate.jpa.QueryHints.HINT_TIMEOUT, value = "30000"))
List<Data> findAll();

// 分批处理
@Transactional
public void processLargeBatch() {
    int pageSize = 1000;
    int page = 0;
    Page<Data> dataPage;
    do {
        dataPage = repository.findAll(PageRequest.of(page, pageSize));
        processBatch(dataPage.getContent());
        page++;
    } while (dataPage.hasNext());
}
```

### 3.2 N+1 查询问题

**风险等级：** 🟡 中危

**模式：**
```java
// 危险：N+1 查询
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
    // 每个订单执行一次查询
}
```

**修复：**
```java
// 使用 JOIN FETCH
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id IN :ids")
List<Order> findWithItems(@Param("ids") List<Long> ids);

// 或使用 @EntityGraph
@EntityGraph(attributePaths = {"items"})
List<Order> findAll();
```

---

## 4. 响应式编程阻塞

### 4.1 在 Mono/Flux 中调用阻塞操作

**风险等级：** 🔴 高危

**模式：**
```java
// 危险：在响应式流中阻塞
public Mono<Data> getData() {
    return Mono.fromCallable(() -> {
        Data data = blockingService.fetch();  // 阻塞调用
        return data;
    });
}

// 危险：使用 block()
public Mono<Data> getData() {
    Data data = otherService.getData().block();  // 阻塞
    return Mono.just(data);
}
```

**修复：**
```java
// 使用 subscribeOn 将阻塞操作调度到专用线程池
public Mono<Data> getData() {
    return Mono.fromCallable(() -> blockingService.fetch())
        .subscribeOn(Schedulers.boundedElastic());  // 使用弹性线程池
}

// 或使用异步客户端
public Mono<Data> getData() {
    return webClient.get()
        .uri(url)
        .retrieve()
        .bodyToMono(Data.class);
}
```

---

## 5. 文件 IO 阻塞

### 5.1 同步文件读写

**风险等级：** 🟢 低危

**模式：**
```java
// 同步文件读取（小文件可接受）
byte[] bytes = Files.readAllBytes(path);

// 同步文件写入
Files.write(path, bytes);
```

**修复：** 对于大文件或高并发场景：
```java
// 使用异步文件通道
AsynchronousFileChannel channel = AsynchronousFileChannel.open(
    path, StandardOpenOption.READ
);
ByteBuffer buffer = ByteBuffer.allocate(8192);
Future<Integer> readResult = channel.read(buffer, 0);
```

---

## 6. 等待操作

### 6.1 Object.wait/Condition.await 无超时

**风险等级：** 🟡 中危

**模式：**
```java
// 危险：无限等待
synchronized (lock) {
    while (!condition) {
        lock.wait();  // 可能永远等待
    }
}

// 危险：Condition.await 无超时
lock.lock();
try {
    while (!ready) {
        condition.await();  // 可能永远等待
    }
} finally {
    lock.unlock();
}
```

**修复：**
```java
// 使用超时等待
synchronized (lock) {
    long deadline = System.currentTimeMillis() + 30000;
    while (!condition && System.currentTimeMillis() < deadline) {
        lock.wait(deadline - System.currentTimeMillis());
    }
}

// 或使用 Condition.await 超时版本
lock.lock();
try {
    boolean ready = condition.await(30, TimeUnit.SECONDS);
    if (!ready) {
        // 超时处理
    }
} finally {
    lock.unlock();
}
```

---

## 检测正则表达式

```regex
# CompletableFuture.get() 无超时
\.get\(\)\s*;(?![^}]*TimeUnit)

# Thread.sleep 在 @Async 方法
@Async[^}]*Thread\.sleep

# RestTemplate 无超时配置
new\s+RestTemplate\(\s*\)

# OkHttpClient.Builder 无超时
new\s+OkHttpClient\.Builder\(\)\s*\.\s*build\(\)

# WebClient 后调用 block
webClient[^;]*\.block\(\)

# wait() 无超时
\.wait\(\)\s*;(?![^}]*TimeUnit)

# Condition.await 无超时
\.await\(\)\s*;(?![^}]*TimeUnit)
```

---

## 项目特定检测

针对本项目的灰度追踪框架，需检测：

```java
// 危险：在灰度过滤器中执行阻塞操作
public class GrayFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        // 阻塞调用外部服务获取灰度规则
        GrayRule rule = ruleService.fetchRuleBlocking();  // 阻塞 Tomcat 线程
        ...
    }
}

// 正确：使用缓存或异步
public class GrayFilter implements Filter {
    private final LoadingCache<String, GrayRule> ruleCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .build(this::fetchRuleAsync);

    @Override
    public void doFilter(...) {
        GrayRule rule = ruleCache.get(key);  // 从缓存获取
        ...
    }
}
```
