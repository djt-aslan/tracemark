---
name: "OPSX: Propose"
description: 提出新变更——一步创建并生成所有 artifacts
category: Workflow
tags: [workflow, artifacts, experimental]
---

提出新变更——一步创建变更并生成所有 artifacts。

我将创建一个包含以下 artifacts 的变更：
- proposal.md（是什么 & 为什么）
- design.md（如何做）
- tasks.md（实现步骤）

准备好实现后，运行 /opsx:apply

---

**输入**：`/opsx:propose` 后面的参数是变更名称（kebab-case），或用户想要构建的内容描述。

**步骤**

1. **若未提供输入，询问他们想构建什么**

   使用 **AskUserQuestion tool**（开放式，无预设选项）询问：
   > "What change do you want to work on? Describe what you want to build or fix."

   根据他们的描述，推导出一个 kebab-case 名称（例如 "add user authentication" → `add-user-auth`）。

   **重要**：在不理解用户想要构建什么之前，不要继续。

2. **创建变更目录**
   ```bash
   openspec new change "<name>"
   ```
   这将在 `openspec/changes/<name>/` 创建一个带有 `.openspec.yaml` 的脚手架变更。

3. **获取 artifact 构建顺序**
   ```bash
   openspec status --change "<name>" --json
   ```
   解析 JSON 以获取：
   - `applyRequires`：实现前所需的 artifact ID 数组（例如 `["tasks"]`）
   - `artifacts`：所有 artifacts 的列表，包含其状态和依赖关系

4. **按顺序创建 artifacts，直至准备好 apply**

   使用 **TodoWrite tool** 追踪 artifacts 的进度。

   按依赖顺序循环处理 artifacts（优先处理没有待处理依赖的 artifacts）：

   a. **对每个状态为 `ready`（依赖已满足）的 artifact**：
      - 获取指令：
        ```bash
        openspec instructions <artifact-id> --change "<name>" --json
        ```
      - 指令 JSON 包含：
        - `context`：项目背景（对你的约束——不要包含在输出中）
        - `rules`：特定于 artifact 的规则（对你的约束——不要包含在输出中）
        - `template`：输出文件使用的结构
        - `instruction`：针对此 artifact 类型的 schema 特定指导
        - `outputPath`：写入 artifact 的位置
        - `dependencies`：供参考的已完成 artifacts
      - 读取所有已完成的依赖文件以获取上下文
      - 使用 `template` 作为结构创建 artifact 文件
      - 将 `context` 和 `rules` 作为约束应用——但不要将其复制到文件中
      - 显示简短进度："Created <artifact-id>"

   b. **继续直至所有 `applyRequires` artifacts 完成**
      - 每次创建 artifact 后，重新运行 `openspec status --change "<name>" --json`
      - 检查 `applyRequires` 中的每个 artifact ID 在 artifacts 数组中是否都有 `status: "done"`
      - 当所有 `applyRequires` artifacts 完成时停止

   c. **若某个 artifact 需要用户输入**（上下文不明确）：
      - 使用 **AskUserQuestion tool** 澄清
      - 然后继续创建

5. **显示最终状态**
   ```bash
   openspec status --change "<name>"
   ```

**输出**

完成所有 artifacts 后，进行摘要：
- 变更名称和位置
- 已创建的 artifacts 列表及简短描述
- 就绪状态："All artifacts created! Ready for implementation."
- 提示："Run `/opsx:apply` to start implementing."

**Artifact 创建指南**

- 遵循每个 artifact 类型的 `openspec instructions` 中的 `instruction` 字段
- schema 定义了每个 artifact 应包含的内容——遵循它
- 创建新 artifact 前先读取依赖 artifacts 获取上下文
- 使用 `template` 作为输出文件的结构——填写其各节
- **重要**：`context` 和 `rules` 是对你的约束，不是文件内容
  - 不要将 `<context>`、`<rules>`、`<project_context>` 块复制到 artifact 中
  - 它们指导你写什么，但不应出现在输出中

**约束规则**
- 创建实现所需的所有 artifacts（由 schema 的 `apply.requires` 定义）
- 创建新 artifact 之前始终读取依赖 artifacts
- 若上下文极其不清楚，询问用户——但倾向于做出合理决策以保持势头
- 若同名变更已存在，询问用户是想继续它还是创建新的
- 写入后验证每个 artifact 文件确实存在，再继续处理下一个
