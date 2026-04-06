---
name: check-spec
description: Spec 规约检查。在 /writing-plans 之前执行，检查 spec 是否符合规范格式。
---

# Spec 规约检查

检查当前变更的 spec 是否符合规范格式。

## 执行步骤

### 1. 确定检查目标

首先确定要检查的 spec 文件：

- 若存在活跃变更：检查 `openspec/changes/<name>/specs/<capability>/spec.md`
- 若无活跃变更：检查 `openspec/specs/<capability>/spec.md`
- 若用户指定了变更名：检查该变更的 spec

```bash
openspec list --json
```

### 2. 读取 spec 文件

读取目标 spec 文件内容。

### 3. 执行检查规则

逐项检查以下规则：

#### 3.1 Requirement 结构检查

| 检查项 | 规则 | 错误提示 |
|-------|------|---------|
| SHALL 关键词 | 每个 Requirement 描述中必须包含 `SHALL` | "Requirement '{title}' 缺少 SHALL 关键词" |
| 前置条件 | Requirement 描述应包含触发条件（当...时） | "Requirement '{title}' 缺少前置条件描述" |

#### 3.2 Scenario 结构检查

| 检查项 | 规则 | 错误提示 |
|-------|------|---------|
| Scenario 数量 | 每个 Requirement 至少有 2 个 Scenario | "Requirement '{title}' 只有 {n} 个 Scenario，至少需要 2 个" |
| WHEN 关键词 | 每个 Scenario 必须包含 `**WHEN**` | "Scenario '{title}' 缺少 WHEN 条件" |
| THEN 关键词 | 每个 Scenario 必须包含 `**THEN**` | "Scenario '{title}' 缺少 THEN 预期结果" |
| SHALL/SHALL NOT | THEN 后必须包含 `SHALL` 或 `SHALL NOT` | "Scenario '{title}' 的 THEN 缺少 SHALL/SHALL NOT" |

#### 3.3 格式检查

| 检查项 | 规则 | 错误提示 |
|-------|------|---------|
| 代码标识符 | 类名、方法名、配置键应使用反引号包裹 | "发现未包裹的代码标识符：{text}" |
| 标题层级 | Requirement 使用 `###`，Scenario 使用 `####` | "标题层级错误：'{text}'" |

#### 3.4 场景覆盖检查

| 场景类型 | 是否必须 | 检查方式 |
|---------|---------|---------|
| 正向场景 | ✓ | 检查是否有正常条件下的 Scenario |
| 空值场景 | ✓ | 检查是否有 null/空值处理的 Scenario |
| 配置场景 | ✓ | 检查是否有 enabled=false 等开关场景 |
| 幂等场景 | ✓ | 检查是否有重复操作/不重复注入的 Scenario |

### 4. 输出检查结果

#### 检查通过

```
## Spec 规约检查通过 ✓

**文件：** {spec-path}

**统计：**
- Requirements: {n} 个
- Scenarios: {n} 个
- 场景覆盖：正向 ✓ 空值 ✓ 配置 ✓ 幂等 ✓

所有检查项均符合规范。
```

#### 检查失败

```
## Spec 规约检查失败 ✗

**文件：** {spec-path}

### 错误 ({error_count})

1. **[Requirement 结构]** Requirement 'xxx' 缺少 SHALL 关键词
   - 位置：第 {line} 行
   - 建议：在行为描述中添加 SHALL，如 "系统 SHALL 自动注入..."

2. **[Scenario 结构]** Scenario 'xxx' 缺少 WHEN 条件
   - 位置：第 {line} 行
   - 建议：使用 `- **WHEN** {条件描述}` 格式

### 警告 ({warning_count})

1. **[场景覆盖]** 未发现幂等场景
   - 建议：添加重复操作的幂等性检查场景

---

请修复上述问题后重新检查。
```

## 检查规则参考

详细规范见：`openspec/templates/SPEC_GUIDELINES.md`

## 约束规则

- 检查失败时，明确指出问题和位置
- 提供具体的修复建议
- 区分错误（必须修复）和警告（建议修复）
