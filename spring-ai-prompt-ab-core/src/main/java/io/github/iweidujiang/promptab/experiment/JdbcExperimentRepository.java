package io.github.iweidujiang.promptab.experiment;

import io.github.iweidujiang.promptab.domain.Experiment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    public List<Experiment> findByStatus(String status) {
        String sql = """
                SELECT id, experiment_key, description, status, created_at, updated_at
                FROM ab_experiment
                WHERE status = ?
                ORDER BY created_at DESC
                """;
        return jdbcTemplate.query(sql, new ExperimentRowMapper(), status);
    }

    @Override
    public Experiment save(Experiment experiment) {
        String sql = """
                INSERT INTO ab_experiment (experiment_key, description, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        LocalDateTime now = LocalDateTime.now();
        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, experiment.getExperimentKey());
            ps.setString(2, experiment.getDescription());
            ps.setString(3, experiment.getStatus() != null ? experiment.getStatus() : "DRAFT");
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.setTimestamp(5, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);
        experiment.setId(keyHolder.getKey().longValue());
        experiment.setCreatedAt(now);
        experiment.setUpdatedAt(now);
        return experiment;
    }

    @Override
    public boolean updateStatus(String experimentKey, String newStatus) {
        String sql = """
                UPDATE ab_experiment SET status = ?, updated_at = ? WHERE experiment_key = ?
                """;
        int rows = jdbcTemplate.update(sql, newStatus, Timestamp.valueOf(LocalDateTime.now()), experimentKey);
        return rows > 0;
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
