-- V42: Configurações globais da organização, gerenciadas pela administração
-- externa (ROOT). Modelo chave/valor simples (parâmetros de segurança,
-- política de senha, configuração de email/integrações etc.).

CREATE TABLE IF NOT EXISTS organization_settings (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    setting_key VARCHAR(100) NOT NULL,
    setting_value JSONB,
    updated_at TIMESTAMP,
    updated_by BIGINT,
    UNIQUE (organization_id, setting_key)
);

CREATE INDEX IF NOT EXISTS idx_organization_settings_org ON organization_settings(organization_id);
