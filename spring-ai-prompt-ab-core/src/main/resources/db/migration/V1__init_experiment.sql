-- 实验表
CREATE TABLE ab_experiment (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_key  VARCHAR(128)  NOT NULL,
    description     VARCHAR(512),
    status          VARCHAR(32)   NOT NULL DEFAULT 'DRAFT',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_experiment_key (experiment_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 变体表
CREATE TABLE ab_variant (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id    BIGINT        NOT NULL,
    variant_key      VARCHAR(128)  NOT NULL,
    prompt_template  TEXT          NOT NULL,
    traffic_pct      INT           NOT NULL DEFAULT 0,
    is_active        TINYINT(1)    NOT NULL DEFAULT 0,
    UNIQUE KEY uk_experiment_variant (experiment_id, variant_key),
    CONSTRAINT fk_variant_experiment FOREIGN KEY (experiment_id) REFERENCES ab_experiment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
