## 1. JDK HttpClient 出口注入

- [x] 1.1 创建 JdkHttpClientOutboundAdvice.java
- [x] 1.2 创建 JdkHttpClientOutboundTransformer.java
- [x] 1.3 在 GrayTraceAgent 中注册 JDK HttpClient Transformer
- [x] 1.4 编写 JdkHttpClientOutboundAdviceTest 测试用例

## 2. RocketMQ Consumer 上下文恢复

- [x] 2.1 创建 RocketMqConsumerAdvice.java
- [x] 2.2 创建 RocketMqConsumerTransformer.java
- [x] 2.3 在 GrayTraceAgent 中注册 RocketMQ Consumer Transformer
- [x] 2.4 编写 RocketMqConsumerAdviceTest 测试用例

## 3. 验证与文档

- [x] 3.1 运行全量测试确保无回归
- [x] 3.2 更新 README.md 标注新功能支持状态
- [x] 3.3 更新 INTEGRATION.md 文档
