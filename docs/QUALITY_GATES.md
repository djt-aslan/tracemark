# 质量卡点机制

本文档定义了 tracemark 项目的质量卡点机制，确保代码质量和规格一致性。

---

## 1. 卡点概览

```
/opsx:propose
     │
     ▼
┌─────────────┐
│  卡点 1     │  Spec 规约检查
│ Spec Check  │
└──────┬──────┘
       │
       ▼
/writing-plans
     │
     ▼
/executing-plans
     │
     ├── Task 完成 → git commit
     │                    │
     │                    ▼
     │              ┌──────────┐
     │              │  卡点 2  │ 代码质量检查
     │              │Code Check│
     │              └────┬─────┘
     │                   │
     │                   ▼
     │              ┌──────────┐
     │              │  卡点 3  │ 性能风险检查
     │              │Perf Check│
     │              └────┬─────┘
     │                   │
     ▼                   ▼
/opsx:archive
```

---

## 2. 卡点 1：Spec 规约检查

### 触发时机

`/writing-plans` 执行之前自动触发。

### 检查内容

| 检查项 | 说明 |
|-------|------|
| Requirement 结构 | 每个 Requirement 有 SHALL 行为描述 |
| Scenario 数量 | 每个 Requirement 至少有 2 个 Scenario |
| Scenario 格式 | 使用 WHEN-THEN 结构 |
| 术语格式 | 技术术语使用反引号包裹 |
| 场景覆盖 | 覆盖正向、空值、配置、幂等场景 |

### 检查命令

```bash
/check-spec
```

### 检查失败处理

- 阻止 `/writing-plans` 执行
- 显示具体检查项和失败原因
- 提示修复建议

---

## 3. 卡点 2：代码质量检查

### 触发时机

`git commit` 执行之前自动触发。

### 检查内容

| 检查项 | 说明 |
|-------|------|
| 测试通过 | 所有单元测试通过 |
| 覆盖率达标 | 行覆盖率、分支覆盖率满足阈值 |
| Spec 场景覆盖 | 每个 Spec 场景有对应测试用例 |

### 覆盖率阈值

| 模块 | 行覆盖率 | 分支覆盖率 |
|-----|---------|-----------|
| gray-trace-agent | 90% | 85% |
| gray-trace-spring-boot-autoconfigure | 85% | 80% |
| gray-trace-core | 80% | 75% |

### 检查命令

```bash
/check-code
```

### 检查失败处理

- 阻止 `git commit` 执行
- 显示具体检查项和失败原因
- 提示修复建议

---

## 4. 卡点 3：性能风险检查

### 触发时机

`git commit` 执行之前，在代码质量检查之后自动触发。

### 检查内容

| 风险类别 | 严重程度 | 检测内容 |
|---------|---------|---------|
| 线程池问题 | 🔴 高危 | 线程泄漏、拒绝策略、队列溢出、配置不当 |
| 锁竞争/死锁 | 🔴 高危 | 锁粒度过粗、嵌套锁、死锁风险模式 |
| 内存泄漏 | 🔴 高危 | ThreadLocal 未清理、集合无限增长、资源未释放 |
| 同步阻塞 | 🟡 中危 | 异步上下文中的阻塞调用、IO 阻塞 |
| 连接池问题 | 🟡 中危 | 连接泄漏、超时配置、池大小不当 |
| 上下文传递 | 🟡 中危 | 线程池上下文丢失、TTL 使用不当 |

### 检查命令

```bash
/check-perf
```

### 检查失败处理

- 🔴 高危问题：阻止提交，必须修复
- 🟡 中危问题：建议修复，可标记为 TODO
- 🟢 低危问题：作为优化建议，不阻止提交

### 输出示例

```markdown
## 性能风险分析报告

### 概览

| 严重程度 | 数量 | 说明 |
|---------|------|------|
| 🔴 高危 | 1 | 可能导致系统故障或严重性能下降 |
| 🟡 中危 | 2 | 高负载下可能出现问题 |
| 🟢 低危 | 0 | 建议优化，影响较小 |

---

### 🔴 高危问题

#### 1. GrayContext.java:32 - ThreadLocal 未清理

**代码片段：**
```java
GrayContext.set(tag);
// 业务逻辑
// 缺少 GrayContext.clear()
```

**风险说明：**
线程池复用时，ThreadLocal 值残留导致内存泄漏和数据错乱。

**修复建议：**
```java
try {
    GrayContext.set(tag);
    // 业务逻辑
} finally {
    GrayContext.clear();
}
```
```

---

## 5. 配置方式

### 5.1 安装 Hooks

在 `.claude/settings.json` 中配置：

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Skill tool with skill: writing-plans",
        "hooks": ["/check-spec"]
      },
      {
        "matcher": "Bash tool with command matching: git commit",
        "hooks": ["/check-code", "/check-perf"]
      }
    ]
  }
}
```

### 5.2 手动检查

团队成员也可手动触发检查：

```bash
# Spec 规约检查
/check-spec

# 代码质量检查
/check-code

# 性能风险检查
/check-perf
```

---

## 6. 检查流程

### 卡点 1 执行流程

```
用户执行 /writing-plans
        │
        ├── Hook 拦截
        │       │
        │       ▼
        │   读取当前变更的 spec.md
        │       │
        │       ▼
        │   执行检查规则
        │       │
        │       ├── 失败 → 显示错误，阻止继续
        │       └── 通过 → 允许执行 /writing-plans
        │
        └── 执行 /writing-plans
```

### 卡点 2 执行流程

```
用户执行 git commit
        │
        ├── Hook 拦截
        │       │
        │       ▼
        │   运行测试 mvn test
        │       │
        │       ▼
        │   检查覆盖率报告
        │       │
        │       ▼
        │   检查 Spec 场景覆盖
        │       │
        │       ├── 失败 → 显示错误，阻止提交
        │       └── 通过 → 继续性能检查
        │
        └── 执行 git commit
```

### 卡点 3 执行流程

```
代码质量检查通过
        │
        ▼
    获取变更文件
        │
        ▼
    代码模式扫描
        │
        ├── 线程池风险检测
        ├── 锁竞争/死锁检测
        ├── 内存泄漏检测
        ├── 同步阻塞检测
        └── 上下文传递检测
        │
        ▼
    生成风险报告
        │
        ├── 高危问题 > 0 → 阻止提交
        └── 无高危问题 → 允许提交
```

---

## 7. 绕过检查

**不推荐**，但在特殊情况下可绕过：

```bash
# 绕过代码检查提交
git commit --no-verify
```

> ⚠️ 注意：绕过检查可能导致代码质量问题，需在 PR 审查时补充检查。

---

## 8. 相关文档

| 文档 | 用途 |
|-----|------|
| `openspec/templates/SPEC_GUIDELINES.md` | Spec 编写规范 |
| `openspec/templates/SPEC_TEMPLATE.md` | Spec 模板 |
| `.claude/skills/check-perf/references/` | 性能风险检测模式库 |
