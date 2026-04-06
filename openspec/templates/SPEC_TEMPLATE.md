# Spec 模板

本文档提供 spec 文件的编写模板，可直接复制使用。

---

## 基础模板

```markdown
## ADDED Requirements

### Requirement: {功能简短描述}
当 {前置条件} 时，系统 SHALL {行为描述}。

#### Scenario: {场景名称}
- **WHEN** {条件描述}
- **THEN** {预期行为} SHALL {具体结果}

#### Scenario: {场景名称}
- **WHEN** {条件描述}
- **THEN** {预期行为} SHALL {具体结果}

#### Scenario: 配置关闭时不执行
- **WHEN** `{配置键}=false`
- **THEN** 系统 SHALL NOT {禁止的行为}
```

---

## Spring Boot 功能模板

```markdown
## ADDED Requirements

### Requirement: {组件名} 出口自动透传灰度标签
当 Spring 容器中存在 `{目标类全限定名}` Bean，且 `{配置属性}` 为 `true`（默认）时，
系统 SHALL 自动在该客户端的每次出口请求中将当前 `GrayContext` 中的 `x-gray-tag` 值注入。

#### Scenario: 灰度上下文存在时自动注入
- **WHEN** 当前线程的 `GrayContext` 中存在非空的灰度标签值
- **THEN** 通过 {组件名} 发出的请求中 SHALL 包含值与上下文一致的 `x-gray-tag` 请求头

#### Scenario: 灰度上下文为空时不注入
- **WHEN** 当前线程的 `GrayContext` 中灰度标签为 `null` 或空字符串
- **THEN** 通过 {组件名} 发出的请求中 SHALL NOT 包含 `x-gray-tag` 请求头

#### Scenario: 配置关闭时不注入
- **WHEN** `{配置属性}=false`
- **THEN** {组件名} BeanPostProcessor SHALL NOT 注入拦截器

#### Scenario: 拦截器不重复注入
- **WHEN** 同一个 Bean 被多次经过 BeanPostProcessor
- **THEN** 系统 SHALL 检测拦截器列表中是否已存在对应拦截器，若已存在则跳过注入

### Requirement: {组件名} 配置属性支持
系统 SHALL 通过 `{配置属性}` 属性提供对 {功能名} 的独立开关控制，默认值为 `true`。

#### Scenario: 默认启用
- **WHEN** 应用配置中未显式设置 `{配置属性}`
- **THEN** {功能名} SHALL 默认启用

#### Scenario: 显式禁用
- **WHEN** 应用配置中设置 `{配置属性}=false`
- **THEN** {功能名} SHALL 被禁用

#### Scenario: 全局开关优先
- **WHEN** `gray.trace.enabled=false`（全局总开关关闭）
- **THEN** {功能名} SHALL 也随之禁用
```

---

## Agent 模式模板

```markdown
## Agent 模式

### Requirement: Agent 模式下 {目标类} 灰度标透传
Java Agent 安装后，`{目标类全限定名}` 的 {目标方法} 方法调用 SHALL 在 {时机} 自动注入 `x-gray-tag`，
值来自当前线程的 `GrayContext`。

#### Scenario: 当前线程携带灰度标时自动注入
- **WHEN** 当前线程 `GrayContext.get()` 返回非 `stable` 的灰度标（例如 `gray-v1`）
- **THEN** {目标操作} 的 `x-gray-tag` Header 值 SHALL 等于该灰度标

#### Scenario: 当前线程无灰度标时不注入
- **WHEN** 当前线程 `GrayContext.get()` 返回 `stable`
- **THEN** {目标操作} SHALL 不添加 `x-gray-tag` Header

#### Scenario: 已存在 Header 时不覆盖
- **WHEN** {目标操作} 已包含 `x-gray-tag` Header
- **THEN** Agent SHALL 保留原有 Header 值，不覆盖

#### Scenario: 配置开关关闭时不插桩
- **WHEN** `{配置属性}=false`
- **THEN** Agent SHALL 跳过对 {目标类} 的字节码插桩

### Requirement: Agent 插桩失败不影响业务
若目标应用 classpath 上不存在 {目标依赖}，Agent SHALL 静默忽略插桩错误，不抛出异常，不阻止应用启动。

#### Scenario: 依赖不存在时静默跳过
- **WHEN** `{目标类}` 类不在 classpath 上
- **THEN** Agent 启动日志 SHALL 输出警告，应用 SHALL 正常启动
```

