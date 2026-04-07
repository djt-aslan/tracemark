# Spec 快速参考卡片

本文档提供 spec 编写的快速参考，适合打印或快速查阅。

---

## Requirement 结构

```markdown
### Requirement: {简短描述}
当 {前置条件} 时，系统 SHALL {行为描述}。
```

---

## Scenario 结构

```markdown
#### Scenario: {场景名称}
- **WHEN** {条件描述}
- **THEN** {预期行为} SHALL {具体结果}
```

**扩展形式：**

```markdown
#### Scenario: {场景名称}
- **WHEN** {条件1}
- **AND** {条件2}
- **THEN** {预期行为} SHALL {具体结果}
```

---

## 关键词速查

| 关键词 | 用途 | 示例 |
|-------|------|------|
| **SHALL** | 强制要求 | 系统 SHALL 自动注入 |
| **SHALL NOT** | 禁止行为 | 请求中 SHALL NOT 包含 |
| **SHOULD** | 推荐做法 | SHOULD 优先使用 |
| **MAY** | 可选功能 | MAY 提供扩展 |
| **WHEN** | 触发条件 | **WHEN** 配置启用 |
| **THEN** | 预期结果 | **THEN** 请求 SHALL 包含 |
| **AND** | 补充条件 | **AND** 灰度标非空 |

---

## 必须覆盖的场景类型

| 类型 | 说明 | 关键词 |
|-----|------|-------|
| 正向场景 | 正常条件下的行为 | SHALL |
| 空值场景 | null/空值处理 | SHALL NOT |
| 配置场景 | 开关控制 | enabled=false |
| 幂等场景 | 重复操作 | 跳过/不重复 |
| 兼容场景 | 多版本共存 | 互不干扰 |

---

## 格式规范速查

```markdown
# 类名/方法名用反引号
`CloseableHttpClient` Bean
`GrayContext.get()` 方法

# 配置属性用反引号
`gray.trace.apache-http-client.enabled`

# 默认值用括号说明
`true`（默认）

# 分隔不同 Requirement 组
---
```

---

## 标题层级

| 层级 | 用途 | 示例 |
|-----|------|------|
| `##` | 模式分组 | `## Agent 模式` |
| `###` | Requirement | `### Requirement: ...` |
| `####` | Scenario | `#### Scenario: ...` |

---

## 检查清单

- [ ] Requirement 有 SHALL 描述
- [ ] Scenario 使用 WHEN-THEN
- [ ] 技术术语用反引号
- [ ] 配置属性说明默认值
- [ ] 覆盖空值/配置/幂等场景

---

## 常用句式

**前置条件：**
- "当 Spring 容器中存在 `{类名}` Bean"
- "当 `{配置属性}` 为 `true`（默认）时"
- "当当前线程的 `GrayContext` 中存在非空的灰度标签值"

**行为描述：**
- "系统 SHALL 自动在...中注入..."
- "系统 SHALL 通过 `{配置属性}` 属性提供...开关控制"
- "Agent SHALL 在...前自动注入..."

**预期结果：**
- "请求中 SHALL 包含值与上下文一致的 `x-gray-tag` 请求头"
- "请求中 SHALL NOT 包含 `x-gray-tag` 请求头"
- "系统 SHALL 检测...，若已存在则跳过"
