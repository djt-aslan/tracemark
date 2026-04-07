# tracemark 开发规范

本文档定义了项目特定的开发规则，AI 助手应遵循这些规则。

---

## 1. 测试覆盖率要求

### 最低覆盖率

| 模块 | 行覆盖率 | 分支覆盖率 |
|-----|---------|-----------|
| gray-trace-agent | 90% | 85% |
| gray-trace-spring-boot-autoconfigure | 85% | 80% |
| gray-trace-core | 80% | 75% |

### 必须覆盖的测试场景

| 场景类型 | Agent 模式 | Spring Boot 模式 |
|---------|-----------|-----------------|
| 正向场景 | ✓ | ✓ |
| 空值/null 场景 | ✓ | ✓ |
| 配置开关场景 | ✓ | ✓ |
| 幂等性场景 | ✓ | ✓ |
| 异常场景 | ✓ | ✓ |

### 覆盖率命令

```bash
mvn test jacoco:report
# 报告位置：target/site/jacoco/index.html
```

---

## 2. 双模式开发

tracemark 支持 Spring Boot Starter 和 Java Agent 两种模式：

```
需要修改字节码？
    ├── 是 → Agent 模式（Advice + Transformer + ByteBuddy）
    └── 否 → Spring Boot 模式（Interceptor + BeanPostProcessor + @Conditional）
```

### Spec 编写要求

- 双模式功能需分别编写 Requirement
- 使用 `## Agent 模式` 分隔 Agent 相关规格
- 每个模式需覆盖完整场景

### Agent 模式必须使用 TDD

```
1. 先写测试（测试会编译失败）
2. 运行测试确认失败
3. 实现最小代码使测试通过
4. 重构优化
5. Commit
```

---

## 3. Spec 场景覆盖要求

每个 Requirement 必须覆盖：

| 场景类型 | 说明 | 示例 |
|---------|------|------|
| 正向场景 | 正常条件下的行为 | 灰度标存在时注入 |
| 空值场景 | null/空值处理 | 灰度标为空时不注入 |
| 配置场景 | 开关控制 | enabled=false 时禁用 |
| 幂等场景 | 重复操作 | 拦截器不重复注入 |

详细规范见 `openspec/templates/SPEC_GUIDELINES.md`

---

## 4. 提交前检查

- [ ] 所有测试通过
- [ ] 测试覆盖率达标
- [ ] 所有 Spec 场景有对应测试
