package io.github.iweidujiang.promptab.router;

import io.github.iweidujiang.promptab.domain.Variant;
import io.github.iweidujiang.promptab.experiment.VariantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 基于 sessionId 哈希的粘性路由器
 * <p>
 * 将 sessionId 哈希后对 100 取模，按变体的 trafficPct 区间分配。
 * 同一 sessionId 始终路由到同一变体，保证会话粘性。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
public class HashPromptRouter implements PromptRouter {

    private static final Logger log = LoggerFactory.getLogger(HashPromptRouter.class);
    private static final String SESSION_ID_KEY = "sessionId";

    private final VariantRepository variantRepository;

    public HashPromptRouter(VariantRepository variantRepository) {
        this.variantRepository = variantRepository;
    }

    @Override
    public String route(String experimentKey, Map<String, Object> context) {
        String sessionId = getSessionId(context);
        List<Variant> variants = variantRepository.findActiveByExperimentKey(experimentKey);

        if (variants.isEmpty()) {
            log.warn("实验 {} 没有激活的变体", experimentKey);
            throw new IllegalStateException("实验 " + experimentKey + " 没有激活的变体");
        }

        int hash = Math.abs(sessionId.hashCode() % 100);
        int cumulative = 0;

        for (Variant variant : variants) {
            cumulative += variant.getTrafficPct();
            if (hash < cumulative) {
                log.debug("路由决策：experimentKey={}, sessionId={}, hash={}, variantKey={}",
                        experimentKey, sessionId, hash, variant.getVariantKey());
                return variant.getVariantKey();
            }
        }

        // 兜底：流量比例之和不足 100 时，返回最后一个变体
        Variant lastVariant = variants.get(variants.size() - 1);
        log.debug("路由决策（兜底）：experimentKey={}, sessionId={}, variantKey={}",
                experimentKey, sessionId, lastVariant.getVariantKey());
        return lastVariant.getVariantKey();
    }

    private String getSessionId(Map<String, Object> context) {
        Object sessionId = context.get(SESSION_ID_KEY);
        if (sessionId == null) {
            throw new IllegalArgumentException("路由上下文缺少 sessionId");
        }
        return sessionId.toString();
    }
}
