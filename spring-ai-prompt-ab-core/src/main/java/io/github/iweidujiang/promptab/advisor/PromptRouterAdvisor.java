package io.github.iweidujiang.promptab.advisor;

import io.github.iweidujiang.promptab.domain.Variant;
import io.github.iweidujiang.promptab.evaluator.EvaluationContext;
import io.github.iweidujiang.promptab.evaluator.EvaluatorChain;
import io.github.iweidujiang.promptab.experiment.VariantRepository;
import io.github.iweidujiang.promptab.router.PromptRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;

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

    private static final Logger log = LoggerFactory.getLogger(PromptRouterAdvisor.class);
    private static final String CONTEXT_VARIANT_KEY = "promptab.variantKey";
    private static final String CONTEXT_SESSION_ID = "sessionId";

    private final PromptRouter promptRouter;
    private final VariantRepository variantRepository;
    private final String experimentKey;
    private final EvaluatorChain evaluatorChain;

    public PromptRouterAdvisor(PromptRouter promptRouter,
                               VariantRepository variantRepository,
                               String experimentKey) {
        this(promptRouter, variantRepository, experimentKey, null);
    }

    public PromptRouterAdvisor(PromptRouter promptRouter,
                               VariantRepository variantRepository,
                               String experimentKey,
                               EvaluatorChain evaluatorChain) {
        this.promptRouter = promptRouter;
        this.variantRepository = variantRepository;
        this.experimentKey = experimentKey;
        this.evaluatorChain = evaluatorChain;
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

        String variantKey = promptRouter.route(experimentKey, routerContext);
        log.info("路由决策：experimentKey={}, sessionId={}, variantKey={}",
                experimentKey, sessionId, variantKey);

        Variant variant = findVariant(variantKey);
        if (variant == null) {
            throw new IllegalStateException("找不到变体: " + variantKey);
        }

        // 用变体的 prompt 模板替换原始用户消息
        Prompt originalPrompt = request.prompt();
        Prompt modifiedPrompt = originalPrompt.augmentUserMessage(variant.getPromptTemplate());
        ChatClientRequest modifiedRequest = request.mutate().prompt(modifiedPrompt).build();

        ChatClientResponse response = chain.nextCall(modifiedRequest);

        long latencyMs = System.currentTimeMillis() - startTime;
        log.info("调用完成：experimentKey={}, variantKey={}, latencyMs={}",
                experimentKey, variantKey, latencyMs);

        // 触发评估器链
        if (evaluatorChain != null) {
            EvaluationContext evalContext = buildEvaluationContext(
                    request, variant, variantKey, sessionId, latencyMs, response);
            try {
                evaluatorChain.evaluate(evalContext);
            } catch (Exception e) {
                log.warn("评估器链执行异常（不影响主链路）：{}", e.getMessage(), e);
            }
        }

        // 将 variantKey 写入响应上下文，便于下游指标采集
        return response.mutate().context(CONTEXT_VARIANT_KEY, variantKey).build();
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
