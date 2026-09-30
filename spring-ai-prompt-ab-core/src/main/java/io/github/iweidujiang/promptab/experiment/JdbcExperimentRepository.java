package io.github.iweidujiang.promptab.experiment;

import io.github.iweidujiang.promptab.domain.Experiment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 基于 JdbcTemplate 的实验仓储实现
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
public class JdbcExperimentRepository implements ExperimentRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcExperimentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Experiment> findByKey(String experimentKey) {
        String sql = """
                SELECT id, experiment_key, description, status, created_at, updated_at
                FROM ab_experiment
                WHERE experiment_key = ?
                """;
        var results = jdbcTemplate.query(sql, new ExperimentRowMapper(), experimentKey);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    private static class ExperimentRowMapper implements RowMapper<Experiment> {
        @Override
        public Experiment mapRow(ResultSet rs, int rowNum) throws SQLException {
            Experiment experiment = new Experiment();
            experiment.setId(rs.getLong("id"));
            experiment.setExperimentKey(rs.getString("experiment_key"));
            experiment.setDescription(rs.getString("description"));
            experiment.setStatus(rs.getString("status"));
            experiment.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
            experiment.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
            return experiment;
        }
    }
}
