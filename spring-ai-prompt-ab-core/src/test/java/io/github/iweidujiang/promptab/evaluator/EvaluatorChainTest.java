package io.github.iweidujiang.promptab.evaluator;

import io.github.iweidujiang.promptab.domain.MetricEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * EvaluatorChain 单元测试
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
@ExtendWith(MockitoExtension.class)
class EvaluatorChainTest {

    @Mock
    private MetricEventRepository metricEventRepository;

    private EvaluationContext context;

    @BeforeEach
    void setUp() {
        context = new EvaluationContext();
        context.setExperimentKey("test-exp");
        context.setVariantKey("variant-a");
        context.setSessionId("session-1");
        context.setInput("你好");
        context.setOutput("你好！有什么可以帮你的？");
        context.setLatencyMs(150L);
        context.setInputTokens(10);
        context.setOutputTokens(20);
    }

    /**
     * 所有评估器正常执行，分数分别写入 metric_event
     */
    @Test
    void evaluate_allEvaluatorsSucceed_savesAllScores() {
        Evaluator evaluator1 = new StubEvaluator("keyword", 0.8);
        Evaluator evaluator2 = new StubEvaluator("latency", 0.9);

        EvaluatorChain chain = new EvaluatorChain(List.of(evaluator1, evaluator2), metricEventRepository);
        chain.evaluate(context);

        ArgumentCaptor<MetricEvent> captor = ArgumentCaptor.forClass(MetricEvent.class);
        verify(metricEventRepository, times(2)).save(captor.capture());

        List<MetricEvent> events = captor.getAllValues();
        assertEquals(2, events.size());

        assertEquals("keyword", events.get(0).getEvaluatorName());
        assertEquals(0.8, events.get(0).getScore());
        assertEquals("test-exp", events.get(0).getExperimentKey());
        assertEquals("variant-a", events.get(0).getVariantKey());

        assertEquals("latency", events.get(1).getEvaluatorName());
        assertEquals(0.9, events.get(1).getScore());
    }

    /**
     * 单个评估器失败不影响其他评估器执行
     */
    @Test
    void evaluate_oneEvaluatorFails_othersStillExecute() {
        Evaluator failingEvaluator = new FailingEvaluator("broken");
        Evaluator goodEvaluator = new StubEvaluator("keyword", 0.7);

        EvaluatorChain chain = new EvaluatorChain(List.of(failingEvaluator, goodEvaluator), metricEventRepository);
        chain.evaluate(context);

        ArgumentCaptor<MetricEvent> captor = ArgumentCaptor.forClass(MetricEvent.class);
        verify(metricEventRepository, times(1)).save(captor.capture());

        MetricEvent event = captor.getValue();
        assertEquals("keyword", event.getEvaluatorName());
        assertEquals(0.7, event.getScore());
    }

    /**
     * 空评估器列表不报错
     */
    @Test
    void evaluate_emptyEvaluators_noError() {
        EvaluatorChain chain = new EvaluatorChain(Collections.emptyList(), metricEventRepository);
        chain.evaluate(context);

        verifyNoInteractions(metricEventRepository);
    }

    /**
     * MetricEvent 的上下文字段正确填充
     */
    @Test
    void evaluate_metricEventFieldsPopulatedFromContext() {
        Evaluator evaluator = new StubEvaluator("test-eval", 0.5);

        EvaluatorChain chain = new EvaluatorChain(List.of(evaluator), metricEventRepository);
        chain.evaluate(context);

        ArgumentCaptor<MetricEvent> captor = ArgumentCaptor.forClass(MetricEvent.class);
        verify(metricEventRepository).save(captor.capture());

        MetricEvent event = captor.getValue();
        assertEquals("test-exp", event.getExperimentKey());
        assertEquals("variant-a", event.getVariantKey());
        assertEquals("session-1", event.getSessionId());
        assertEquals(0.5, event.getScore());
        assertEquals("test-eval", event.getEvaluatorName());
        assertEquals(150L, event.getLatencyMs());
        assertEquals(10, event.getInputTokens());
        assertEquals(20, event.getOutputTokens());
        assertNotNull(event.getTimestamp());
    }

    /**
     * 注入 MeterRegistry 后，评估分数通过 DistributionSummary 记录
     */
    @Test
    void evaluate_withMeterRegistry_recordsScoreMetrics() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        Evaluator evaluator1 = new StubEvaluator("keyword", 0.8);
        Evaluator evaluator2 = new StubEvaluator("latency", 0.6);

        EvaluatorChain chain = new EvaluatorChain(List.of(evaluator1, evaluator2), metricEventRepository, meterRegistry);
        chain.evaluate(context);

        // 验证 keyword 评估器的分数记录
        double keywordMean = meterRegistry.find("promptab.evaluation.score")
                .tag("evaluatorName", "keyword")
                .summary().mean();
        assertEquals(0.8, keywordMean, 0.001);

        // 验证 latency 评估器的分数记录
        double latencyMean = meterRegistry.find("promptab.evaluation.score")
                .tag("evaluatorName", "latency")
                .summary().mean();
        assertEquals(0.6, latencyMean, 0.001);
    }

    /**
     * 未注入 MeterRegistry 时不报错（向后兼容）
     */
    @Test
    void evaluate_withoutMeterRegistry_doesNotThrow() {
        Evaluator evaluator = new StubEvaluator("test-eval", 0.5);

        EvaluatorChain chain = new EvaluatorChain(List.of(evaluator), metricEventRepository);
        chain.evaluate(context);

        verify(metricEventRepository).save(any(MetricEvent.class));
    }

    private static class StubEvaluator implements Evaluator {
        private final String name;
        private final double score;

        StubEvaluator(String name, double score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public double evaluate(EvaluationContext context) {
            return score;
        }
    }

    private static class FailingEvaluator implements Evaluator {
        private final String name;

        FailingEvaluator(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public double evaluate(EvaluationContext context) {
            throw new RuntimeException("模拟评估器异常");
        }
    }
}
