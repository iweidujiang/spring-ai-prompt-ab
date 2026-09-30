package io.github.iweidujiang.promptab.experiment;

import io.github.iweidujiang.promptab.domain.Experiment;
import io.github.iweidujiang.promptab.domain.Variant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository 层集成测试（H2 内存库）
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
class JdbcRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcExperimentRepository experimentRepository;
    private JdbcVariantRepository variantRepository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .build();
        jdbcTemplate = new JdbcTemplate(dataSource);

        // 初始化表结构
        jdbcTemplate.execute("""
                CREATE TABLE ab_experiment (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    experiment_key VARCHAR(128) NOT NULL,
                    description VARCHAR(512),
                    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE (experiment_key)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE ab_variant (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    experiment_id BIGINT NOT NULL,
                    variant_key VARCHAR(128) NOT NULL,
                    prompt_template TEXT NOT NULL,
                    traffic_pct INT NOT NULL DEFAULT 0,
                    is_active BOOLEAN NOT NULL DEFAULT FALSE,
                    UNIQUE (experiment_id, variant_key)
                )
                """);

        experimentRepository = new JdbcExperimentRepository(jdbcTemplate);
        variantRepository = new JdbcVariantRepository(jdbcTemplate);
    }

    /**
     * 根据 key 查询存在的实验
     */
    @Test
    void experimentRepository_findByKey_returnsExperiment() {
        jdbcTemplate.update(
                "INSERT INTO ab_experiment (experiment_key, description, status) VALUES (?, ?, ?)",
                "test-exp", "测试实验", "ACTIVE");

        Optional<Experiment> result = experimentRepository.findByKey("test-exp");

        assertTrue(result.isPresent());
        assertEquals("test-exp", result.get().getExperimentKey());
        assertEquals("测试实验", result.get().getDescription());
        assertEquals("ACTIVE", result.get().getStatus());
    }

    /**
     * 根据 key 查询不存在的实验返回空
     */
    @Test
    void experimentRepository_findByKey_returnsEmptyWhenNotFound() {
        Optional<Experiment> result = experimentRepository.findByKey("non-existent");
        assertTrue(result.isEmpty());
    }

    /**
     * 查询实验下所有激活的变体
     */
    @Test
    void variantRepository_findActiveByExperimentKey_returnsOnlyActiveVariants() {
        // 准备实验
        jdbcTemplate.update(
                "INSERT INTO ab_experiment (experiment_key, status) VALUES (?, ?)",
                "test-exp", "ACTIVE");
        Long experimentId = jdbcTemplate.queryForObject(
                "SELECT id FROM ab_experiment WHERE experiment_key = ?", Long.class, "test-exp");

        // 准备变体：2 个激活，1 个未激活
        jdbcTemplate.update(
                "INSERT INTO ab_variant (experiment_id, variant_key, prompt_template, traffic_pct, is_active) VALUES (?, ?, ?, ?, ?)",
                experimentId, "variant-a", "模板A", 50, true);
        jdbcTemplate.update(
                "INSERT INTO ab_variant (experiment_id, variant_key, prompt_template, traffic_pct, is_active) VALUES (?, ?, ?, ?, ?)",
                experimentId, "variant-b", "模板B", 30, true);
        jdbcTemplate.update(
                "INSERT INTO ab_variant (experiment_id, variant_key, prompt_template, traffic_pct, is_active) VALUES (?, ?, ?, ?, ?)",
                experimentId, "variant-c", "模板C", 20, false);

        List<Variant> result = variantRepository.findActiveByExperimentKey("test-exp");

        assertEquals(2, result.size());
        assertEquals("variant-a", result.get(0).getVariantKey());
        assertEquals("variant-b", result.get(1).getVariantKey());
    }

    /**
     * 实验无激活变体时返回空列表
     */
    @Test
    void variantRepository_findActiveByExperimentKey_returnsEmptyWhenNoActiveVariants() {
        jdbcTemplate.update(
                "INSERT INTO ab_experiment (experiment_key, status) VALUES (?, ?)",
                "empty-exp", "ACTIVE");

        List<Variant> result = variantRepository.findActiveByExperimentKey("empty-exp");

        assertTrue(result.isEmpty());
    }
}
