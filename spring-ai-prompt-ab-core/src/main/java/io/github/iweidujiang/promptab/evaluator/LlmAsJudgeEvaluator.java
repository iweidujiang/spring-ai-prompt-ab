package io.github.iweidujiang.promptab.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * LLM-as-Judge 评估器
 * <p>
 * 使用 LLM 对 Prompt 输出进行评分。评分 Prompt 模板可配置，
 * 要求评分模型返回 JSON 格式，包含 "score" 字段（0.0-1.0）。
 * <p>
 * 评分 Prompt 模板中可使用以下变量：
 * - {input}：用户输入
 * - {output}：LLM 输出
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
public class LlmAsJudgeEvaluator implements Evaluator {

    private static final Logger log = LoggerFactory.getLogger(LlmAsJudgeEvaluator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DEFAULT_PROMPT = """
            你是一个评估专家。请对以下 AI 助手的回答进行评分。

            用户问题：
            {input}

            AI 回答：
            {output}

            请根据回答的准确性、完整性和有用性，给出 0.0 到 1.0 之间的分数。
            只返回 JSON 格式：{"score": 分数}
            """;

    private final String evaluatorName;
    private final ChatModel chatModel;
    private final String promptTemplate;

    public LlmAsJudgeEvaluator(ChatModel chatModel) {
        this("llm_judge", chatModel, DEFAULT_PROMPT);
    }

    public LlmAsJudgeEvaluator(String evaluatorName, ChatModel chatModel, String promptTemplate) {
        this.evaluatorName = evaluatorName;
        this.chatModel = chatModel;
        this.promptTemplate = promptTemplate;
    }

    @Override
    public String name() {
        return evaluatorName;
    }

    @Override
    public double evaluate(EvaluationContext context) {
        try {
            String scoringPrompt = buildScoringPrompt(context);
            ChatResponse response = chatModel.call(new Prompt(scoringPrompt));

            if (response == null || response.getResult() == null) {
                log.warn("LLM-as-Judge 评分模型返回空响应");
                return 0.0;
            }

            String content = response.getResult().getOutput().getText();
            return parseScore(content);
        } catch (Exception e) {
            log.warn("LLM-as-Judge 评估失败：{}", e.getMessage(), e);
            return 0.0;
        }
    }

    private String buildScoringPrompt(EvaluationContext context) {
        String input = context.getInput() != null ? context.getInput() : "";
        String output = context.getOutput() != null ? context.getOutput() : "";
        return promptTemplate
                .replace("{input}", input)
                .replace("{output}", output);
    }

    private double parseScore(String content) {
        if (content == null || content.isBlank()) {
            return 0.0;
        }

        try {
            JsonNode node = objectMapper.readTree(content);
            if (node.has("score")) {
                double score = node.get("score").asDouble();
                return Math.max(0.0, Math.min(1.0, score));
            }
        } catch (Exception e) {
            log.debug("解析评分 JSON 失败，尝试从文本提取：{}", content);
        }

        // 尝试从文本中提取数字
        try {
            String trimmed = content.trim();
            double score = Double.parseDouble(trimmed);
            return Math.max(0.0, Math.min(1.0, score));
        } catch (NumberFormatException e) {
            log.warn("无法从评分模型输出中解析分数：{}", content);
            return 0.0;
        }
    }
}
