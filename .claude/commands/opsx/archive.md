---
name: "OPSX: Archive"
description: 在实验性工作流中归档已完成的变更
category: Workflow
tags: [workflow, archive, experimental]
---

在实验性工作流中归档已完成的变更。

**输入**：可在 `/opsx:archive` 后面可选地指定变更名称（例如 `/opsx:archive add-auth`）。若未指定，检查是否能从对话上下文中推断。若模糊不清，必须提示用户选择可用的变更。

**步骤**

1. **若未提供变更名称，提示用户选择**

   运行 `openspec list --json` 获取可用变更。使用 **AskUserQuestion tool** 让用户选择。

   仅显示活跃变更（尚未归档的）。
   若可用，包含每个变更所用的 schema。

   **重要**：不要猜测或自动选择变更，始终让用户自行选择。

2. **检查 artifact 完成状态**

   运行 `openspec status --change "<name>" --json` 检查 artifact 完成情况。

   解析 JSON 以了解：
   - `schemaName`：正在使用的工作流
   - `artifacts`：带状态（`done` 或其他）的 artifacts 列表

   **若有任何 artifacts 不是 `done`：**
   - 显示警告，列出未完成的 artifacts
   - 提示用户确认是否继续
   - 用户确认后继续

3. **检查任务完成状态**

   读取任务文件（通常为 `tasks.md`）以检查未完成的任务。

   统计标记为 `- [ ]`（未完成）和 `- [x]`（已完成）的任务数量。

   **若发现未完成任务：**
   - 显示警告，显示未完成任务的数量
   - 提示用户确认是否继续
   - 用户确认后继续

   **若不存在任务文件：** 无需任务相关警告，直接继续。

4. **评估 delta spec 同步状态**

   检查 `openspec/changes/<name>/specs/` 中是否存在 delta specs。若不存在，无需同步提示，直接继续。

   **若存在 delta specs：**
   - 将每个 delta spec 与 `openspec/specs/<capability>/spec.md` 中对应的主 spec 进行比较
   - 确定将应用的更改（新增、修改、删除、重命名）
   - 在提示前显示合并摘要

   **提示选项：**
   - 若需要更改："Sync now (recommended)"、"Archive without syncing"
   - 若已同步："Archive now"、"Sync anyway"、"Cancel"

   若用户选择同步，使用 Task tool（subagent_type: "general-purpose"，prompt: "Use Skill tool to invoke openspec-sync-specs for change '<name>'. Delta spec analysis: <include the analyzed delta spec summary>"）。无论用户如何选择，均继续执行归档。

5. **执行归档**

   若归档目录不存在，创建它：
   ```bash
   mkdir -p openspec/changes/archive
   ```

   使用当前日期生成目标名称：`YYYY-MM-DD-<change-name>`

   **检查目标是否已存在：**
   - 若存在：报错失败，建议重命名现有归档或使用不同日期
   - 若不存在：将变更目录移动至归档

   ```bash
   mv openspec/changes/<name> openspec/changes/archive/YYYY-MM-DD-<name>
   ```

6. **显示摘要**

   显示归档完成摘要，包括：
   - 变更名称
   - 所用 schema
   - 归档位置
   - Spec 同步状态（已同步 / 跳过同步 / 无 delta specs）
   - 关于警告的说明（未完成的 artifacts/任务）

**成功时的输出**

```
## Archive Complete

**Change:** <change-name>
**Schema:** <schema-name>
**Archived to:** openspec/changes/archive/YYYY-MM-DD-<name>/
**Specs:** ✓ Synced to main specs

All artifacts complete. All tasks complete.
```

**成功时的输出（无 delta specs）**

```
## Archive Complete

**Change:** <change-name>
**Schema:** <schema-name>
**Archived to:** openspec/changes/archive/YYYY-MM-DD-<name>/
**Specs:** No delta specs

All artifacts complete. All tasks complete.
```

**带警告的成功输出**

```
## Archive Complete (with warnings)

**Change:** <change-name>
**Schema:** <schema-name>
**Archived to:** openspec/changes/archive/YYYY-MM-DD-<name>/
**Specs:** Sync skipped (user chose to skip)

**Warnings:**
- Archived with 2 incomplete artifacts
- Archived with 3 incomplete tasks
- Delta spec sync was skipped (user chose to skip)

Review the archive if this was not intentional.
```

**错误时的输出（归档已存在）**

```
## Archive Failed

**Change:** <change-name>
**Target:** openspec/changes/archive/YYYY-MM-DD-<name>/

Target archive directory already exists.

**Options:**
1. Rename the existing archive
2. Delete the existing archive if it's a duplicate
3. Wait until a different date to archive
```

**约束规则**
- 若未提供变更，始终提示用户选择
- 使用 artifact graph（openspec status --json）进行完成度检查
- 不要因警告而阻止归档——仅通知并确认
- 移动至归档时保留 .openspec.yaml（它会随目录一起移动）
- 清晰展示发生了什么
- 若需要同步，使用 Skill tool 调用 `openspec-sync-specs`（由 agent 驱动）
- 若存在 delta specs，始终运行同步评估并在提示前显示合并摘要
