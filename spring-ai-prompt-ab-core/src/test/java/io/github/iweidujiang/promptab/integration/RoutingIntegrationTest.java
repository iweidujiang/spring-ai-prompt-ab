package io.github.iweidujiang.promptab.integration;

import io.github.iweidujiang.promptab.experiment.JdbcVariantRepository;
import io.github.iweidujiang.promptab.router.HashPromptRouter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 路由链路集成测试
 * <p>
 * 使用真实 MySQL 容器验证：
 * 1. 流量分配比例接近预期（50/50）
 * 2. 同一 sessionId 始终路由到同一变体（粘性）
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-10
 */
class RoutingIntegrationTest extends BaseIntegrationTest {

    private static final String EXPERIMENT_KEY = "routing-test-" + UUID.randomUUID().toString().substring(0, 8);

    private HashPromptRouter router;
    private Long experimentId;

    @BeforeEach
    void setUpTestData() {
        // 插入实验
        getJdbcTemplate().update(
                "INSERT INTO ab_experiment (experiment_key, description, status) VALUES (?, ?, ?)",
                EXPERIMENT_KEY, "路由集成测试", "ACTIVE");

        experimentId = getJdbcTemplate().queryForObject(
                "SELECT id FROM ab_experiment WHERE experiment_key = ?",
                Long.class, EXPERIMENT_KEY);

        // 插入两个变体，各 50% 流量
        getJdbcTemplate().update(
                "INSERT INTO ab_variant (experiment_id, variant_key, prompt_template, traffic_pct, is_active) VALUES (?, ?, ?, ?, ?)",
                experimentId, "variant-a", "你是助手A", 50, true);
        getJdbcTemplate().update(
                "INSERT INTO ab_variant (experiment_id, variant_key, prompt_template, traffic_pct, is_active) VALUES (?, ?, ?, ?, ?)",
                experimentId, "variant-b", "你是助手B", 50, true);

        router = new HashPromptRouter(new JdbcVariantRepository(getJdbcTemplate()));
    }

    @AfterEach
    void cleanUpTestData() {
        getJdbcTemplate().update("DELETE FROM ab_variant WHERE experiment_id = ?", experimentId);
        getJdbcTemplate().update("DELETE FROM ab_experiment WHERE id = ?", experimentId);
    }

    /**
     * 100 个不同 sessionId 路由后，流量分配应接近 50/50（允许 ±20% 偏差）
     */
    @Test
    void route_with100Sessions_distributesTrafficApproximately5050() {
        Map<String, Integer> distribution = new HashMap<>();

        for (int i = 0; i < 100; i++) {
            String sessionId = "session-" + i;
            Map<String, Object> context = Map.of("sessionId", sessionId);
            String variantKey = router.route(EXPERIMENT_KEY, context);
            distribution.merge(variantKey, 1, Integer::sum);
        }

        int countA = distribution.getOrDefault("variant-a", 0);
        int countB = distribution.getOrDefault("variant-b", 0);

        assertEquals(100, countA + countB, "所有请求都应被路由");
        assertTrue(countA >= 30 && countA <= 70,
                "variant-a 应接近 50%，实际: " + countA + "%");
        assertTrue(countB >= 30 && countB <= 70,
                "variant-b 应接近 50%，实际: " + countB + "%");
    }

    /**
     * 同一 sessionId 多次路由，结果应始终一致（粘性）
     */
    @Test
    void route_withSameSessionId_alwaysReturnsSameVariant() {
        Map<String, Object> context = Map.of("sessionId", "sticky-user-001");

        String firstResult = router.route(EXPERIMENT_KEY, context);
        assertNotNull(firstResult);

        for (int i = 0; i < 10; i++) {
            String result = router.route(EXPERIMENT_KEY, context);
            assertEquals(firstResult, result,
                    "同一 sessionId 第 " + (i + 2) + " 次路由应返回相同变体");
        }
    }
}
