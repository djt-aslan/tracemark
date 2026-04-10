## Why

项目目前同时维护 Starter 和 Agent 两种模式，增加了维护成本。Agent 模式功能更完整（支持 CompletableFuture、Spring Boot 3.x），且无需修改代码即可接入。决定移除 Starter 模式，只保留 Agent 模式，简化项目结构。

## What Changes

**BREAKING** - 移除 Starter 模式相关模块和文档：

- 删除 `gray-trace-spring-boot-starter` 模块
- 删除 `gray-trace-spring-boot-autoconfigure` 模块（含 42 个 Java 文件）
- 删除 `gray-trace-test` 示例模块（含 9 个 Java 文件）
- 更新 `pom.xml` 移除 3 个 module
- 更新文档移除 Starter 相关内容：
  - `README.md`
  - `docs/INTEGRATION.md`
  - `docs/VERIFICATION.md`
- 更新 `GrayProperties.java` 注释，移除 Starter 相关描述

## Capabilities

### New Capabilities

无新增能力。

### Modified Capabilities

无修改的能力。此变更为项目结构简化，不影响 Agent 模式的现有功能。

## Impact

**删除代码：**
- `gray-trace-spring-boot-starter/` 整个目录
- `gray-trace-spring-boot-autoconfigure/` 整个目录（42 个 Java 文件）
- `gray-trace-test/` 整个目录（9 个 Java 文件）

**修改文件：**
- `pom.xml` - 移除 3 个 module
- `README.md` - 删除 Starter 模式内容
- `docs/INTEGRATION.md` - 删除 Starter 章节
- `docs/VERIFICATION.md` - 删除 Starter 验证结果
- `gray-trace-core/.../GrayProperties.java` - 更新注释

**保留模块：**
- `gray-trace-core` - 核心模块
- `gray-trace-agent` - Agent 模式

**用户影响：**
- 使用 Starter 模式的用户需要迁移到 Agent 模式
- Agent 模式功能完全覆盖 Starter 模式，迁移只需添加 JVM 参数
