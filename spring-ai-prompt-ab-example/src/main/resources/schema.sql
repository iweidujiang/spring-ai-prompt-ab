-- 实验表
CREATE TABLE IF NOT EXISTS ab_experiment (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_key  VARCHAR(128)  NOT NULL,
    description     VARCHAR(512),
    status          VARCHAR(32)   NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_experiment_key UNIQUE (experiment_key)
);

-- 变体表
CREATE TABLE IF NOT EXISTS ab_variant (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id    BIGINT        NOT NULL,
    variant_key      VARCHAR(128)  NOT NULL,
    prompt_template  CLOB          NOT NULL,
    traffic_pct      INT           NOT NULL DEFAULT 0,
    is_active        BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_experiment_variant UNIQUE (experiment_id, variant_key),
    CONSTRAINT fk_variant_experiment FOREIGN KEY (experiment_id) REFERENCES ab_experiment(id)
);

-- 指标事件表
CREATE TABLE IF NOT EXISTS ab_metric_event (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_key  VARCHAR(128) NOT NULL,
    variant_key     VARCHAR(128) NOT NULL,
    session_id      VARCHAR(256),
    score           DOUBLE,
    evaluator_name  VARCHAR(128),
    latency_ms      BIGINT,
    input_tokens    INT,
    output_tokens   INT,
    timestamp       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_experiment_variant_ts
    ON ab_metric_event (experiment_key, variant_key, timestamp);
