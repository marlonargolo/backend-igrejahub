-- V36: Setup completo de contabilidade (CORRIGIDO)

-- 1. Verificar/Adicionar role CONTADOR (sem organization_id)
INSERT INTO roles (name, description, is_system)
SELECT 'CONTADOR', 'Contador com acesso total à contabilidade', true
WHERE NOT EXISTS (
  SELECT 1 FROM roles WHERE name = 'CONTADOR'
);

-- 2. Verificar/Adicionar permissões de contabilidade
-- ACCOUNTING_VIEW
INSERT INTO permissions (name, description, category, is_active, is_system)
SELECT 'ACCOUNTING_VIEW', 'Visualizar módulo de contabilidade', 'ACCOUNTING', true, true
WHERE NOT EXISTS (
  SELECT 1 FROM permissions WHERE name = 'ACCOUNTING_VIEW'
);

-- ACCOUNTING_CREATE
INSERT INTO permissions (name, description, category, is_active, is_system)
SELECT 'ACCOUNTING_CREATE', 'Criar lançamentos contábeis', 'ACCOUNTING', true, true
WHERE NOT EXISTS (
  SELECT 1 FROM permissions WHERE name = 'ACCOUNTING_CREATE'
);

-- ACCOUNTING_UPDATE
INSERT INTO permissions (name, description, category, is_active, is_system)
SELECT 'ACCOUNTING_UPDATE', 'Atualizar lançamentos contábeis', 'ACCOUNTING', true, true
WHERE NOT EXISTS (
  SELECT 1 FROM permissions WHERE name = 'ACCOUNTING_UPDATE'
);

-- ACCOUNTING_EXPORT
INSERT INTO permissions (name, description, category, is_active, is_system)
SELECT 'ACCOUNTING_EXPORT', 'Exportar dados contábeis', 'ACCOUNTING', true, true
WHERE NOT EXISTS (
  SELECT 1 FROM permissions WHERE name = 'ACCOUNTING_EXPORT'
);

-- ACCOUNTING_REPORT
INSERT INTO permissions (name, description, category, is_active, is_system)
SELECT 'ACCOUNTING_REPORT', 'Gerar relatórios contábeis', 'ACCOUNTING', true, true
WHERE NOT EXISTS (
  SELECT 1 FROM permissions WHERE name = 'ACCOUNTING_REPORT'
);

-- ACCOUNTING_ADMIN
INSERT INTO permissions (name, description, category, is_active, is_system)
SELECT 'ACCOUNTING_ADMIN', 'Administrar contabilidade', 'ACCOUNTING', true, true
WHERE NOT EXISTS (
  SELECT 1 FROM permissions WHERE name = 'ACCOUNTING_ADMIN'
);

-- 3. Atribuir permissões ACCOUNTING_VIEW para ADMIN e PASTOR_PRINCIPAL
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p
WHERE r.name IN ('ADMIN', 'PASTOR_PRINCIPAL')
AND p.name = 'ACCOUNTING_VIEW'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 4. Atribuir TODAS as permissões de contabilidade para CONTADOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p
WHERE r.name = 'CONTADOR'
AND p.name IN (
  'ACCOUNTING_VIEW',
  'ACCOUNTING_CREATE',
  'ACCOUNTING_UPDATE',
  'ACCOUNTING_EXPORT',
  'ACCOUNTING_REPORT',
  'ACCOUNTING_ADMIN',
  'MEMBER_VIEW',
  'FINANCE_VIEW',
  'FINANCE_CREATE',
  'FINANCE_UPDATE',
  'FINANCE_CONFIRM',
  'FINANCE_CANCEL',
  'FINANCE_EXPORT',
  'FINANCE_REPORT'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 5. Tabela de observações do contador
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

-- 6. Colunas de controle de conciliação nas transações
ALTER TABLE financial_transactions
  ADD COLUMN IF NOT EXISTS reconciled     BOOLEAN   DEFAULT FALSE;

ALTER TABLE financial_transactions
  ADD COLUMN IF NOT EXISTS reconciled_at  TIMESTAMP;

ALTER TABLE financial_transactions
  ADD COLUMN IF NOT EXISTS reconciled_by  BIGINT;

ALTER TABLE financial_transactions
  ADD COLUMN IF NOT EXISTS accounting_obs TEXT;

-- 7. Verificar se a tabela role_permissions existe e tem a constraint correta
-- Se a tabela não tiver a constraint, adicionar
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint 
    WHERE conname = 'role_permissions_role_id_permission_id_key'
    AND conrelid = 'role_permissions'::regclass
  ) THEN
    ALTER TABLE role_permissions 
    ADD CONSTRAINT role_permissions_role_id_permission_id_key 
    UNIQUE (role_id, permission_id);
  END IF;
END $$;