package io.github.iweidujiang.promptab.integration;

import io.github.iweidujiang.promptab.evaluator.EvaluationContext;
import io.github.iweidujiang.promptab.evaluator.EvaluatorChain;
import io.github.iweidujiang.promptab.evaluator.JdbcMetricEventRepository;
import io.github.iweidujiang.promptab.evaluator.KeywordEvaluator;
import io.github.iweidujiang.promptab.evaluator.LatencyEvaluator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 评估链路集成测试
 * <p>
 * 使用真实 MySQL 容器验证：
 * 1. 评估器执行后 metric_event 表有记录
 * 2. 分数在 [0.0, 1.0] 范围内
 * 3. 不同评估器的评分逻辑正确
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-10
 */
class EvaluationIntegrationTest extends BaseIntegrationTest {

    private static final String EXPERIMENT_KEY = "eval-test-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String VARIANT_KEY = "variant-a";
    private static final String SESSION_ID = "session-eval-001";

    private JdbcMetricEventRepository metricEventRepository;

    @BeforeEach
    void setUp() {
        metricEventRepository = new JdbcMetricEventRepository(getJdbcTemplate());
    }

    @AfterEach
    void cleanUp() {
        getJdbcTemplate().update(
                "DELETE FROM ab_metric_event WHERE experiment_key = ?", EXPERIMENT_KEY);
    }

    /**
     * 关键词评估器：输出包含必需关键词 → 得分 1.0，metric_event 表有记录
     */
    @Test
    void evaluate_keywordEvaluator_passingOutput_persistsMetricEvent() {
        KeywordEvaluator keywordEvaluator = new KeywordEvaluator(
                List.of("你好"), List.of());
        EvaluatorChain chain = new EvaluatorChain(
                List.of(keywordEvaluator), metricEventRepository);

        EvaluationContext context = buildContext("测试输入", "你好，我是AI助手");
        chain.evaluate(context);

        // 验证 metric_event 表有记录
        Integer count = getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM ab_metric_event WHERE experiment_key = ?",
                Integer.class, EXPERIMENT_KEY);
        assertEquals(1, count, "应插入 1 条 metric_event 记录");

