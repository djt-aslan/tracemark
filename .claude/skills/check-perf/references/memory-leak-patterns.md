# 内存泄漏风险模式库

本文档定义内存泄漏相关的性能风险检测规则。

---

## 1. ThreadLocal 泄漏

### 1.1 ThreadLocal 未清理

**风险等级：** 🔴 高危

**模式：**
```java
// 危险：ThreadLocal.set() 后无 remove()
public class RequestContext {
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();

    public void setUser(User user) {
        currentUser.set(user);
        // 缺少清理逻辑
    }
}

// 危险：异常路径未清理
public void process() {
    threadLocal.set(value);
    doSomething();  // 可能抛异常
    threadLocal.remove();  // 不会执行
}
```

**问题：** 线程池复用时，ThreadLocal 值残留导致：
- 内存泄漏
- 数据错乱（上一个请求的数据被当前请求读取）

**修复：**
```java
// 方案 1：try-finally 确保清理
public void process() {
    try {
        threadLocal.set(value);
        doSomething();
    } finally {
        threadLocal.remove();
    }
}

// 方案 2：使用 TransmittableThreadLocal（阿里 TTL）
private static final TransmittableThreadLocal<String> CONTEXT =
    new TransmittableThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return DEFAULT_VALUE;
        }
    };

// 方案 3：封装为工具类
public class ThreadLocalHolder {
    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    public static void set(Context ctx) {
        HOLDER.set(ctx);
    }

    public static Context get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

// 使用时
try {
    ThreadLocalHolder.set(context);
    // 业务逻辑
} finally {
    ThreadLocalHolder.clear();
}
```

### 1.2 ThreadLocal 存储大对象

**风险等级：** 🟡 中危

**模式：**
```java
// 危险：存储大对象
private static final ThreadLocal<LargeCache> cache = new ThreadLocal<>();
cache.set(new LargeCache(100_000));  // 每个线程一份副本
```

**问题：** 线程数 × 大对象 = 内存爆炸

**修复：**
```java
// 使用弱引用或共享对象
private static final ThreadLocal<WeakReference<LargeCache>> cache =
    new ThreadLocal<>();

// 或重新设计，避免 ThreadLocal 存储大对象
```

---

## 2. 集合无限增长

### 2.1 静态集合无清理

**风险等级：** 🔴 高危

**模式：**
```java
// 危险：静态 Map 无限增长
public class Cache {
    private static final Map<String, Data> CACHE = new HashMap<>();

    public static void put(String key, Data data) {
        CACHE.put(key, data);  // 只增不减
    }
}

// 危险：监听器列表无移除
public class EventBus {
    private static final List<Listener> listeners = new ArrayList<>();

    public static void register(Listener listener) {
        listeners.add(listener);  // 只增不减
    }
}
```

**修复：**
```java
// 方案 1：使用有界缓存
public class Cache {
    private static final int MAX_SIZE = 10000;
    private static final LinkedHashMap<String, Data> CACHE =
        new LinkedHashMap<String, Data>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Data> eldest) {
                return size() > MAX_SIZE;
            }
        };
}

// 方案 2：使用 Caffeine/Guava Cache
private static final Cache<String, Data> CACHE = Caffeine.newBuilder()
    .maximumSize(10000)
    .expireAfterWrite(Duration.ofMinutes(10))
    .build();

// 方案 3：提供注销方法
public class EventBus {
    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public static void register(Listener listener) {
        listeners.add(listener);
    }

    public static void unregister(Listener listener) {
        listeners.remove(listener);
    }
}

// 方案 4：使用 WeakHashMap（键为弱引用）
private static final Map<Key, Value> CACHE = new WeakHashMap<>();
```

### 2.2 缓存无过期策略

**风险等级：** 🟡 中危

**模式：**
```java
// 危险：无过期时间
Map<String, CachedData> cache = new ConcurrentHashMap<>();
cache.put(key, data);  // 永不过期
```

**修复：**
```java
// 使用 Caffeine
Cache<String, CachedData> cache = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofMinutes(30))
    .refreshAfterWrite(Duration.ofMinutes(10))
    .build();
```

---

## 3. 资源未释放

### 3.1 连接/流未关闭

**风险等级：** 🔴 高危

**模式：**
```java
// 危险：未关闭连接
Connection conn = dataSource.getConnection();
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(sql);
// 如果这里抛异常，资源泄漏
conn.close();

// 危险：流未关闭
InputStream is = new FileInputStream(file);
// 处理...
is.close();  // 如果前面抛异常，不会执行
```

**修复：**
```java
// try-with-resources
try (Connection conn = dataSource.getConnection();
     Statement stmt = conn.createStatement();
     ResultSet rs = stmt.executeQuery(sql)) {
    // 处理结果
}

// 或 try-finally
InputStream is = null;
try {
    is = new FileInputStream(file);
    // 处理...
} finally {
    if (is != null) {
        is.close();
    }
}
```

### 3.2 数据库连接池泄漏

**风险等级：** 🔴 高危

**模式：**
```java
// 危险：获取连接后未归还
public void query() {
    Connection conn = dataSource.getConnection();
    // 异常时连接不归还池
    conn.close();
}
```

**检测方式：**
- HikariCP: 检查 `leakDetectionThreshold` 配置
- Druid: 检查 `removeAbandoned` 配置

---

## 4. 回调/监听器泄漏

### 4.1 注册后未注销

**风险等级：** 🟡 中危

**模式：**
```java
// 危险：注册监听器后未注销
public class MyComponent {
    public void init() {
        eventBus.register(this);  // 注册
        // 缺少注销逻辑
    }
}
```

**修复：**
```java
public class MyComponent implements DisposableBean {
    public void init() {
        eventBus.register(this);
    }

    @Override
    public void destroy() {
        eventBus.unregister(this);  // 注销
    }
}
```

---

## 5. 对象引用链

### 5.1 内部类持有外部类引用

**风险等级：** 🟡 中危

**模式：**
```java
// 危险：非静态内部类持有外部类引用
public class Outer {
    private byte[] largeData = new byte[1024 * 1024];

    public Runnable createTask() {
        return new Runnable() {  // 非静态内部类
            @Override
            public void run() {
                // 即使不使用 largeData，也会持有 Outer 引用
            }
        };
    }
}
```

**修复：**
```java
// 使用静态内部类
public class Outer {
    private byte[] largeData = new byte[1024 * 1024];

    public Runnable createTask() {
        return new StaticTask();  // 静态内部类
    }

    private static class StaticTask implements Runnable {
        @Override
        public void run() {
            // 不持有 Outer 引用
        }
    }
}
```

---

## 检测正则表达式

```regex
# ThreadLocal.set() 后无 remove()
ThreadLocal.*\.set\([^)]*\)[^;]*;(?![^}]*\.remove\(\))

# 静态集合
private\s+static\s+final\s+(Map|List|Set|Collection)<[^>]+>\s+\w+\s*=\s*new

# 未使用 try-with-resources 的资源
(Connection|Statement|ResultSet|InputStream|OutputStream|Reader|Writer)\s+\w+\s*=\s*[^;]+;(?!.*try\s*\()

# 非静态内部类实现接口
new\s+\w+\(\)\s*\{[^}]*implements\s+(Runnable|Callable|Listener)
```

---

## 项目特定：GrayContext 检测

针对本项目的 `GrayContext`，需额外检测：

```java
// 危险：设置后未清理
GrayContext.set(tag);
// 业务逻辑
// 缺少 GrayContext.clear()

// 正确模式
try {
    GrayContext.set(tag);
    // 业务逻辑
} finally {
    GrayContext.clear();
}
```
