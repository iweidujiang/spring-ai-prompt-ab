package io.github.iweidujiang.promptab.evaluator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KeywordEvaluator 单元测试
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
class KeywordEvaluatorTest {

    /**
     * 包含所有必需关键词且不包含禁止关键词，得满分
     */
    @Test
    void evaluate_allRequiredPresent_noForbidden_returnsFullScore() {
        var evaluator = new KeywordEvaluator(List.of("你好", "帮助"), List.of("错误"));
        var context = buildContext("你好！有什么可以帮助你的？");

        assertEquals(1.0, evaluator.evaluate(context));
    }

    /**
     * 缺少部分必需关键词，扣分
     */
    @Test
    void evaluate_missingRequired_returnsPartialScore() {
        var evaluator = new KeywordEvaluator(List.of("你好", "帮助", "请"), List.of());
        var context = buildContext("你好！有什么可以做的？");

        double score = evaluator.evaluate(context);
        assertEquals(1.0 / 3.0, score, 0.001);
    }

    /**
     * 包含禁止关键词，扣分
     */
    @Test
    void evaluate_containsForbidden_returnsPartialScore() {
        var evaluator = new KeywordEvaluator(List.of(), List.of("错误", "失败"));
        var context = buildContext("操作出现错误");

        double score = evaluator.evaluate(context);
        assertEquals(0.5, score, 0.001);
    }

    /**
     * 无关键词要求，得满分
     */
    @Test
    void evaluate_noKeywords_returnsFullScore() {
        var evaluator = new KeywordEvaluator(List.of(), List.of());
        var context = buildContext("任意内容");

        assertEquals(1.0, evaluator.evaluate(context));
    }

    /**
     * 空输出，得零分
     */
    @Test
    void evaluate_emptyOutput_returnsZero() {
        var evaluator = new KeywordEvaluator(List.of("你好"), List.of());
        var context = buildContext("");

        assertEquals(0.0, evaluator.evaluate(context));
    }

    /**
     * 必需和禁止混合场景
     */
    @Test
    void evaluate_mixedKeywords_returnsCorrectScore() {
        var evaluator = new KeywordEvaluator(List.of("你好", "帮助"), List.of("错误", "失败"));
        var context = buildContext("你好！有错误需要帮助处理");

        double score = evaluator.evaluate(context);
        assertEquals(3.0 / 4.0, score, 0.001);
    }

    private EvaluationContext buildContext(String output) {
        var context = new EvaluationContext();
        context.setOutput(output);
        return context;
    }
}
