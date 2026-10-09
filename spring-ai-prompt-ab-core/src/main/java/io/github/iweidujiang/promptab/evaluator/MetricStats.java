package io.github.iweidujiang.promptab.evaluator;

/**
 * 指标聚合统计
 *
 * @param avgScore    平均分数
 * @param p99LatencyMs P99 延迟（毫秒）
 * @param sampleCount  样本数
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
public record MetricStats(double avgScore, long p99LatencyMs, int sampleCount) {
}
