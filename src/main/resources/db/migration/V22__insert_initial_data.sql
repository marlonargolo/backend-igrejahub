-- Insert roles
INSERT INTO roles (name, description, is_active, is_system) VALUES
('ROOT', 'Acesso total ao sistema', true, true),
('ADMIN', 'Administrador da organização', true, true),
('PASTOR_PRINCIPAL', 'Pastor principal', true, true),
('PASTOR_CONGREGACAO', 'Pastor de congregação', true, true),
('TESOUREIRO', 'Tesoureiro', true, true),
('SECRETARIO', 'Secretário', true, true),
('USUARIO', 'Usuário comum', true, true)
ON CONFLICT (name) DO NOTHING;

-- Insert permissions
INSERT INTO permissions (name, description, category, is_active, is_system) VALUES
('MEMBER_VIEW', 'Visualizar membros', 'MEMBERS', true, true),
('MEMBER_CREATE', 'Criar membros', 'MEMBERS', true, true),
('MEMBER_UPDATE', 'Atualizar membros', 'MEMBERS', true, true),
('MEMBER_DELETE', 'Excluir membros', 'MEMBERS', true, true),
('FINANCE_VIEW', 'Visualizar finanças', 'FINANCE', true, true),
('FINANCE_CREATE', 'Criar transações', 'FINANCE', true, true),
('FINANCE_UPDATE', 'Atualizar transações', 'FINANCE', true, true),
('FINANCE_CONFIRM', 'Confirmar transações', 'FINANCE', true, true),
('FINANCE_CANCEL', 'Cancelar transações', 'FINANCE', true, true),
('FINANCE_EXPORT', 'Exportar finanças', 'FINANCE', true, true),
('ACCOUNTING_VIEW', 'Visualizar contabilidade', 'ACCOUNTING', true, true),
('ACCOUNTING_CREATE', 'Criar lançamentos contábeis', 'ACCOUNTING', true, true),
('ACCOUNTING_UPDATE', 'Atualizar lançamentos contábeis', 'ACCOUNTING', true, true),
('ACCOUNTING_EXPORT', 'Exportar contabilidade', 'ACCOUNTING', true, true),
('ACCOUNTING_REPORT', 'Gerar relatórios contábeis', 'ACCOUNTING', true, true),
('ACCOUNTING_ADMIN', 'Administrar contabilidade', 'ACCOUNTING', true, true),
('ASSET_VIEW', 'Visualizar patrimônio', 'ASSETS', true, true),
('ASSET_CREATE', 'Criar patrimônio', 'ASSETS', true, true),
('ASSET_UPDATE', 'Atualizar patrimônio', 'ASSETS', true, true),
('ASSET_DELETE', 'Excluir patrimônio', 'ASSETS', true, true),
('REPORT_VIEW', 'Visualizar relatórios', 'REPORTS', true, true),
('REPORT_CREATE', 'Criar relatórios', 'REPORTS', true, true),
('REPORT_DOWNLOAD', 'Baixar relatórios', 'REPORTS', true, true),
('USER_VIEW', 'Visualizar usuários', 'USERS', true, true),
('USER_CREATE', 'Criar usuários', 'USERS', true, true),
('USER_UPDATE', 'Atualizar usuários', 'USERS', true, true),
('USER_DISABLE', 'Desabilitar usuários', 'USERS', true, true),
('USER_ROLE_MANAGE', 'Gerenciar roles de usuários', 'USERS', true, true),
('AUDIT_VIEW', 'Visualizar auditoria', 'AUDIT', true, true),
('SETTINGS_VIEW', 'Visualizar configurações', 'SETTINGS', true, true),
('SETTINGS_UPDATE', 'Atualizar configurações', 'SETTINGS', true, true),
('BILLING_VIEW', 'Visualizar faturamento', 'BILLING', true, true),
('BILLING_MANAGE', 'Gerenciar faturamento', 'BILLING', true, true),
('ROOT_ACCESS', 'Acesso root ao sistema', 'SYSTEM', true, true)
ON CONFLICT (name) DO NOTHING;

-- Assign all permissions to ROOT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'ROOT'
ON CONFLICT DO NOTHING;
