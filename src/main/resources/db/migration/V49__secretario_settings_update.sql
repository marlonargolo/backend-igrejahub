-- V49: SECRETARIO ganha SETTINGS_UPDATE — necessário para enviar/excluir
-- documentos em /secretaria/documentos (antes exigia ROOT_ACCESS, por isso
-- a Secretaria nunca conseguia usar o upload, só visualizar).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SECRETARIO' AND p.name = 'SETTINGS_UPDATE'
ON CONFLICT DO NOTHING;
