package io.github.iweidujiang.promptab.evaluator;

/**
 * 延迟评估器
 * <p>
 * 根据 LLM 调用延迟评分。延迟在阈值内得满分，超过阈值后线性递减，
 * 达到阈值的 2 倍时得 0 分。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
public class LatencyEvaluator implements Evaluator {

    private final long thresholdMs;

    /**
     * @param thresholdMs 延迟阈值（毫秒），低于此值得满分 1.0
     */
    public LatencyEvaluator(long thresholdMs) {
        this.thresholdMs = thresholdMs;
    }

    @Override
    public String name() {
        return "latency";
    }

    @Override
    public double evaluate(EvaluationContext context) {
        long latencyMs = context.getLatencyMs();

        if (latencyMs <= thresholdMs) {
            return 1.0;
        }

        long maxLatency = thresholdMs * 2;
        if (latencyMs >= maxLatency) {
            return 0.0;
        }

        double excess = latencyMs - thresholdMs;
        double range = maxLatency - thresholdMs;
        return Math.max(0.0, 1.0 - excess / range);
    }
}