        // 验证分数在合理范围
        Double score = getJdbcTemplate().queryForObject(
                "SELECT score FROM ab_metric_event WHERE experiment_key = ? AND evaluator_name = ?",
                Double.class, EXPERIMENT_KEY, "keyword");
        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0, "分数应在 [0.0, 1.0] 范围内");
        assertEquals(1.0, score, "输出包含必需关键词，应得满分");
    }

    /**
     * 关键词评估器：输出缺少必需关键词 → 得分 0.0
     */
    @Test
    void evaluate_keywordEvaluator_missingKeyword_scoresZero() {
        KeywordEvaluator keywordEvaluator = new KeywordEvaluator(
                List.of("你好"), List.of());
        EvaluatorChain chain = new EvaluatorChain(
                List.of(keywordEvaluator), metricEventRepository);

        EvaluationContext context = buildContext("测试输入", "今天天气不错");
        chain.evaluate(context);

        Double score = getJdbcTemplate().queryForObject(
                "SELECT score FROM ab_metric_event WHERE experiment_key = ? AND evaluator_name = ?",
                Double.class, EXPERIMENT_KEY, "keyword");
        assertNotNull(score);
        assertEquals(0.0, score, "输出缺少必需关键词，应得 0 分");
    }

    /**
     * 延迟评估器：延迟低于阈值 → 得分 1.0
     */
    @Test
    void evaluate_latencyEvaluator_belowThreshold_scoresFull() {
        LatencyEvaluator latencyEvaluator = new LatencyEvaluator(1000);
        EvaluatorChain chain = new EvaluatorChain(
                List.of(latencyEvaluator), metricEventRepository);

        EvaluationContext context = buildContext("测试输入", "输出内容");
        context.setLatencyMs(500);
        chain.evaluate(context);

        Double score = getJdbcTemplate().queryForObject(
                "SELECT score FROM ab_metric_event WHERE experiment_key = ? AND evaluator_name = ?",
                Double.class, EXPERIMENT_KEY, "latency");
        assertNotNull(score);
        assertEquals(1.0, score, "延迟低于阈值，应得满分");
    }

    /**
     * 多评估器链路：两个评估器都执行，各生成一条 metric_event
     */
    @Test
    void evaluate_multipleEvaluators_eachPersistsOwnMetricEvent() {
        KeywordEvaluator keywordEvaluator = new KeywordEvaluator(
                List.of("AI"), List.of());
        LatencyEvaluator latencyEvaluator = new LatencyEvaluator(2000);
        EvaluatorChain chain = new EvaluatorChain(
                List.of(keywordEvaluator, latencyEvaluator), metricEventRepository);

        EvaluationContext context = buildContext("问题", "AI助手回答");
        context.setLatencyMs(800);
        chain.evaluate(context);

        // 验证总记录数
        Integer totalCount = getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM ab_metric_event WHERE experiment_key = ?",
                Integer.class, EXPERIMENT_KEY);
        assertEquals(2, totalCount, "2 个评估器应各生成 1 条记录");

        // 验证每个评估器的分数都在合理范围
        List<Double> scores = getJdbcTemplate().queryForList(
                "SELECT score FROM ab_metric_event WHERE experiment_key = ?",
                Double.class, EXPERIMENT_KEY);
        assertEquals(2, scores.size());
        for (Double s : scores) {
            assertTrue(s >= 0.0 && s <= 1.0, "分数应在 [0.0, 1.0] 范围内");
        }

        // 关键词评估器：输出包含 "AI" → 1.0
        Double keywordScore = getJdbcTemplate().queryForObject(
                "SELECT score FROM ab_metric_event WHERE experiment_key = ? AND evaluator_name = ?",
                Double.class, EXPERIMENT_KEY, "keyword");
        assertEquals(1.0, keywordScore);

        // 延迟评估器：800ms < 2000ms 阈值 → 1.0
        Double latencyScore = getJdbcTemplate().queryForObject(
                "SELECT score FROM ab_metric_event WHERE experiment_key = ? AND evaluator_name = ?",
                Double.class, EXPERIMENT_KEY, "latency");
        assertEquals(1.0, latencyScore);
    }

    /**
     * findStats 聚合查询：验证平均分和样本数
     */
    @Test
    void findStats_afterMultipleEvaluations_returnsAggregatedResults() {
        KeywordEvaluator keywordEvaluator = new KeywordEvaluator(
                List.of("AI"), List.of());
        EvaluatorChain chain = new EvaluatorChain(
                List.of(keywordEvaluator), metricEventRepository);

        // 第一次：通过（包含 "AI"）
        EvaluationContext context1 = buildContext("问题1", "AI回答");
        chain.evaluate(context1);

        // 第二次：不通过（不包含 "AI"）
        EvaluationContext context2 = buildContext("问题2", "普通回答");
        chain.evaluate(context2);

        var stats = metricEventRepository.findStats(EXPERIMENT_KEY, VARIANT_KEY);
        assertEquals(2, stats.sampleCount(), "应有 2 个样本");
        assertEquals(0.5, stats.avgScore(), 0.001, "平均分应为 0.5（1.0 + 0.0）/ 2");
    }

    private EvaluationContext buildContext(String input, String output) {
        EvaluationContext context = new EvaluationContext();
        context.setExperimentKey(EXPERIMENT_KEY);
        context.setVariantKey(VARIANT_KEY);
        context.setSessionId(SESSION_ID);
        context.setInput(input);
        context.setOutput(output);
        context.setLatencyMs(100);
        context.setInputTokens(10);
        context.setOutputTokens(20);
        return context;
    }
}
