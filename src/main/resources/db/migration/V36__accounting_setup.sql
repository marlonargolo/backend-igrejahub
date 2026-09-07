-- V36: Setup completo de contabilidade
-- 1. Role CONTADOR (se não existir)
INSERT INTO roles (name, description, is_system, organization_id)
SELECT 'CONTADOR', 'Contador com acesso total à contabilidade', true, o.id
FROM organizations o
WHERE NOT EXISTS (
  SELECT 1 FROM roles r WHERE r.name = 'CONTADOR'
)
LIMIT 1;

-- 2. Permissões ACCOUNTING_ADMIN para CONTADOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CONTADOR'
AND p.name IN (
  'MEMBER_VIEW',
  'FINANCE_VIEW','FINANCE_CREATE','FINANCE_UPDATE','FINANCE_CONFIRM','FINANCE_CANCEL',
  'FINANCE_EXPORT','FINANCE_REPORT',
  'ACCOUNTING_VIEW','ACCOUNTING_CREATE','ACCOUNTING_UPDATE','ACCOUNTING_EXPORT',
  'ACCOUNTING_REPORT','ACCOUNTING_ADMIN',
  'ASSET_VIEW'
)
ON CONFLICT DO NOTHING;

-- 3. Garantir ACCOUNTING_VIEW para ADMIN (caso não tenha)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('ADMIN', 'PASTOR_PRINCIPAL')
AND p.name = 'ACCOUNTING_VIEW'
ON CONFLICT DO NOTHING;

-- 4. Tabela de observações do contador
CREATE TABLE IF NOT EXISTS accounting_observations (
  id              BIGSERIAL PRIMARY KEY,
  organization_id BIGINT    NOT NULL,
  transaction_id  BIGINT    NOT NULL REFERENCES financial_transactions(id) ON DELETE CASCADE,
  observation     TEXT,
  created_by      BIGINT,
  created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by      BIGINT,
  updated_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_acc_obs_transaction  ON accounting_observations(transaction_id);
CREATE INDEX IF NOT EXISTS idx_acc_obs_organization ON accounting_observations(organization_id);

-- 5. Colunas de controle de conciliação nas transações
ALTER TABLE financial_transactions
  ADD COLUMN IF NOT EXISTS reconciled     BOOLEAN   DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS reconciled_at  TIMESTAMP,
  ADD COLUMN IF NOT EXISTS reconciled_by  BIGINT,
  ADD COLUMN IF NOT EXISTS accounting_obs TEXT;    -- snapshot rápido sem JOIN