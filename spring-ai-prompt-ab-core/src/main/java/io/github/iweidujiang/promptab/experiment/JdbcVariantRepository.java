package io.github.iweidujiang.promptab.experiment;

import io.github.iweidujiang.promptab.domain.Variant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 基于 JdbcTemplate 的变体仓储实现
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
public class JdbcVariantRepository implements VariantRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcVariantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Variant> findActiveByExperimentKey(String experimentKey) {
        String sql = """
                SELECT v.id, v.experiment_id, v.variant_key, v.prompt_template, v.traffic_pct, v.is_active
                FROM ab_variant v
                JOIN ab_experiment e ON v.experiment_id = e.id
                WHERE e.experiment_key = ? AND v.is_active = TRUE
                ORDER BY v.id
                """;
        return jdbcTemplate.query(sql, new VariantRowMapper(), experimentKey);
    }

    private static class VariantRowMapper implements RowMapper<Variant> {
        @Override
        public Variant mapRow(ResultSet rs, int rowNum) throws SQLException {
            Variant variant = new Variant();
            variant.setId(rs.getLong("id"));
            variant.setExperimentId(rs.getLong("experiment_id"));
            variant.setVariantKey(rs.getString("variant_key"));
            variant.setPromptTemplate(rs.getString("prompt_template"));
            variant.setTrafficPct(rs.getInt("traffic_pct"));
            variant.setIsActive(rs.getBoolean("is_active"));
            return variant;
        }
    }
}
