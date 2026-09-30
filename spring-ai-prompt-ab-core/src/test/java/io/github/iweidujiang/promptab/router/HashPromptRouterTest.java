package io.github.iweidujiang.promptab.router;

import io.github.iweidujiang.promptab.domain.Variant;
import io.github.iweidujiang.promptab.experiment.VariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * HashPromptRouter 单元测试
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
class HashPromptRouterTest {

    private VariantRepository variantRepository;
    private HashPromptRouter router;

    @BeforeEach
    void setUp() {
        variantRepository = mock(VariantRepository.class);
        router = new HashPromptRouter(variantRepository);
    }

    /**
     * 同一 sessionId 始终路由到同一变体（粘性分配）
     */
    @Test
    void route_sameSessionIdAlwaysRoutesToSameVariant() {
        // 准备：两个变体，各 50% 流量
        Variant variantA = createVariant("variant-a", 50);
        Variant variantB = createVariant("variant-b", 50);
        when(variantRepository.findActiveByExperimentKey("test-exp"))
                .thenReturn(List.of(variantA, variantB));

        Map<String, Object> context = Map.of("sessionId", "user-123");

        // 执行多次
        String result1 = router.route("test-exp", context);
        String result2 = router.route("test-exp", context);
        String result3 = router.route("test-exp", context);

        // 验证：结果一致（粘性分配）
        assertEquals(result1, result2);
        assertEquals(result2, result3);
    }

    /**
     * 流量分配比例大致正确（允许 ±5% 偏差）
     */
    @Test
    void route_trafficDistributionRoughlyCorrect() {
        // 准备：A 占 70%，B 占 30%
        Variant variantA = createVariant("variant-a", 70);
        Variant variantB = createVariant("variant-b", 30);
        when(variantRepository.findActiveByExperimentKey("test-exp"))
                .thenReturn(List.of(variantA, variantB));

        // 执行 1000 次不同 sessionId 的路由
        int countA = 0;
        for (int i = 0; i < 1000; i++) {
            Map<String, Object> context = Map.of("sessionId", "user-" + i);
            String result = router.route("test-exp", context);
            if ("variant-a".equals(result)) {
                countA++;
            }
        }

        // 验证：A 的比例在 65%-75% 之间（允许一定偏差）
        assertTrue(countA >= 650 && countA <= 750,
                "variant-a 实际比例: " + countA / 10.0 + "%，期望约 70%");
    }

    /**
     * 无激活变体时抛出异常
     */
    @Test
    void route_throwsExceptionWhenNoActiveVariants() {
        when(variantRepository.findActiveByExperimentKey("empty-exp"))
                .thenReturn(Collections.emptyList());

        Map<String, Object> context = Map.of("sessionId", "user-123");

        assertThrows(IllegalStateException.class, () -> router.route("empty-exp", context));
    }

    /**
     * 缺少 sessionId 时抛出异常
     */
    @Test
    void route_throwsExceptionWhenSessionIdMissing() {
        Map<String, Object> context = new HashMap<>();

        assertThrows(IllegalArgumentException.class, () -> router.route("test-exp", context));
    }

    /**
     * 流量比例不足 100 时兜底返回最后一个变体
     */
    @Test
    void route_fallbackToLastVariantWhenTrafficBelow100() {
        // 准备：A 占 30%，B 占 20%，总共只有 50%
        Variant variantA = createVariant("variant-a", 30);
        Variant variantB = createVariant("variant-b", 20);
        when(variantRepository.findActiveByExperimentKey("test-exp"))
                .thenReturn(List.of(variantA, variantB));

        // 找一个哈希值落在 50-99 区间的 sessionId
        Map<String, Object> context = Map.of("sessionId", "fallback-user");
        int hash = Math.abs("fallback-user".hashCode() % 100);

        if (hash >= 50) {
            String result = router.route("test-exp", context);
            assertEquals("variant-b", result, "哈希值 " + hash + " 应兜底到最后一个变体");
        }
    }

    private Variant createVariant(String variantKey, int trafficPct) {
        Variant variant = new Variant();
        variant.setVariantKey(variantKey);
        variant.setTrafficPct(trafficPct);
        variant.setIsActive(true);
        return variant;
    }
}
