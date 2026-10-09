package io.github.iweidujiang.promptab.evaluator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LatencyEvaluator 单元测试
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
class LatencyEvaluatorTest {

    /**
     * 延迟在阈值内，得满分
     */
    @Test
    void evaluate_latencyBelowThreshold_returnsFullScore() {
        var evaluator = new LatencyEvaluator(1000);
        var context = buildContext(500);

        assertEquals(1.0, evaluator.evaluate(context));
    }

    /**
     * 延迟等于阈值，得满分
     */
    @Test
    void evaluate_latencyEqualsThreshold_returnsFullScore() {
        var evaluator = new LatencyEvaluator(1000);
        var context = buildContext(1000);

        assertEquals(1.0, evaluator.evaluate(context));
    }

    /**
     * 延迟超过阈值但在 2 倍以内，线性递减
     */
    @Test
    void evaluate_latencyExceedsThreshold_returnsPartialScore() {
        var evaluator = new LatencyEvaluator(1000);
        var context = buildContext(1500);

        double score = evaluator.evaluate(context);
        assertEquals(0.5, score, 0.001);
    }

    /**
     * 延迟达到 2 倍阈值，得零分
     */
    @Test
    void evaluate_latencyDoubleThreshold_returnsZero() {
        var evaluator = new LatencyEvaluator(1000);
        var context = buildContext(2000);

        assertEquals(0.0, evaluator.evaluate(context));
    }

    /**
     * 延迟超过 2 倍阈值，得零分
     */
    @Test
    void evaluate_latencyBeyondDoubleThreshold_returnsZero() {
        var evaluator = new LatencyEvaluator(1000);
        var context = buildContext(3000);

        assertEquals(0.0, evaluator.evaluate(context));
    }

    /**
     * 延迟为 0，得满分
     */
    @Test
    void evaluate_zeroLatency_returnsFullScore() {
        var evaluator = new LatencyEvaluator(1000);
        var context = buildContext(0);

        assertEquals(1.0, evaluator.evaluate(context));
    }

    private EvaluationContext buildContext(long latencyMs) {
        var context = new EvaluationContext();
        context.setLatencyMs(latencyMs);
        return context;
    }
}
