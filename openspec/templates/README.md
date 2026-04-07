# OpenSpec 模板目录

本目录包含 spec 编写的模板和规范文档，供团队成员参考使用。

## 文档列表

| 文档 | 用途 | 适用场景 |
|-----|------|---------|
| [SPEC_GUIDELINES.md](SPEC_GUIDELINES.md) | Spec 编写规范 | 详细了解编写规则、格式要求、场景覆盖 |
| [SPEC_TEMPLATE.md](SPEC_TEMPLATE.md) | Spec 模板集合 | 复制模板快速开始编写 |
| [SPEC_QUICK_REFERENCE.md](SPEC_QUICK_REFERENCE.md) | 快速参考卡片 | 打印查阅、快速确认格式 |

## 快速开始

### 1. 新建 Spec 文件

在 `openspec/specs/{feature-name}/` 目录下创建 `spec.md` 文件。

### 2. 选择合适模板

根据功能类型选择模板：

- **Spring Boot 功能** → 使用 Spring Boot 功能模板
- **Java Agent 功能** → 使用 Agent 模式模板
- **多版本支持** → 使用多版本支持模板
- **异步/并发场景** → 使用异步场景模板

### 3. 填写内容

参考 [SPEC_GUIDELINES.md](SPEC_GUIDELINES.md) 中的规范填写。

### 4. 检查完整性

使用 [SPEC_QUICK_REFERENCE.md](SPEC_QUICK_REFERENCE.md) 中的检查清单确认。

## 示例参考

完整示例请查看：

- `openspec/specs/apache-http-client-outbound/spec.md` - Spring Boot + Agent 双模式
- `openspec/specs/completable-future-async/spec.md` - 异步场景

## 目录结构

```
openspec/
├── config.yaml              # OpenSpec 配置
├── templates/               # 模板目录（本目录）
│   ├── README.md           # 本文件
│   ├── SPEC_GUIDELINES.md  # 编写规范
│   ├── SPEC_TEMPLATE.md    # 模板集合
│   └── SPEC_QUICK_REFERENCE.md  # 快速参考
├── specs/                   # 活跃 spec 目录
│   └── {feature-name}/
│       └── spec.md
└── changes/                 # 变更归档目录
    └── archive/
        └── {date}-{feature-name}/
            ├── proposal.md
            ├── design.md
            ├── tasks.md
            └── specs/
                └── {feature-name}/
                    └── spec.md
```
