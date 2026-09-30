-- 指标事件表
CREATE TABLE ab_metric_event (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_key  VARCHAR(128)  NOT NULL,
    variant_key     VARCHAR(128)  NOT NULL,
    session_id      VARCHAR(256),
    score           DOUBLE,
    evaluator_name  VARCHAR(128),
    latency_ms      BIGINT,
    input_tokens    INT,
    output_tokens   INT,
    timestamp       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_experiment_variant_ts (experiment_key, variant_key, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
