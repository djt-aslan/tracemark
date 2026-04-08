## 1. Maven 配置调整

- [x] 1.1 修改父 `pom.xml` 中 `java.version` 从 11 改为 8
- [x] 1.2 更新 `maven-compiler-plugin` 配置确保 `source` 和 `target` 为 8
- [x] 1.3 验证 ByteBuddy 版本 1.14.18 支持 JDK 8（查阅官方文档确认）
- [x] 1.4 验证 TTL 版本 2.14.5 支持 JDK 8（查阅官方文档确认）

## 2. 编译验证

- [x] 2.1 使用 JDK 8 编译器执行 `mvn clean compile` 验证编译成功
- [x] 2.2 检查生成的 `.class` 文件字节码版本为 52.0（使用 `javap -verbose` 或 bytecode viewer）
- [x] 2.3 执行 `mvn clean package` 生成 Agent JAR
- [x] 2.4 验证 Agent JAR Manifest 中 Premain-Class 正确配置

## 3. 测试验证

- [x] 3.1 在 JDK 8 环境执行 `mvn test` 验证所有测试通过
- [x] 3.2 确认无因 JDK 版本差异导致的测试失败

## 4. CI 配置更新（可选）

- [ ] 4.1 添加 JDK 8 构建任务到 CI 配置文件
- [ ] 4.2 配置 JDK 8 测试任务，确保测试在目标环境运行
