-- Tabela de planos de assinatura
-- Controla quotas por Igreja: max_users, max_congregations, max_members

CREATE TABLE IF NOT EXISTS plans (
    id                BIGSERIAL PRIMARY KEY,
    organization_id   BIGINT NOT NULL DEFAULT 1,
    name              VARCHAR(100) NOT NULL UNIQUE,
    description       VARCHAR(255),
    price             NUMERIC(10,2) NOT NULL DEFAULT 0,
    max_users         INT NOT NULL DEFAULT 5,
    max_congregations INT NOT NULL DEFAULT 1,
    max_members       INT NOT NULL DEFAULT 100,
    features          TEXT,
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    is_system         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by        BIGINT,
    updated_by        BIGINT,
    version           BIGINT DEFAULT 0,
    deleted           BOOLEAN DEFAULT FALSE,
    deleted_at        TIMESTAMP,
    deleted_by        BIGINT
);

-- Planos padrão do sistema
INSERT INTO plans (organization_id, name, description, price, max_users, max_congregations, max_members, features, is_system)
VALUES
  (1, 'BASICO',       'Plano Básico',         79.00,   3,   1,   100,  '["Membros","Relatórios básicos"]',                            TRUE),
  (1, 'PROFISSIONAL', 'Plano Profissional',   149.00,  10,  5,   500,  '["Membros","Tesouraria","Patrimônio","Relatórios avançados"]', TRUE),
  (1, 'PREMIUM',      'Plano Premium Multi',  299.00, 999, 999, 9999,  '["Membros","Tesouraria","Patrimônio","Contabilidade","Multi-congregações"]', TRUE)
ON CONFLICT (name) DO NOTHING;

-- Adicionar plan_id na tabela churches
ALTER TABLE churches ADD COLUMN IF NOT EXISTS plan_id BIGINT REFERENCES plans(id);

-- Vincular igrejas existentes ao plano PREMIUM por padrão
UPDATE churches SET plan_id = (SELECT id FROM plans WHERE name = 'PREMIUM') WHERE plan_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_plans_active  ON plans(is_active) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_churches_plan ON churches(plan_id);