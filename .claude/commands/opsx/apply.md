---
name: "OPSX: Apply"
description: 从 OpenSpec 变更中实现任务（实验性）
category: Workflow
tags: [workflow, artifacts, experimental]
---

从 OpenSpec 变更中实现任务。

**输入**：可选择指定变更名称（例如 `/opsx:apply add-auth`）。若未指定，检查是否能从对话上下文中推断。若模糊不清，必须提示用户选择可用的变更。

**步骤**

1. **选择变更**

   若提供了名称，直接使用。否则：
   - 若用户在对话中提及了某个变更，则从上下文推断
   - 若只有一个活跃变更，则自动选择
   - 若存在歧义，运行 `openspec list --json` 获取可用变更，并使用 **AskUserQuestion tool** 让用户选择

   始终声明："Using change: <name>"，并说明如何覆盖（例如 `/opsx:apply <other>`）。

2. **检查状态以了解 schema**
   ```bash
   openspec status --change "<name>" --json
   ```
   解析 JSON 以了解：
   - `schemaName`：正在使用的工作流（例如 "spec-driven"）
   - 哪个 artifact 包含任务（spec-driven 通常为 "tasks"，其他情况请查看 status）

3. **获取 apply 指令**

   ```bash
   openspec instructions apply --change "<name>" --json
   ```

   返回内容：
   - 上下文文件路径（因 schema 而异）
   - 进度（总数、已完成、剩余）
   - 带状态的任务列表
   - 基于当前状态的动态指令

   **处理各状态：**
   - 若 `state: "blocked"`（缺少 artifacts）：显示消息，建议使用 `/opsx:continue`
   - 若 `state: "all_done"`：表示祝贺，建议归档
   - 其他情况：继续实现

4. **读取上下文文件**

   读取 apply 指令输出中 `contextFiles` 列出的文件。
   具体文件取决于所用 schema：
   - **spec-driven**：proposal、specs、design、tasks
   - 其他 schema：按 CLI 输出中的 contextFiles 操作

5. **显示当前进度**

   展示：
   - 正在使用的 schema
   - 进度："N/M tasks complete"
   - 剩余任务概览
   - 来自 CLI 的动态指令

6. **实现任务（循环直至完成或受阻）**

   对每个待处理任务：
   - 显示正在处理哪个任务
   - 进行所需的代码更改
   - 保持更改最小化且聚焦
   - 在任务文件中将任务标记为完成：`- [ ]` → `- [x]`
   - 继续下一个任务

   **在以下情况暂停：**
   - 任务不明确 → 请求澄清
   - 实现揭示了设计问题 → 建议更新 artifacts
   - 遇到错误或阻碍 → 上报并等待指导
   - 用户中断

7. **完成或暂停时，显示状态**

   展示：
   - 本次会话完成的任务
   - 总体进度："N/M tasks complete"
   - 若全部完成：建议归档
   - 若暂停：解释原因并等待指导

**实现过程中的输出**

```
## Implementing: <change-name> (schema: <schema-name>)

Working on task 3/7: <task description>
[...implementation happening...]
✓ Task complete

Working on task 4/7: <task description>
[...implementation happening...]
✓ Task complete
```

**完成时的输出**

```
## Implementation Complete

**Change:** <change-name>
**Schema:** <schema-name>
**Progress:** 7/7 tasks complete ✓

### Completed This Session
- [x] Task 1
- [x] Task 2
...

All tasks complete! You can archive this change with `/opsx:archive`.
```

**暂停时的输出（遇到问题）**

```
## Implementation Paused

**Change:** <change-name>
**Schema:** <schema-name>
**Progress:** 4/7 tasks complete

### Issue Encountered
<description of the issue>

**Options:**
1. <option 1>
2. <option 2>
3. Other approach

What would you like to do?
```

**约束规则**
- 持续处理任务直至完成或受阻
- 开始前始终读取上下文文件（来自 apply 指令输出）
- 若任务不明确，暂停并询问后再实现
- 若实现揭示问题，暂停并建议更新 artifacts
- 代码更改保持最小化，限定在每个任务范围内
- 完成每个任务后立即更新任务复选框
- 遇到错误、阻碍或需求不清时暂停——不要猜测
- 使用 CLI 输出中的 contextFiles，不要假设特定文件名

**流式工作流集成**

此 skill 支持"对变更执行操作"模型：

- **可随时调用**：在所有 artifacts 完成之前（若任务已存在）、部分实现之后、与其他操作交替进行
- **允许更新 artifacts**：若实现揭示设计问题，建议更新 artifacts——不要锁定阶段，灵活工作
