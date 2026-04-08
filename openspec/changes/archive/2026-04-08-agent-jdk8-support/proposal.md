## Why

当前开发环境使用 JDK 11 编译，但生产环境运行在 JDK 8。Java Agent 需要在生产环境以 `-javaagent` 方式部署，Agent JAR 的字节码版本必须与目标 JVM 匹配（JDK 8 无法加载 JDK 11 编译的类）。现在需要调整项目配置和依赖，确保 Agent JAR 可以在 JDK 8 环境正常运行。

## What Changes

- **BREAKING**: 将 Java 编译目标从 JDK 11 降级到 JDK 8
- 调整 `pom.xml` 中的 `java.version` 从 11 改为 8
- 验证 ByteBuddy 版本兼容性（1.14.x 支持 JDK 8+）
- 验证 TTL（TransmittableThreadLocal）版本兼容性（2.14.x 支持 JDK 8+）
- 更新 maven-compiler-plugin 配置
- 添加 CI 构建验证，确保 JDK 8 编译通过
- 运行完整测试套件验证功能不受影响

## Capabilities

### New Capabilities

- `jdk8-build-support`: JDK 8 编译和构建支持，包括 Maven 配置调整和 CI 验证

### Modified Capabilities

- 无（现有 Apache HttpClient、CompletableFuture 等 spec 的需求不变，仅编译目标调整）

## Impact

- **Affected Code**: 所有模块的 `pom.xml`（父 pom 和子模块）
- **Affected APIs**: 无 API 变化，仅编译目标调整
- **Dependencies**: ByteBuddy 1.14.x、TTL 2.14.x 均支持 JDK 8，无需降级
- **Build**: 需要使用 JDK 8 或 JDK 11（带 `-source 8 -target 8`）进行编译
- **CI**: 需要添加 JDK 8 构建验证步骤