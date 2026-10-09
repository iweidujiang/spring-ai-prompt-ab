package io.github.iweidujiang.promptab.evaluator;

import io.github.iweidujiang.promptab.domain.MetricEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MetricEventRepository 集成测试（H2 内存库）
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
class JdbcMetricEventRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcMetricEventRepository repository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .build();
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
                CREATE TABLE ab_metric_event (
                    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                    experiment_key  VARCHAR(128)  NOT NULL,
                    variant_key     VARCHAR(128)  NOT NULL,
                    session_id      VARCHAR(256),
                    score           DOUBLE,
                    evaluator_name  VARCHAR(128),
                    latency_ms      BIGINT,
                    input_tokens    INT,
                    output_tokens   INT,
                    timestamp       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX idx_experiment_variant_ts ON ab_metric_event (experiment_key, variant_key, timestamp)
                """);

        repository = new JdbcMetricEventRepository(jdbcTemplate);
    }

    /**
     * 保存指标事件后可通过聚合查询获取统计信息
     */
    @Test
    void save_and_findStats_returnsCorrectAggregation() {
        MetricEvent event = new MetricEvent();
        event.setExperimentKey("test-exp");
        event.setVariantKey("variant-a");
        event.setSessionId("session-1");
        event.setScore(0.85);
        event.setEvaluatorName("keyword");
        event.setLatencyMs(120L);
        event.setInputTokens(50);
        event.setOutputTokens(100);
        event.setTimestamp(LocalDateTime.of(2026, 10, 9, 10, 0));

        repository.save(event);

        MetricStats stats = repository.findStats("test-exp", "variant-a");

        assertEquals(1, stats.sampleCount());
        assertEquals(0.85, stats.avgScore(), 0.001);
        assertEquals(120L, stats.p99LatencyMs());
    }

    /**
     * 多条事件聚合：平均分和 P99 延迟计算正确
     */
    @Test
    void findStats_multipleEvents_computesAvgAndP99() {
        for (int i = 0; i < 100; i++) {
            MetricEvent event = new MetricEvent();
            event.setExperimentKey("test-exp");
            event.setVariantKey("variant-a");
            event.setScore(0.5 + (i * 0.005));
            event.setEvaluatorName("latency");
            event.setLatencyMs(10L + i);
            event.setTimestamp(LocalDateTime.of(2026, 10, 9, 10, 0));
            repository.save(event);
        }

        MetricStats stats = repository.findStats("test-exp", "variant-a");

        assertEquals(100, stats.sampleCount());
        double expectedAvg = 0.5 + (49 * 0.005);
        assertEquals(expectedAvg, stats.avgScore(), 0.01);
        assertEquals(108L, stats.p99LatencyMs());
    }

    /**
     * 无数据时返回零值统计
     */
    @Test
    void findStats_noData_returnsZeroStats() {
        MetricStats stats = repository.findStats("non-existent", "variant-a");

        assertEquals(0, stats.sampleCount());
        assertEquals(0.0, stats.avgScore());
        assertEquals(0L, stats.p99LatencyMs());
    }

    /**
     * 不同变体的统计数据互不干扰
     */
    @Test
    void findStats_differentVariants_independentStats() {
        MetricEvent eventA = new MetricEvent();
        eventA.setExperimentKey("test-exp");
        eventA.setVariantKey("variant-a");
        eventA.setScore(0.9);
        eventA.setLatencyMs(50L);
        eventA.setTimestamp(LocalDateTime.of(2026, 10, 9, 10, 0));
        repository.save(eventA);

        MetricEvent eventB = new MetricEvent();
        eventB.setExperimentKey("test-exp");
        eventB.setVariantKey("variant-b");
        eventB.setScore(0.3);
        eventB.setLatencyMs(200L);
        eventB.setTimestamp(LocalDateTime.of(2026, 10, 9, 10, 0));
        repository.save(eventB);

        MetricStats statsA = repository.findStats("test-exp", "variant-a");
        MetricStats statsB = repository.findStats("test-exp", "variant-b");

        assertEquals(0.9, statsA.avgScore(), 0.001);
        assertEquals(0.3, statsB.avgScore(), 0.001);
        assertEquals(50L, statsA.p99LatencyMs());
        assertEquals(200L, statsB.p99LatencyMs());
    }
}
