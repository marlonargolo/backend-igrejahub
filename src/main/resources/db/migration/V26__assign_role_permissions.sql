-- ADMIN: acesso total exceto ROOT_ACCESS
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
AND p.name != 'ROOT_ACCESS'
ON CONFLICT DO NOTHING;

-- PASTOR_PRINCIPAL: ver tudo, gerenciar membros e relatórios
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'PASTOR_PRINCIPAL'
AND p.name IN (
  'MEMBER_VIEW','MEMBER_CREATE','MEMBER_UPDATE',
  'FINANCE_VIEW','FINANCE_CONFIRM',
  'ACCOUNTING_VIEW','ACCOUNTING_REPORT',
  'ASSET_VIEW',
  'REPORT_VIEW','REPORT_CREATE','REPORT_DOWNLOAD',
  'USER_VIEW',
  'SETTINGS_VIEW'
)
ON CONFLICT DO NOTHING;

-- PASTOR_CONGREGACAO: igual ao pastor principal mas sem contabilidade avançada
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'PASTOR_CONGREGACAO'
AND p.name IN (
  'MEMBER_VIEW','MEMBER_CREATE','MEMBER_UPDATE',
  'FINANCE_VIEW',
  'ASSET_VIEW',
  'REPORT_VIEW','REPORT_DOWNLOAD',
  'SETTINGS_VIEW'
)
ON CONFLICT DO NOTHING;

-- TESOUREIRO: foco em finanças e contabilidade
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'TESOUREIRO'
AND p.name IN (
  'MEMBER_VIEW',
  'FINANCE_VIEW','FINANCE_CREATE','FINANCE_UPDATE','FINANCE_CONFIRM','FINANCE_CANCEL','FINANCE_EXPORT',
  'ACCOUNTING_VIEW','ACCOUNTING_CREATE','ACCOUNTING_UPDATE','ACCOUNTING_EXPORT','ACCOUNTING_REPORT',
  'ASSET_VIEW',
  'REPORT_VIEW','REPORT_DOWNLOAD',
  'SETTINGS_VIEW'
)
ON CONFLICT DO NOTHING;

-- SECRETARIO: foco em membros
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SECRETARIO'
AND p.name IN (
  'MEMBER_VIEW','MEMBER_CREATE','MEMBER_UPDATE','MEMBER_DELETE',
  'REPORT_VIEW','REPORT_CREATE','REPORT_DOWNLOAD',
  'SETTINGS_VIEW'
)
ON CONFLICT DO NOTHING;

-- USUARIO: somente visualização
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'USUARIO'
AND p.name IN (
  'MEMBER_VIEW',
  'FINANCE_VIEW',
  'ASSET_VIEW',
  'REPORT_VIEW',
  'SETTINGS_VIEW'
)
ON CONFLICT DO NOTHING;

-- Adicionar campo church_id na tabela users para vincular à igreja
ALTER TABLE users ADD COLUMN IF NOT EXISTS church_id BIGINT;
