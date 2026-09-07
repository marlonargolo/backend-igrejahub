-- V33: Tabela de configurações da secretaria por Igreja
CREATE TABLE IF NOT EXISTS church_configurations (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT       NOT NULL,
    church_id       BIGINT       NOT NULL,
    config_key      VARCHAR(50)  NOT NULL,
    config_value    TEXT         NOT NULL,
    is_system       BOOLEAN      DEFAULT FALSE,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    version         BIGINT       DEFAULT 0,
    deleted         BOOLEAN      DEFAULT FALSE,
    CONSTRAINT uk_church_config_key UNIQUE(organization_id, church_id, config_key)
);

CREATE INDEX IF NOT EXISTS idx_church_configurations_org_church
    ON church_configurations(organization_id, church_id);
CREATE INDEX IF NOT EXISTS idx_church_configurations_org_church_key
    ON church_configurations(organization_id, church_id, config_key);
