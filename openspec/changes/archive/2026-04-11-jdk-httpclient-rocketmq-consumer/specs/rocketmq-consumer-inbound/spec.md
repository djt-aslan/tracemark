## ADDED Requirements

### Requirement: RocketMQ Consumer 消息消费时恢复灰度上下文

当 RocketMQ Consumer 消费消息时，Agent SHALL 从消息的 UserProperty 中提取灰度标签并设置到 GrayContext。

#### Scenario: 消息包含灰度标签时恢复上下文
- **WHEN** 消息的 UserProperty 包含 `grayTag=gray-v1`
- **AND** Consumer 开始处理该消息
- **THEN** GrayContext.get() SHALL 返回 "gray-v1"

#### Scenario: 消息不包含灰度标签时设置稳定标签
- **WHEN** 消息的 UserProperty 不包含 grayTag
- **OR** grayTag 值为 null 或空
- **THEN** GrayContext.get() SHALL 返回 "stable"

#### Scenario: 消息处理完成后清理上下文
- **WHEN** 消息处理完成
- **THEN** GrayContext SHALL 被清理（防止线程池复用时污染）

#### Scenario: 配置开关关闭时不恢复
- **WHEN** 配置 `gray.trace.mq.consumer` 为 false
- **OR** 配置 `gray.trace.mq.enabled` 为 false
- **THEN** Agent SHALL NOT 恢复灰度上下文

### Requirement: RocketMQ Consumer 日志输出

当成功恢复灰度上下文时，Agent SHALL 输出链路追踪日志（若日志开关开启）。

#### Scenario: 日志开关开启时输出
- **WHEN** 配置 `gray.trace.log.enabled` 为 true
- **AND** 成功从消息恢复灰度标签
- **THEN** SHALL 输出包含灰度标签、消息 Topic、线程名的日志

### Requirement: RocketMQ Consumer 支持所有消费模式

Agent SHALL 支持 Push 和 Pull 两种消费模式。

#### Scenario: Push 消费模式支持
- **WHEN** 使用 DefaultMQPushConsumer 消费消息
- **THEN** 灰度上下文 SHALL 正确恢复

#### Scenario: Pull 消费模式支持
- **WHEN** 使用 DefaultMQPullConsumer 消费消息
- **THEN** 灰度上下文 SHALL 正确恢复
