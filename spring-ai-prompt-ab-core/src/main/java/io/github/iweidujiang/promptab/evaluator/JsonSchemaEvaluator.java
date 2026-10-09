package io.github.iweidujiang.promptab.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * JSON 结构评估器
 * <p>
 * 校验 LLM 输出是否为合法 JSON，并检查是否包含指定的必需字段。
 * 评分规则：合法 JSON 得 0.5 分，每个缺失的必需字段扣减 (0.5 / 必需字段数)。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
public class JsonSchemaEvaluator implements Evaluator {

    private static final Logger log = LoggerFactory.getLogger(JsonSchemaEvaluator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final List<String> requiredFields;

    public JsonSchemaEvaluator(List<String> requiredFields) {
        this.requiredFields = requiredFields != null ? List.copyOf(requiredFields) : List.of();
    }

    @Override
    public String name() {
        return "json_schema";
    }

    @Override
    public double evaluate(EvaluationContext context) {
        String output = context.getOutput();
        if (output == null || output.isBlank()) {
            return 0.0;
        }

        JsonNode node;
        try {
            node = objectMapper.readTree(output);
        } catch (Exception e) {
            log.debug("JSON 解析失败：{}", e.getMessage());
            return 0.0;
        }

        if (requiredFields.isEmpty()) {
            return 1.0;
        }

        long missingCount = requiredFields.stream()
                .filter(field -> !node.has(field))
                .count();

        double penalty = (double) missingCount / requiredFields.size() * 0.5;
        return Math.max(0.0, 1.0 - penalty);
    }
}
