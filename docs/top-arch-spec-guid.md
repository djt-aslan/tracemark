flowchart TD
    A[完整大PRD] --> B[抽取：架构摘要<br/>定位+业务域+核心流程+非功能需求]
    B --> C[AI 生成 global-arch.yml<br/>全局架构宪法]
    
    C --> D[全局规约归档<br/>/opsx:propose → /opsx:archive]
    D --> E[项目统一基线：领域/状态/ID/错误码/安全/接口规范]
    
    A --> F[按业务域 拆小功能任务]
    F --> G[抽取：单个功能PRD片段]
    
    G --> H[生成功能Spec<br/>/opsx:propose --ref global-arch.yml]
    H --> I[/writing-plans]
    I --> J[executing-plans]
    J --> K[功能规约归档<br/>/opsx:archive]
    
    E --> H
    K --> L[循环迭代：所有功能统一架构、不乱扩展]、