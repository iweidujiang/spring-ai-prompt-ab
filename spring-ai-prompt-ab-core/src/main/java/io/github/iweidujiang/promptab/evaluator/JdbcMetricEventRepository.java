package io.github.iweidujiang.promptab.evaluator;

import io.github.iweidujiang.promptab.domain.MetricEvent;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 基于 JdbcTemplate 的指标事件仓储实现
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
public class JdbcMetricEventRepository implements MetricEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMetricEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(MetricEvent event) {
        String sql = """
                INSERT INTO ab_metric_event (experiment_key, variant_key, session_id, score, evaluator_name, latency_ms, input_tokens, output_tokens, timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime now = event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now();
        jdbcTemplate.update(sql,
                event.getExperimentKey(),
                event.getVariantKey(),
                event.getSessionId(),
                event.getScore(),
                event.getEvaluatorName(),
                event.getLatencyMs(),
                event.getInputTokens(),
                event.getOutputTokens(),
                Timestamp.valueOf(now));
    }

    @Override
    public MetricStats findStats(String experimentKey, String variantKey) {
        String avgSql = """
                SELECT AVG(score) AS avg_score, COUNT(*) AS cnt
                FROM ab_metric_event
                WHERE experiment_key = ? AND variant_key = ?
                """;
        var avgResult = jdbcTemplate.query(avgSql, (rs, rowNum) ->
                new AvgResult(rs.getDouble("avg_score"), rs.getInt("cnt")),
                experimentKey, variantKey);

        if (avgResult.isEmpty() || avgResult.get(0).count() == 0) {
            return new MetricStats(0.0, 0L, 0);
        }

        double avgScore = avgResult.get(0).avgScore();
        int count = avgResult.get(0).count();

        String latencySql = """
                SELECT latency_ms FROM ab_metric_event
                WHERE experiment_key = ? AND variant_key = ?
                ORDER BY latency_ms ASC
                """;
        List<Long> latencies = jdbcTemplate.queryForList(latencySql, Long.class,
                experimentKey, variantKey);

        long p99 = computeP99(latencies);
        return new MetricStats(avgScore, p99, count);
    }

    private long computeP99(List<Long> sortedLatencies) {
        if (sortedLatencies.isEmpty()) {
            return 0L;
        }
        int index = (int) Math.ceil(0.99 * sortedLatencies.size()) - 1;
        index = Math.max(0, Math.min(index, sortedLatencies.size() - 1));
        return sortedLatencies.get(index);
    }

    private record AvgResult(double avgScore, int count) {
    }
}
