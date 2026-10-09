package io.github.iweidujiang.promptab.evaluator;

import io.github.iweidujiang.promptab.domain.MetricEvent;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评估器编排链
 * <p>
 * 按顺序执行多个 Evaluator，将每个评估器的分数写入 metric_event。
 * 单个评估器失败不影响其他评估器的执行。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
public class EvaluatorChain {

    private static final Logger log = LoggerFactory.getLogger(EvaluatorChain.class);

    private final List<Evaluator> evaluators;
    private final MetricEventRepository metricEventRepository;
    private final MeterRegistry meterRegistry;

    public EvaluatorChain(List<Evaluator> evaluators, MetricEventRepository metricEventRepository) {
        this(evaluators, metricEventRepository, null);
    }

    public EvaluatorChain(List<Evaluator> evaluators, MetricEventRepository metricEventRepository,
                          MeterRegistry meterRegistry) {
        this.evaluators = evaluators;
        this.metricEventRepository = metricEventRepository;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 执行所有评估器
     *
     * @param context 评估上下文
     */
    public void evaluate(EvaluationContext context) {
        for (Evaluator evaluator : evaluators) {
            try {
                double score = evaluator.evaluate(context);
                log.info("评估完成：evaluator={}, score={}, experimentKey={}, variantKey={}",
                        evaluator.name(), score, context.getExperimentKey(), context.getVariantKey());

                MetricEvent event = buildMetricEvent(context, evaluator.name(), score);
                metricEventRepository.save(event);

                recordEvaluationScore(evaluator.name(), score);
            } catch (Exception e) {
                log.warn("评估器执行失败（不影响其他评估器）：evaluator={}, error={}",
                        evaluator.name(), e.getMessage(), e);
            }
        }
    }

    private void recordEvaluationScore(String evaluatorName, double score) {
        if (meterRegistry == null) {
            return;
        }
        DistributionSummary.builder("promptab.evaluation.score")
                .tag("evaluatorName", evaluatorName)
                .baseUnit("score")
                .register(meterRegistry)
                .record(score);
    }

    private MetricEvent buildMetricEvent(EvaluationContext context, String evaluatorName, double score) {
        MetricEvent event = new MetricEvent();
        event.setExperimentKey(context.getExperimentKey());
        event.setVariantKey(context.getVariantKey());
        event.setSessionId(context.getSessionId());
        event.setScore(score);
        event.setEvaluatorName(evaluatorName);
        event.setLatencyMs(context.getLatencyMs());
        event.setInputTokens(context.getInputTokens());
        event.setOutputTokens(context.getOutputTokens());
        event.setTimestamp(LocalDateTime.now());
        return event;
    }
}
