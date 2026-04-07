# 锁竞争风险模式库

本文档定义锁竞争和死锁相关的性能风险检测规则。

---

## 1. 锁粒度问题

### 1.1 锁粒度过粗

**风险等级：** 🟡 中危

**模式：**
```java
// 危险：整个方法加锁，阻塞所有调用
public synchronized void process() {
    validateInput();      // 无需加锁
    queryDatabase();      // IO 操作，阻塞时间长
    updateCache();        // 需要加锁的部分
}

// 危险：synchronized 块内执行 IO
synchronized (lock) {
    database.query(sql);  // IO 操作持锁
    file.write(data);     // IO 操作持锁
}
```

**问题：** 锁持有时间过长，并发性能下降

**修复：**
```java
// 缩小锁范围
public void process() {
    validateInput();
    queryDatabase();
    synchronized (this) {
        updateCache();  // 仅对需要同步的部分加锁
    }
}

// 或使用读写锁
private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

public Data read() {
    rwLock.readLock().lock();
    try {
        return cache.get();
    } finally {
        rwLock.readLock().unlock();
    }
}

public void write(Data data) {
    rwLock.writeLock().lock();
    try {
        cache.put(data);
    } finally {
        rwLock.writeLock().unlock();
    }
}
```

---

## 2. 死锁风险

### 2.1 嵌套锁（不同顺序）

**风险等级：** 🔴 高危

**模式：**
```java
// 危险：嵌套锁，顺序不一致
// 线程 A: lock1 -> lock2
// 线程 B: lock2 -> lock1
// 死锁！
public void transfer(Account from, Account to, int amount) {
    synchronized (from) {
        synchronized (to) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}
```

**修复：**
```java
// 方案 1：统一锁顺序
public void transfer(Account from, Account to, int amount) {
    Account first = from.getId() < to.getId() ? from : to;
    Account second = from.getId() < to.getId() ? to : from;

    synchronized (first) {
        synchronized (second) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}

// 方案 2：使用单一锁
private final Object globalLock = new Object();
public void transfer(Account from, Account to, int amount) {
    synchronized (globalLock) {
        from.debit(amount);
        to.credit(amount);
    }
}

// 方案 3：使用 java.util.concurrent 锁
private final Lock lock1 = new ReentrantLock();
private final Lock lock2 = new ReentrantLock();

public void transfer() throws InterruptedException {
    if (lock1.tryLock(100, TimeUnit.MILLISECONDS)) {
        try {
            if (lock2.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    // 执行操作
                } finally {
                    lock2.unlock();
                }
            }
        } finally {
            lock1.unlock();
        }
    }
}
```

### 2.2 锁对象使用不当

**风险等级：** 🔴 高危

**模式：**
```java
// 危险：使用 String 作为锁对象
String lock = "LOCK";  // 字符串常量池可能导致意外共享
synchronized (lock) { ... }

// 危险：使用装箱类型
Integer lock = 100;  // -128 到 127 会被缓存，可能意外共享
synchronized (lock) { ... }

// 危险：使用可变对象
List<String> list = new ArrayList<>();
synchronized (list) {
    list.add("item");  // 如果 list 被替换，锁失效
}
```

**修复：**
```java
// 使用专用的锁对象
private final Object lock = new Object();
synchronized (lock) { ... }

// 或使用 ReentrantLock
private final Lock lock = new ReentrantLock();
```

---

## 3. ReentrantLock 使用问题

### 3.1 未在 finally 中释放锁

**风险等级：** 🔴 高危

**模式：**
```java
// 危险：异常时锁不会被释放
Lock lock = new ReentrantLock();
public void process() {
    lock.lock();
    doSomething();  // 如果抛异常，锁永远不释放
    lock.unlock();
}
```

**修复：**
```java
public void process() {
    lock.lock();
    try {
        doSomething();
    } finally {
        lock.unlock();  // 确保锁被释放
    }
}
```

### 3.2 lockInterruptibly 使用不当

**风险等级：** 🟡 中危

**模式：**
```java
// 可能忽略中断
lock.lockInterruptibly();
// 未处理 InterruptedException
```

**修复：**
```java
try {
    lock.lockInterruptibly();
    try {
        // 执行操作
    } finally {
        lock.unlock();
    }
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();  // 恢复中断状态
    // 处理中断
}
```

---

## 4. 等待/通知问题

### 4.1 wait/notify 使用不当

**风险等级：** 🟡 中危

**模式：**
```java
// 危险：不在循环中等待
synchronized (lock) {
    if (!condition) {  // 应该用 while
        lock.wait();
    }
}

// 危险：notify 而非 notifyAll
synchronized (lock) {
    lock.notify();  // 可能唤醒错误的线程
}
```

**修复：**
```java
// 使用 while 循环检查条件
synchronized (lock) {
    while (!condition) {  // 防止虚假唤醒
        lock.wait();
    }
    // 执行操作
}

// 或使用 Condition
private final Lock lock = new ReentrantLock();
private final Condition condition = lock.newCondition();

public void await() throws InterruptedException {
    lock.lock();
    try {
        while (!ready) {
            condition.await();
        }
    } finally {
        lock.unlock();
    }
}

public void signal() {
    lock.lock();
    try {
        ready = true;
        condition.signalAll();
    } finally {
        lock.unlock();
    }
}
```

---

## 5. 并发集合误用

### 5.1 复合操作非原子

**风险等级：** 🟡 中危

**模式：**
```java
// 危险：复合操作非原子
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
if (!map.containsKey(key)) {  // 检查
    map.put(key, value);       // 操作
}
// 在检查和操作之间，其他线程可能已经 put

// 危险：size + remove
if (map.size() > 1000) {
    map.remove(someKey);
}
```

**修复：**
```java
// 使用原子方法
map.putIfAbsent(key, value);

// 或使用 compute
map.compute(key, (k, v) -> v == null ? value : v);

// 复合操作使用同步
synchronized (map) {
    if (!map.containsKey(key)) {
        map.put(key, value);
    }
}
```

---

## 检测正则表达式

```regex
# synchronized 方法
public\s+synchronized\s+\w+\s+\w+\s*\(

# synchronized 块内可能有 IO（需要上下文判断）
synchronized\s*\([^)]+\)\s*\{[^}]*\.(query|execute|write|read|send|receive)\(

# 嵌套 synchronized
synchronized\s*\([^)]+\)\s*\{[^}]*synchronized\s*\(

# ReentrantLock.lock() 不在 try 块中
\.lock\(\)\s*;\s*\n\s*[^t][^r][^y]

# wait 不在 while 循环中
if\s*\([^)]+\)\s*\{\s*\.wait\(\)

# String 作为锁对象
synchronized\s*\(\s*"[^"]*"\s*\)

# Integer/Long 作为锁对象
synchronized\s*\(\s*(Integer|Long)\s*\.
```
