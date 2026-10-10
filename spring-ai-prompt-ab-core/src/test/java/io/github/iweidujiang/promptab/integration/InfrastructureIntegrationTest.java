package io.github.iweidujiang.promptab.integration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 Testcontainers 基础设施：MySQL 容器启动 + Flyway 迁移
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-10
 */
class InfrastructureIntegrationTest extends BaseIntegrationTest {

    /**
     * MySQL 容器启动且 Flyway 迁移成功后，三张核心表应存在
     */
    @Test
    void flywayMigration_createsAllTables() {
        List<Map<String, Object>> tables = getJdbcTemplate().queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE()");

        List<String> tableNames = tables.stream()
                .map(row -> (String) row.get("TABLE_NAME"))
                .sorted()
                .toList();

        assertTrue(tableNames.contains("ab_experiment"), "ab_experiment 表应存在");
        assertTrue(tableNames.contains("ab_variant"), "ab_variant 表应存在");
        assertTrue(tableNames.contains("ab_metric_event"), "ab_metric_event 表应存在");
    }

    /**
     * 容器 JDBC 连接可用
     */
    @Test
    void databaseConnection_isAvailable() {
        Integer result = getJdbcTemplate().queryForObject("SELECT 1", Integer.class);
        assertEquals(1, result);
    }
}
