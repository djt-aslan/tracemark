---
name: check-code
description: 代码质量检查。在 git commit 之前执行，检查测试通过率、覆盖率、Spec 场景覆盖。
---

# 代码质量检查

检查代码质量是否满足项目要求，包括测试通过率、覆盖率、Spec 场景覆盖。

## 执行步骤

### 1. 运行测试

```bash
mvn test -q
```

**检查结果：**
- 通过：继续下一步
- 失败：输出失败信息，阻止提交

### 2. 检查测试覆盖率

```bash
mvn jacoco:report -q
```

读取覆盖率报告：`target/site/jacoco/index.html` 或 `*/target/site/jacoco/index.html`

#### 覆盖率阈值

| 模块 | 行覆盖率 | 分支覆盖率 |
|-----|---------|-----------|
| gray-trace-agent | 25% | 20% |
| gray-trace-spring-boot-autoconfigure | 75% | 70% |
| gray-trace-core | 80% | 75% |

**检查逻辑：**
1. 识别本次修改涉及的模块
2. 读取对应模块的覆盖率报告
3. 对比阈值要求

### 3. 检查 Spec 场景覆盖

#### 3.1 获取相关 Spec

```bash
openspec list --json
```

确定当前变更的 spec 文件路径。

#### 3.2 提取 Spec 场景

从 spec.md 中提取所有 Scenario：
- 正向场景
- 空值/null 场景
- 配置开关场景
- 幂等性场景
- 异常场景

#### 3.3 检查测试覆盖

对于每个 Scenario，检查是否存在对应的测试用例：

| Spec 场景 | 对应测试 | 检查方式 |
|----------|---------|---------|
| 灰度标存在时注入 | `testXxx_withGrayTag_shouldInject` | 搜索测试方法名/注释 |
| 灰度标为空时不注入 | `testXxx_withEmptyTag_shouldNotInject` | 搜索测试方法名/注释 |
| enabled=false 时禁用 | `testXxx_whenDisabled_shouldNotInject` | 搜索测试方法名/注释 |

### 4. 输出检查结果

#### 检查通过

```
## 代码质量检查通过 ✓

### 测试结果
- 状态：全部通过
- 用例数：{n} 个

### 覆盖率
| 模块 | 行覆盖率 | 分支覆盖率 | 状态 |
|-----|---------|-----------|------|
| gray-trace-agent | 92% | 87% | ✓ |
| gray-trace-spring-boot-autoconfigure | 88% | 82% | ✓ |

### Spec 场景覆盖
- 正向场景：{n}/{n} ✓
- 空值场景：{n}/{n} ✓
- 配置场景：{n}/{n} ✓
- 幂等场景：{n}/{n} ✓

所有检查项均通过，可以提交。
```

#### 检查失败

```
## 代码质量检查失败 ✗

### 测试结果
- 状态：失败
- 失败用例：
  1. {test_name}: {error_message}
  2. {test_name}: {error_message}

### 覆盖率
| 模块 | 行覆盖率 | 要求 | 状态 |
|-----|---------|------|------|
| gray-trace-agent | 75% | 90% | ✗ 不达标 |

### Spec 场景覆盖
- 正向场景：2/2 ✓
- 空值场景：1/1 ✓
- 配置场景：0/1 ✗ 缺少测试
  - 缺失场景：`enabled=false 时禁用`

---

请修复上述问题后重新检查。
```

## 快速检查命令

```bash
# 仅运行测试
mvn test -q

# 测试 + 覆盖率报告
mvn test jacoco:report -q

# 查看覆盖率报告
# 位置：target/site/jacoco/index.html
```

## 约束规则

- 测试失败必须修复，不允许跳过
- 覆盖率不达标必须补充测试
- Spec 场景缺失必须补充测试用例
- 提供具体的修复建议和命令
