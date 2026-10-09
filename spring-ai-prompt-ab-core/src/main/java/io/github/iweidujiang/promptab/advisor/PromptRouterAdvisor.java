package io.github.iweidujiang.promptab.advisor;

import io.github.iweidujiang.promptab.domain.Variant;
import io.github.iweidujiang.promptab.evaluator.EvaluationContext;
import io.github.iweidujiang.promptab.evaluator.EvaluatorChain;
import io.github.iweidujiang.promptab.experiment.VariantRepository;
import io.github.iweidujiang.promptab.logging.ConfigurableLogger;
import io.github.iweidujiang.promptab.router.PromptRouter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prompt 路由 Advisor
 * <p>
 * 在 ChatClient 调用链中拦截请求，根据路由策略选择 Prompt 变体，
 * 替换原始 PromptTemplate 后继续执行。
 * 调用完成后触发评估器链，将评估分数写入 metric_event。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
public class PromptRouterAdvisor implements CallAdvisor {

    private static final String CONTEXT_VARIANT_KEY = "promptab.variantKey";
    private static final String CONTEXT_SESSION_ID = "sessionId";
    private static final String MDC_SESSION_ID = "promptab.sessionId";
    private static final String MDC_EXPERIMENT_KEY = "promptab.experimentKey";
    private static final String MDC_VARIANT_KEY = "promptab.variantKey";

    private final PromptRouter promptRouter;
    private final VariantRepository variantRepository;
    private final String experimentKey;
    private final EvaluatorChain evaluatorChain;
    private final MeterRegistry meterRegistry;
    private final ConfigurableLogger logger;

    public PromptRouterAdvisor(PromptRouter promptRouter,
                               VariantRepository variantRepository,
                               String experimentKey) {
        this(promptRouter, variantRepository, experimentKey, null, null, "INFO");
    }

    public PromptRouterAdvisor(PromptRouter promptRouter,
                               VariantRepository variantRepository,
                               String experimentKey,
                               EvaluatorChain evaluatorChain) {
        this(promptRouter, variantRepository, experimentKey, evaluatorChain, null, "INFO");
    }

    public PromptRouterAdvisor(PromptRouter promptRouter,
                               VariantRepository variantRepository,
                               String experimentKey,
                               EvaluatorChain evaluatorChain,
                               MeterRegistry meterRegistry) {
        this(promptRouter, variantRepository, experimentKey, evaluatorChain, meterRegistry, "INFO");
    }

    public PromptRouterAdvisor(PromptRouter promptRouter,
                               VariantRepository variantRepository,
                               String experimentKey,
                               EvaluatorChain evaluatorChain,
                               MeterRegistry meterRegistry,
                               String logLevel) {
        this.promptRouter = promptRouter;
        this.variantRepository = variantRepository;
        this.experimentKey = experimentKey;
        this.evaluatorChain = evaluatorChain;
        this.meterRegistry = meterRegistry;
        this.logger = new ConfigurableLogger(
                LoggerFactory.getLogger(PromptRouterAdvisor.class), logLevel);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return "PromptRouterAdvisor";
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> routerContext = buildRouterContext(request);
        String sessionId = (String) routerContext.get(CONTEXT_SESSION_ID);

        MDC.put(MDC_SESSION_ID, sessionId);
        MDC.put(MDC_EXPERIMENT_KEY, experimentKey);

        try {
            String variantKey = promptRouter.route(experimentKey, routerContext);
            MDC.put(MDC_VARIANT_KEY, variantKey);

            logger.log("路由决策：experimentKey={}, variantKey={}, sessionId={}",
                    experimentKey, variantKey, sessionId);

            recordRoutingDecision(experimentKey, variantKey);

            Variant variant = findVariant(variantKey);
            if (variant == null) {
                throw new IllegalStateException("找不到变体: " + variantKey);
            }

            Prompt originalPrompt = request.prompt();
            Prompt modifiedPrompt = originalPrompt.augmentUserMessage(variant.getPromptTemplate());
            ChatClientRequest modifiedRequest = request.mutate().prompt(modifiedPrompt).build();

            ChatClientResponse response = chain.nextCall(modifiedRequest);

            long latencyMs = System.currentTimeMillis() - startTime;
            logger.log("路由完成：experimentKey={}, variantKey={}, sessionId={}, latencyMs={}",
                    experimentKey, variantKey, sessionId, latencyMs);

            recordRoutingLatency(experimentKey, variantKey, latencyMs);

            if (evaluatorChain != null) {
                EvaluationContext evalContext = buildEvaluationContext(
                        request, variant, variantKey, sessionId, latencyMs, response);
                try {
                    evaluatorChain.evaluate(evalContext);
                } catch (Exception e) {
                    logger.warn("评估器链执行异常（不影响主链路）：{}", e.getMessage(), e);
                }
            }

            return response.mutate().context(CONTEXT_VARIANT_KEY, variantKey).build();
        } finally {
            MDC.remove(MDC_SESSION_ID);
            MDC.remove(MDC_EXPERIMENT_KEY);
            MDC.remove(MDC_VARIANT_KEY);
        }
    }

    private void recordRoutingDecision(String experimentKey, String variantKey) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("promptab.routing.decisions")
                .tag("experimentKey", experimentKey)
                .tag("variantKey", variantKey)
                .register(meterRegistry)
                .increment();
    }

    private void recordRoutingLatency(String experimentKey, String variantKey, long latencyMs) {
        if (meterRegistry == null) {
            return;
        }
        Timer.builder("promptab.routing.latency")
                .tag("experimentKey", experimentKey)
                .tag("variantKey", variantKey)
                .register(meterRegistry)
                .record(Duration.ofMillis(latencyMs));
    }

    private EvaluationContext buildEvaluationContext(ChatClientRequest request,
                                                      Variant variant,
                                                      String variantKey,
                                                      String sessionId,
                                                      long latencyMs,
                                                      ChatClientResponse response) {
        EvaluationContext context = new EvaluationContext();
        context.setExperimentKey(experimentKey);
        context.setVariantKey(variantKey);
        context.setSessionId(sessionId);
        context.setInput(request.prompt().getUserMessage().getText());
        context.setOutput(response.chatResponse() != null
                ? response.chatResponse().getResult().getOutput().getText()
                : "");
        context.setLatencyMs(latencyMs);
        // token 统计暂从 metadata 中获取，后续可完善
        context.setInputTokens(0);
        context.setOutputTokens(0);
        return context;
    }

    private Map<String, Object> buildRouterContext(ChatClientRequest request) {
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> requestContext = request.context();
        if (requestContext != null && requestContext.containsKey(CONTEXT_SESSION_ID)) {
            context.put(CONTEXT_SESSION_ID, requestContext.get(CONTEXT_SESSION_ID));
        } else {
            context.put(CONTEXT_SESSION_ID, String.valueOf(request.hashCode()));
        }
        return context;
    }

    private Variant findVariant(String variantKey) {
        List<Variant> variants = variantRepository.findActiveByExperimentKey(experimentKey);
        return variants.stream()
                .filter(v -> v.getVariantKey().equals(variantKey))
                .findFirst()
                .orElse(null);
    }
}
