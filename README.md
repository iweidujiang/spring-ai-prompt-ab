# spring-ai-prompt-ab

**Prompt A/B 测试框架** —— 让 Prompt 可版本管理、可灰度发布、可量化评估。

基于 Spring AI 2.0 的 Advisor 机制，在 LLM 调用链路中无侵入地接入 A/B 实验能力。

---

## 功能特性

- **流量路由**：基于 sessionId 哈希的粘性路由，同一用户始终命中同一变体
- **实验管理**：REST API 管理实验生命周期（创建 → 激活 → 暂停 → 归档）
- **变体管理**：支持多变体、流量比例动态调整、灰度发布
- **评估框架**：内置关键词 / 延迟 / JSON Schema / LLM-as-Judge 评估器，支持扩展
- **可观测性**：Micrometer 指标 + MDC 结构化日志，路由决策可追踪
- **零侵入**：通过 Spring AI Advisor 接入，业务代码仅需一行配置

---

## 架构总览

```
                          ┌─────────────────────────────────────────────┐
                          │              Spring AI ChatClient            │
                          │                                             │
  用户请求 ──▶ ChatClient ──▶ PromptRouterAdvisor (CallAroundAdvisor)   │
                          │        │                                    │
                          │        ├─① PromptRouter.route(sessionId)    │
                          │        │    └─▶ HashPromptRouter            │
                          │        │         └─▶ VariantRepository (DB) │
                          │        │                                    │
                          │        ├─② 替换 UserMessage 为变体模板       │
                          │        │                                    │
                          │        ├─③ ChatModel.call(substitutedPrompt) │
                          │        │    └─▶ OpenAI / Ollama / Mock      │
                          │        │                                    │
                          │        └─④ EvaluatorChain.evaluate()        │
                          │             ├─▶ KeywordEvaluator             │
                          │             ├─▶ LatencyEvaluator            │
                          │             ├─▶ JsonSchemaEvaluator          │
                          │             ├─▶ LlmAsJudgeEvaluator          │
                          │             └─▶ MetricEventRepository (DB)   │
                          │                                             │
                          └─────────────────────────────────────────────┘
                                            │
                                            ▼
                                  ┌───────────────────┐
                                  │    MySQL / H2      │
                                  │  ab_experiment      │
                                  │  ab_variant         │
                                  │  ab_metric_event    │
                                  └───────────────────┘
```

---

## 核心概念

| 概念 | 说明 |
|------|------|
| **Experiment（实验）** | 一次 A/B 测试，包含唯一标识 `experimentKey` 和生命周期状态（DRAFT → ACTIVE → PAUSED → ARCHIVED） |
| **Variant（变体）** | 实验下的 Prompt 模板版本，包含 `variantKey`、`promptTemplate`、`trafficPct`（流量百分比）和 `isActive`（是否参与路由） |
| **SessionId（会话标识）** | 用于粘性路由的用户标识，相同 sessionId 始终命中同一变体 |
| **PromptRouter（路由器）** | 根据 sessionId 和变体流量比例，决定本次请求使用哪个变体 |
| **Evaluator（评估器）** | 对 LLM 输出进行质量评分的组件，分数写入 `ab_metric_event` 表 |
| **PromptRouterAdvisor** | Spring AI CallAroundAdvisor 实现，串联路由 → 替换 → 调用 → 评估的完整链路 |

---

## 快速启动

### 前置条件

- JDK 21+
- Maven 3.8+
- Docker（仅集成测试需要）

### 运行示例

```bash
# 1. 克隆并编译
git clone https://github.com/iweidujiang/spring-ai-prompt-ab.git
cd spring-ai-prompt-ab
mvn install -DskipTests

# 2. 启动示例工程（H2 内存库，无需 Docker）
java -jar spring-ai-prompt-ab-example/target/spring-ai-prompt-ab-example-0.1.0-SNAPSHOT.jar

# 3. 发送请求
curl -X POST http://localhost:8080/api/demo/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好", "sessionId": "user-001"}'
```