---

## 多版本支持模板

```markdown
## ADDED Requirements

### Requirement: {组件名} 4.x 出口自动透传灰度标签
当 {4.x 条件} 时，系统 SHALL {4.x 行为}。

#### Scenario: {场景名称}
- **WHEN** {条件}
- **THEN** {预期行为}

### Requirement: {组件名} 5.x 出口自动透传灰度标签
当 {5.x 条件} 时，系统 SHALL {5.x 行为}。

#### Scenario: {场景名称}
- **WHEN** {条件}
- **THEN** {预期行为}

#### Scenario: 4.x 与 5.x 共存时互不干扰
- **WHEN** Spring 容器中同时存在 4.x 和 5.x 的相关 Bean
- **THEN** 系统 SHALL 分别为两者注入对应版本的拦截器，各自独立工作，互不影响
```

---

## 异步/并发场景模板

```markdown
## ADDED Requirements

### Requirement: {组件名} 异步方法灰度上下文传递
{组件描述} 的异步方法（{方法列表}）SHALL 在任务执行前捕获当前线程的 `GrayContext`，
并在异步线程中恢复，保证灰度标正确传递。

#### Scenario: 异步执行时灰度标传递
- **WHEN** 当前线程 `GrayContext` 包含灰度标 `{示例值}`
- **AND** 调用 `{方法调用示例}`
- **THEN** 异步任务执行时 `GrayContext.get()` SHALL 返回 `{示例值}`

#### Scenario: 指定 Executor 时灰度标传递
- **WHEN** 当前线程 `GrayContext` 包含灰度标 `{示例值}`
- **AND** 调用 `{带 Executor 的方法调用示例}`
- **THEN** 异步任务在 executor 的线程中执行时 `GrayContext.get()` SHALL 返回 `{示例值}`

#### Scenario: 无灰度标时不注入
- **WHEN** 当前线程 `GrayContext.get()` 返回 `stable`
- **AND** 调用任意异步方法
- **THEN** 异步任务执行时 `GrayContext.get()` SHALL 返回 `stable`

### Requirement: {组件名} 拦截不影响原有语义
Agent 插桩后，{组件名} 的返回值、异常处理、链式调用语义 SHALL 保持不变。

#### Scenario: 返回值正确传递
- **WHEN** 调用 `{方法调用示例}`
- **THEN** `future.get()` SHALL 返回预期结果

#### Scenario: 异常正确传递
- **WHEN** 异步任务抛出 `{异常类型}`
- **THEN** `future.get()` SHALL 抛出 `{包装异常}` 包装原异常
```

---

## 占位符说明

| 占位符 | 说明 | 示例 |
|-------|------|------|
| `{功能简短描述}` | Requirement 的简短标题 | Apache HttpClient 4.x 出口自动透传灰度标签 |
| `{前置条件}` | 触发功能的前置条件 | Spring 容器中存在 `CloseableHttpClient` Bean |
| `{行为描述}` | 系统应执行的行为 | 自动在出口请求中注入灰度标签 |
| `{场景名称}` | Scenario 的描述性名称 | 灰度上下文存在时自动注入请求头 |
| `{条件描述}` | WHEN 条件的具体内容 | 当前线程的 `GrayContext` 中存在非空的灰度标签值 |
| `{预期行为}` | THEN 中的预期结果 | 发出的请求中 |
| `{具体结果}` | SHALL 后的具体结果描述 | 包含值与上下文一致的 `x-gray-tag` 请求头 |
| `{配置键}` | 配置属性键 | `gray.trace.apache-http-client.enabled` |
| `{目标类全限定名}` | 目标类的完整包路径 | `org.apache.http.impl.client.CloseableHttpClient` |
| `{组件名}` | 功能组件名称 | Apache HttpClient |
| `{示例值}` | 示例值 | `gray-v1` |
