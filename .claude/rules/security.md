# 安全规则

本规则定义了代码开发过程中的安全约束，确保代码安全性和敏感信息保护。

---

## 1. 敏感信息保护

### 禁止硬编码敏感信息

**绝不**在代码中硬编码以下敏感信息：

- 密码、密钥、Token
- 数据库连接字符串（含密码）
- API 密钥、Secret Key
- 私钥文件内容
- 用户个人信息

```java
// ❌ 禁止
String password = "myPassword123";
String apiKey = "sk-xxxxx";

// ✅ 正确
String password = System.getenv("DB_PASSWORD");
String apiKey = properties.getApiKey();
```

### 敏感信息文件处理

- `.env`、`credentials.json`、`*.pem` 等敏感文件**不得提交到 Git**
- 确保 `.gitignore` 包含这些文件模式
- 提供示例配置文件（如 `.env.example`）

---

## 2. 输入验证

### 所有外部输入必须验证

- HTTP 请求参数
- 数据库查询结果
- 文件内容
- 用户输入

```java
// ✅ 验证后再使用
public void process(String input) {
    if (input == null || input.isEmpty()) {
        throw new IllegalArgumentException("Input cannot be null or empty");
    }
    // 处理逻辑
}
```

### 防止注入攻击

- SQL 查询使用参数化语句，禁止字符串拼接
- 避免命令注入，不直接执行用户输入

```java
// ❌ 禁止
String sql = "SELECT * FROM users WHERE id = " + userId;

// ✅ 正确
String sql = "SELECT * FROM users WHERE id = ?";
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setString(1, userId);
```

---

## 3. 认证与授权

### 密码存储

- 使用强哈希算法（BCrypt、Argon2）
- 禁止明文存储密码
- 禁止使用 MD5、SHA1 等弱哈希

### Token 处理

- JWT Token 不存储敏感信息
- Token 设置合理过期时间
- 敏感操作需要二次验证

---

## 4. 日志安全

### 禁止记录敏感信息

```java
// ❌ 禁止
log.info("User login: password={}", password);
log.info("API call with token: {}", authToken);

// ✅ 正确
log.info("User login: username={}", username);
log.info("API call initiated by user: {}", userId);
```

### 异常处理

- 异常信息不暴露系统内部细节
- 生产环境禁用详细错误堆栈输出

---

## 5. 依赖安全

### 第三方依赖

- 使用可信来源的依赖
- 定期检查依赖漏洞（使用 OWASP Dependency Check）
- 及时更新有漏洞的依赖版本

---

## 6. 文件操作安全

### 路径遍历防护

```java
// ❌ 禁止直接使用用户输入作为路径
File file = new File(userInput);

// ✅ 验证路径
Path basePath = Paths.get("/safe/directory");
Path resolvedPath = basePath.resolve(userInput).normalize();
if (!resolvedPath.startsWith(basePath)) {
    throw new SecurityException("Path traversal detected");
}
```

---

## 违规处理

发现安全违规时：
1. 立即停止相关代码的提交
2. 报告安全问题
3. 修复后方可继续