响应示例：

```json
{
  "reply": "收到你的消息：你是一个活泼、友好的AI助手，请用轻松幽默的语气回答用户问题～。这是来自 FakeChatModel 的模拟回复。",
  "sessionId": "user-001",
  "variantKey": "casual"
}
```

> 示例工程预置了 `demo-chat` 实验，包含 `formal`（正式风格）和 `casual`（活泼风格）两个变体，各占 50% 流量。  
> 更换 `sessionId` 可命中不同变体。

---

## 模块结构

```
spring-ai-prompt-ab
├── spring-ai-prompt-ab-core       # 核心领域：路由、评估、实体、Repository
├── spring-ai-prompt-ab-starter    # Spring Boot 自动装配
├── spring-ai-prompt-ab-server     # 实验管理 REST API
├── spring-ai-prompt-ab-example    # 可运行示例工程
├── db/migration                   # Flyway 迁移脚本（MySQL）
└── docs                           # 开发文档（不入版本）
```

### 集成方式

在你的项目中引入 starter 依赖：

```xml
<dependency>
    <groupId>io.github.iweidujiang</groupId>
    <artifactId>spring-ai-prompt-ab-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

确保容器中存在 `ChatModel` Bean（Spring AI 的 OpenAI / Ollama 自动配置均可），Advisor 会自动注册。

在 ChatClient 中使用：

```java
@RestController
public class MyController {

    private final ChatClient chatClient;

    public MyController(ChatModel chatModel, PromptRouterAdvisor advisor) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(advisor)
                .build();
    }

    @PostMapping("/chat")
    public String chat(@RequestBody String message,
                       @RequestParam String sessionId) {
        ChatClientResponse response = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param("sessionId", sessionId))
                .call()
                .chatClientResponse();
        return response.chatResponse().getResult().getOutput().getText();
    }
}
```

---

## 配置项

所有配置项均在 `application.yml` 中通过 `prompt-ab` 前缀管理：

```yaml
prompt-ab:
  enabled: true                    # 是否启用 Prompt AB（默认 true）
  default-routing-strategy: HASH   # 路由策略（目前仅 HASH）
  default-experiment-key: my-exp   # 默认实验标识（调用方未指定时使用）
  metrics-enabled: true            # 是否启用 Micrometer 指标采集（默认 true）
  log-level: INFO                  # 日志级别：TRACE / DEBUG / INFO / WARN / ERROR
```

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `prompt-ab.enabled` | boolean | `true` | 总开关，设为 false 则不注册任何 Bean |
| `prompt-ab.default-routing-strategy` | String | `HASH` | 路由策略，当前仅支持 HASH |
| `prompt-ab.default-experiment-key` | String | - | 默认实验标识，可在 Advisor 构造时覆盖 |
| `prompt-ab.metrics-enabled` | boolean | `true` | 是否采集 Micrometer 指标 |
| `prompt-ab.log-level` | String | `INFO` | 框架日志级别 |

---

## API 文档

### 实验管理

#### 创建实验

```
POST /api/experiments
Content-Type: application/json

{
  "experimentKey": "chat-prompt-v2",
  "description": "聊天场景 Prompt V2 实验"
}
```

响应 `201 Created`：

```json
{
  "id": 1,
  "experimentKey": "chat-prompt-v2",
  "description": "聊天场景 Prompt V2 实验",
  "status": "DRAFT",
  "createdAt": "2026-10-10T10:00:00",
  "updatedAt": "2026-10-10T10:00:00"
}
```

#### 查询实验

```
GET /api/experiments/{key}
```

响应 `200 OK`（同上），实验不存在时返回 `404`。

#### 列表查询

```
GET /api/experiments?status=ACTIVE
```

`status` 参数可选，支持 `DRAFT` / `ACTIVE` / `PAUSED` / `ARCHIVED`，不传则返回全部。

#### 变更状态

```
PATCH /api/experiments/{key}/status
Content-Type: application/json

