-- V38: Permissão de administração externa (apenas ROOT)

INSERT INTO permissions (name, description, category, is_active, is_system)
SELECT 'ADMIN_EXTERNAL_ACCESS', 'Acesso à administração externa (apenas ROOT)', 'ADMIN', true, true
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'ADMIN_EXTERNAL_ACCESS');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROOT' AND p.name = 'ADMIN_EXTERNAL_ACCESS'
ON CONFLICT (role_id, permission_id) DO NOTHING;