# Spec 编写规范

本文档定义了 tracemark 项目 spec 文件的编写规范，确保团队产出一致、高质量的需求规格文档。

## 1. 文档结构

### 1.1 Spec 文件位置

- **活跃 spec**: `openspec/specs/{feature-name}/spec.md`
- **归档 spec**: `openspec/changes/archive/{date}-{feature-name}/specs/{feature-name}/spec.md`

### 1.2 文档组织

每个 spec 文件应包含以下部分（按顺序）：

1. **背景**（可选）：说明需求的来源、动机
2. **Requirements**：核心需求定义
3. **Scenarios**：每个 Requirement 下的具体场景

## 2. Requirement 编写规范

### 2.1 标题格式

```markdown
### Requirement: {简短描述}
```

- 使用中文描述
- 简洁明了，一句话概括核心需求
- 示例：`### Requirement: Apache HttpClient 4.x 出口自动透传灰度标签`

### 2.2 描述格式

Requirement 描述应遵循以下模式：

```
当 {前置条件} 时，系统 SHALL {行为描述}。
```

**关键要素：**

1. **前置条件**：明确触发条件（如 Bean 存在、配置启用）
2. **SHALL 关键词**：使用 RFC 2119 规范的 SHALL 表示强制要求
3. **行为描述**：清晰说明系统应执行的操作

**示例：**

```markdown
当 Spring 容器中存在 `org.apache.http.impl.client.CloseableHttpClient` Bean，
且 `gray.trace.apache-http-client.enabled` 为 `true`（默认）时，
系统 SHALL 自动在该客户端的每次出口 HTTP 请求中将当前 `GrayContext` 中的
`x-gray-tag` 值注入请求头。
```

### 2.3 多版本支持

当功能需要支持多个版本（如 4.x 和 5.x）时：

1. 分别创建独立的 Requirement
2. 在标题中明确版本号
3. 说明版本间的差异和共存策略

## 3. Scenario 编写规范

### 3.1 标题格式

```markdown
#### Scenario: {场景描述}
```

- 使用中文描述
- 描述具体的行为或边界条件
- 示例：`#### Scenario: 灰度上下文存在时自动注入请求头`

### 3.2 结构格式

每个 Scenario 使用 **WHEN-THEN** 结构：

```markdown
#### Scenario: {场景名称}
- **WHEN** {条件描述}
- **THEN** {预期行为} SHALL {具体结果}
```

**可选扩展：**

- 使用 **AND** 添加额外条件或预期
- 使用 **SHALL NOT** 表示禁止的行为

**示例：**

```markdown
#### Scenario: 灰度上下文存在时自动注入请求头
- **WHEN** 当前线程的 `GrayContext` 中存在非空的灰度标签值
- **THEN** 通过 Apache HttpClient 4.x `CloseableHttpClient` 发出的请求中
  SHALL 包含值与上下文一致的 `x-gray-tag` 请求头
```

### 3.3 场景覆盖要求

每个 Requirement 应至少覆盖以下场景类型：

| 场景类型 | 说明 | 示例 |
|---------|------|------|
| 正向场景 | 正常条件下的预期行为 | 灰度上下文存在时自动注入请求头 |
| 空值/边界场景 | 空值、null、默认值等边界条件 | 灰度上下文为空时不注入请求头 |
| 配置场景 | 配置开关的行为 | 配置关闭时不注入请求头 |
| 幂等场景 | 重复操作的幂等性保证 | 拦截器不重复注入 |
| 兼容场景 | 多版本共存或迁移场景 | 4.x 与 5.x 共存时互不干扰 |
| 异常场景 | 异常情况的处理（可选） | 依赖不存在时静默跳过 |

## 4. 术语与关键词

### 4.1 RFC 2119 关键词

| 关键词 | 含义 | 使用场景 |
|-------|------|---------|
| **SHALL** | 必须执行 | 强制性要求 |
| **SHALL NOT** | 禁止执行 | 禁止性行为 |
| **SHOULD** | 建议执行 | 推荐做法 |
| **MAY** | 可选执行 | 可选功能 |

### 4.2 技术术语保留英文

以下内容保留英文原文，不翻译：

- 类名：`CloseableHttpClient`、`GrayContext`
- 方法名：`postProcessAfterInitialization`
- 包名：`org.apache.http.impl.client`
- 配置键：`gray.trace.apache-http-client.enabled`
- HTTP Header：`x-gray-tag`

## 5. 格式规范

### 5.1 代码标识符格式

使用反引号包裹代码标识符：

```markdown
`CloseableHttpClient` Bean
`GrayContext.get()` 方法
`gray.trace.apache-http-client.enabled` 属性
```

### 5.2 分隔符使用

- 使用 `---` 分隔不同的 Requirement 组
- 使用 `##` 作为一级标题（如 Agent 模式分组）
- 使用 `###` 作为 Requirement 标题
- 使用 `####` 作为 Scenario 标题

### 5.3 列表格式

- 场景条件使用无序列表
- **WHEN**、**THEN**、**AND** 使用粗体
- 每个条件/预期独占一行

## 6. 模式分类

### 6.1 Spring Boot 模式

适用于通过 Spring Bean 机制实现的功能：

- 使用 `BeanPostProcessor` 检测 Bean
- 使用 `@ConditionalOnClass` 条件注册
- 使用 `@ConditionalOnProperty` 配置开关

### 6.2 Agent 模式

适用于 Java Agent 字节码插桩功能：

- 明确标注 `## Agent 模式`
- 说明插桩目标类和方法
- 描述异常情况下的降级行为

## 7. 检查清单

编写完成后，请检查以下项目：

- [ ] 每个 Requirement 都有明确的 SHALL 行为描述
- [ ] 每个 Requirement 至少有 2 个 Scenario
- [ ] Scenario 使用 WHEN-THEN 结构
- [ ] 技术术语使用反引号包裹
- [ ] 配置属性说明默认值
- [ ] 多版本支持已明确区分
- [ ] 异常/边界场景已覆盖

## 8. 示例参考

完整示例请参考：

- `openspec/specs/apache-http-client-outbound/spec.md`
- `openspec/specs/completable-future-async/spec.md`
