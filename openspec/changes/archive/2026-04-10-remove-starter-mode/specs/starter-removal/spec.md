# Starter 模式移除

本变更移除 Starter 模式相关模块，为结构性清理，无新增或修改的能力规格。

---

## ADDED Requirements

无新增需求。

## MODIFIED Requirements

无修改的需求。

## REMOVED Requirements

### Requirement: Starter 模式自动装配

**Reason**: Agent 模式功能完全覆盖 Starter 模式，且接入更简单（仅需 JVM 参数）

**Migration**: 使用 Agent 模式替代：
1. 移除 `gray-trace-spring-boot-starter` 依赖
2. 添加 `gray-trace-agent` JVM 参数：`-javaagent:path/to/gray-trace-agent.jar`

### Requirement: Spring Boot 自动配置

**Reason**: 随 Starter 模式移除

**Migration**: Agent 模式通过字节码插桩实现，无需 Spring Boot 自动配置

### Requirement: BeanPostProcessor 拦截

**Reason**: Agent 模式使用 ByteBuddy Advice 实现拦截，无需 BeanPostProcessor

**Migration**: Agent 模式自动处理 RestTemplate、OkHttp 等 HTTP 客户端
