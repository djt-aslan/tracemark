## Context

当前项目结构：

```
tracemark/
├── gray-trace-core/                        # 核心模块
├── gray-trace-agent/                       # Agent 模式
├── gray-trace-spring-boot-autoconfigure/   # Starter 自动配置（待删除）
├── gray-trace-spring-boot-starter/         # Starter 依赖聚合（待删除）
└── gray-trace-test/                        # 示例应用（待删除）
```

Starter 模式通过 Spring Boot 自动装配实现灰度标传递，Agent 模式通过 ByteBuddy 字节码插桩实现。两者功能重复，但 Agent 模式更完整：

| 功能 | Starter | Agent |
|------|---------|-------|
| Servlet 入口 | ✅ Filter | ✅ Advice |
| RestTemplate | ✅ Interceptor | ✅ Advice |
| OkHttp | ✅ Interceptor | ✅ Advice |
| Apache HttpClient | ✅ Interceptor | ✅ Advice |
| CompletableFuture | ❌ | ✅ Advice |
| Spring Boot 3.x | ❌ | ✅ |

## Goals / Non-Goals

**Goals:**
- 删除 Starter 相关的 3 个模块
- 更新项目文档，移除 Starter 相关内容
- 简化项目结构，只保留 Agent 模式

**Non-Goals:**
- 不修改 Agent 模式的任何实现
- 不修改 gray-trace-core 的功能代码（仅更新注释）

## Decisions

### 决策 1：直接删除模块而非废弃

**选择：** 直接删除 `gray-trace-spring-boot-starter`、`gray-trace-spring-boot-autoconfigure`、`gray-trace-test` 模块

**理由：**
- 项目尚未正式发布，无向后兼容负担
- 简化代码库，避免维护无用代码
- Agent 模式功能完全覆盖 Starter 模式

**备选方案：** 标记为 `@Deprecated` 并保留一个版本周期
- 否决原因：增加维护成本，且用户迁移成本低

### 决策 2：保留 GrayProperties 中的配置项

**选择：** 保留 `GrayProperties` 中所有嵌套配置类（Servlet、RestTemplate、OkHttp 等）

**理由：**
- Agent 模式同样使用这些配置项
- 配置项用于控制 Agent 的插桩行为
- 删除会破坏 Agent 模式的功能

## Risks / Trade-offs

**风险 1：使用 Starter 模式的用户需要迁移**
→ 缓解：Agent 模式接入更简单（仅添加 JVM 参数），迁移成本低

**风险 2：文档遗漏更新**
→ 缓解：按 checklist 逐个文件检查更新