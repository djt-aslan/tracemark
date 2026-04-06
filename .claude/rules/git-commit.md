# Git 提交约束规范

本规则定义了 Git 提交的约束和要求，确保代码质量和一致性。

---

## 1. 提交前检查

### 强制执行

在执行 `git commit` 之前，**必须**完成以下检查：

| 检查项 | 命令 | 说明 |
|-------|------|------|
| 代码质量检查 | `/check-code` | 测试通过、覆盖率达标 |
| 性能风险检查 | `/check-perf` | 无高危性能问题 |

### 执行顺序

```
1. /check-code  → 通过后继续
2. /check-perf  → 无高危问题后继续
3. git commit
```

### 检查失败处理

- 🔴 高危问题：**必须修复**后才能提交
- 🟡 中危问题：建议修复，可标记 TODO 后提交
- 🟢 低危问题：作为优化建议，不阻止提交

---

## 2. 提交信息规范

### 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

| Type | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `style` | 代码格式（不影响功能） |
| `refactor` | 重构（不新增功能、不修复 Bug） |
| `test` | 测试相关 |
| `chore` | 构建、依赖等辅助性变更 |
| `perf` | 性能优化 |

### 示例

```
feat(agent): add TTL wrapper for CompletableFuture async methods

- Add advice for supplyAsync, runAsync, thenApplyAsync
- Ensure gray context propagation in async execution
- Add unit tests for all async method variants

Closes #123
```

---

## 3. 提交粒度

### 原则

- **一个提交解决一个问题**
- 提交粒度适中，便于 code review 和回滚
- 避免巨型提交（超过 500 行变更需拆分）

### 禁止

- ❌ 一次提交包含多个不相关的功能
- ❌ 提交无法编译的代码
- ❌ 提交未测试的代码

---

## 4. 分支管理

### 禁止直接在 master 上提交

**master 分支受保护，禁止直接提交。** 所有变更必须通过 feature 分支开发，再通过 PR 合并。

```
❌ git checkout master && git commit    # 禁止
✅ git checkout -b feature/xxx && git commit && git push && 创建 PR
```

### 分支命名

| 分支类型 | 命名格式 |
|---------|---------|
| 功能分支 | `feature/<feature-name>` |
| 修复分支 | `fix/<bug-name>` |
| 发布分支 | `release/<version>` |
| 热修复分支 | `hotfix/<version>` |

### 合并要求

- 合并前确保所有检查通过
- 合并前进行 Code Review
- 使用 `--no-ff` 保留分支历史

---

## 5. 绕过检查

**不推荐**，仅在紧急情况下使用：

```bash
git commit --no-verify
```

> ⚠️ 使用 `--no-verify` 绕过检查时，必须在 PR 审查时补充检查。

---

## 6. 敏感文件

### 禁止提交

以下文件**禁止提交到 Git**：

- `.env`、`.env.local`、`.env.*.local`
- `credentials.json`、`secrets.json`
- `*.pem`、`*.key`、`*.p12`
- IDE 配置文件（`.idea/`、`*.iml`）
- 构建产物（`target/`、`*.class`）

### 检查方式

```bash
# 提交前检查是否包含敏感文件
git diff --cached --name-only | grep -E '\.(env|pem|key|p12)$'
```

---

## 违规处理

违反提交规范时：
1. 撤销提交：`git reset --soft HEAD~1`
2. 修复问题
3. 重新提交
