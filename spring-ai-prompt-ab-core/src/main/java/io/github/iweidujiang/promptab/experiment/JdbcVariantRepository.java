package io.github.iweidujiang.promptab.experiment;

import io.github.iweidujiang.promptab.domain.Variant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

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

    @Override
    public List<Variant> findByExperimentKey(String experimentKey) {
        String sql = """
                SELECT v.id, v.experiment_id, v.variant_key, v.prompt_template, v.traffic_pct, v.is_active
                FROM ab_variant v
                JOIN ab_experiment e ON v.experiment_id = e.id
                WHERE e.experiment_key = ?
                ORDER BY v.id
                """;
        return jdbcTemplate.query(sql, new VariantRowMapper(), experimentKey);
    }

    @Override
    public Optional<Variant> findById(Long id) {
        String sql = """
                SELECT id, experiment_id, variant_key, prompt_template, traffic_pct, is_active
                FROM ab_variant
                WHERE id = ?
                """;
        var results = jdbcTemplate.query(sql, new VariantRowMapper(), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Variant save(Variant variant) {
        String sql = """
                INSERT INTO ab_variant (experiment_id, variant_key, prompt_template, traffic_pct, is_active)
                VALUES (?, ?, ?, ?, ?)
                """;
        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, variant.getExperimentId());
            ps.setString(2, variant.getVariantKey());
            ps.setString(3, variant.getPromptTemplate());
            ps.setInt(4, variant.getTrafficPct());
            ps.setBoolean(5, variant.getIsActive());
            return ps;
        }, keyHolder);
        variant.setId(keyHolder.getKey().longValue());
        return variant;
    }

    @Override
    public boolean updateTrafficPct(Long id, int trafficPct) {
        String sql = "UPDATE ab_variant SET traffic_pct = ? WHERE id = ?";
        return jdbcTemplate.update(sql, trafficPct, id) > 0;
    }

    @Override
    public boolean updateIsActive(Long id, boolean isActive) {
        String sql = "UPDATE ab_variant SET is_active = ? WHERE id = ?";
        return jdbcTemplate.update(sql, isActive, id) > 0;
    }

    @Override
    public boolean existsByExperimentIdAndVariantKey(Long experimentId, String variantKey) {
        String sql = "SELECT COUNT(*) FROM ab_variant WHERE experiment_id = ? AND variant_key = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, experimentId, variantKey);
        return count != null && count > 0;
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
