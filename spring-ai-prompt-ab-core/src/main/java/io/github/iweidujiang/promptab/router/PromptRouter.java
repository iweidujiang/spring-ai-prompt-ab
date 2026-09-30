package io.github.iweidujiang.promptab.router;

import java.util.Map;

/**
 * Prompt 变体路由器
 * <p>
 * 根据实验标识和上下文信息，决定本次请求使用哪个 Prompt 变体。
 * 默认实现基于 sessionId 哈希做粘性分配。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
public interface PromptRouter {

    /**
     * 路由到具体的变体
     *
     * @param experimentKey 实验标识
     * @param context       路由上下文（至少包含 sessionId）
     * @return 变体标识 variantKey
     */
    String route(String experimentKey, Map<String, Object> context);
}