{
  "status": "ACTIVE"
}
```

状态流转：`DRAFT → ACTIVE → PAUSED → ARCHIVED`，不可逆（ARCHIVED 为终态）。

---

### 变体管理

#### 创建变体

```
POST /api/experiments/{key}/variants
Content-Type: application/json

{
  "variantKey": "formal",
  "promptTemplate": "你是一位专业、严谨的AI助手，请使用正式的语言回答问题。",
  "trafficPct": 50
}
```

响应 `201 Created`：

```json
{
  "id": 1,
  "experimentId": 1,
  "variantKey": "formal",
  "promptTemplate": "你是一位专业、严谨的AI助手，请使用正式的语言回答问题。",
  "trafficPct": 50,
  "isActive": false
}
```

#### 查询变体列表

```
GET /api/experiments/{key}/variants
```

#### 调整流量比例

```
PATCH /api/variants/{id}/traffic
Content-Type: application/json

{
  "trafficPct": 70
}
```

#### 激活 / 停用变体

```
PATCH /api/variants/{id}/activate
Content-Type: application/json

{
  "isActive": true
}
```

> 仅 `isActive=true` 的变体参与路由。所有变体的 `trafficPct` 之和应等于 100。

---

### 错误响应

所有 `400 Bad Request` 返回统一格式：

```json
{
  "message": "实验状态不允许从 DRAFT 直接切换到 ARCHIVED"
}
```

---

## Micrometer 指标

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `promptab.routing.decisions` | Counter | `experimentKey`, `variantKey` | 路由决策次数 |
| `promptab.routing.latency` | Timer | `experimentKey`, `variantKey` | 路由耗时 |
| `promptab.evaluation.score` | Timer | `experimentKey`, `evaluatorName` | 评估耗时 |

---

## 数据库表结构

### ab_experiment（实验表）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `experiment_key` | VARCHAR(128) | NOT NULL, UNIQUE | 实验标识 |
| `description` | VARCHAR(512) | - | 描述 |
| `status` | VARCHAR(32) | NOT NULL, DEFAULT 'DRAFT' | 状态 |
| `created_at` | DATETIME | NOT NULL | 创建时间 |
| `updated_at` | DATETIME | NOT NULL | 更新时间 |

### ab_variant（变体表）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `experiment_id` | BIGINT | NOT NULL, FK | 所属实验 |
| `variant_key` | VARCHAR(128) | NOT NULL | 变体标识 |
| `prompt_template` | TEXT | NOT NULL | Prompt 模板 |
| `traffic_pct` | INT | NOT NULL, DEFAULT 0 | 流量百分比 |
| `is_active` | TINYINT(1) | NOT NULL, DEFAULT 0 | 是否激活 |

### ab_metric_event（指标事件表）

| 列名 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `experiment_key` | VARCHAR(128) | NOT NULL | 实验标识 |
| `variant_key` | VARCHAR(128) | NOT NULL | 变体标识 |
| `session_id` | VARCHAR(256) | - | 会话标识 |
| `score` | DOUBLE | - | 评估分数 |
| `evaluator_name` | VARCHAR(128) | - | 评估器名称 |
| `latency_ms` | BIGINT | - | 延迟（毫秒） |
| `input_tokens` | INT | - | 输入 token 数 |
| `output_tokens` | INT | - | 输出 token 数 |
| `timestamp` | DATETIME | NOT NULL | 事件时间 |

---

## 技术栈

| 组件 | 版本 |
|------|------|
| JDK | 21 |
| Spring Boot | 4.0.8 |
| Spring AI | 2.0.1 |
| MySQL | 8.4（生产）/ H2（示例） |
| Flyway | 迁移管理 |
| Micrometer | 指标采集 |
| Testcontainers | 1.21.4（集成测试） |
| JUnit 5 + Mockito | 单元测试 |

---

## License

[Apache License 2.0](LICENSE)
