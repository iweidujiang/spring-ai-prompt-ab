package io.github.iweidujiang.promptab.evaluator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonSchemaEvaluator 单元测试
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
class JsonSchemaEvaluatorTest {

    /**
     * 合法 JSON 且包含所有必需字段，得满分
     */
    @Test
    void evaluate_validJsonWithAllFields_returnsFullScore() {
        var evaluator = new JsonSchemaEvaluator(List.of("name", "age"));
        var context = buildContext("{\"name\":\"Alice\",\"age\":30}");

        assertEquals(1.0, evaluator.evaluate(context));
    }

    /**
     * 合法 JSON 但缺少部分必需字段，扣分
     */
    @Test
    void evaluate_validJsonMissingFields_returnsPartialScore() {
        var evaluator = new JsonSchemaEvaluator(List.of("name", "age", "email"));
        var context = buildContext("{\"name\":\"Alice\"}");

        double score = evaluator.evaluate(context);
        assertEquals(1.0 - (2.0 / 3.0 * 0.5), score, 0.001);
    }

    /**
     * 合法 JSON 无必需字段要求，得满分
     */
    @Test
    void evaluate_validJsonNoRequiredFields_returnsFullScore() {
        var evaluator = new JsonSchemaEvaluator(List.of());
        var context = buildContext("{\"anything\":\"value\"}");

        assertEquals(1.0, evaluator.evaluate(context));
    }

    /**
     * 非法 JSON，得零分
     */
    @Test
    void evaluate_invalidJson_returnsZero() {
        var evaluator = new JsonSchemaEvaluator(List.of("name"));
        var context = buildContext("这不是 JSON");

        assertEquals(0.0, evaluator.evaluate(context));
    }

    /**
     * 空输出，得零分
     */
    @Test
    void evaluate_emptyOutput_returnsZero() {
        var evaluator = new JsonSchemaEvaluator(List.of("name"));
        var context = buildContext("");

        assertEquals(0.0, evaluator.evaluate(context));
    }

    /**
     * null 输出，得零分
     */
    @Test
    void evaluate_nullOutput_returnsZero() {
        var evaluator = new JsonSchemaEvaluator(List.of("name"));
        var context = buildContext(null);

        assertEquals(0.0, evaluator.evaluate(context));
    }

    private EvaluationContext buildContext(String output) {
        var context = new EvaluationContext();
        context.setOutput(output);
        return context;
    }
}
