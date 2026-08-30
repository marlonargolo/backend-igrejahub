-- V30: Permissões individuais por usuário + permissões de suporte

-- Tabela que persiste permissões extras por usuário (além das do role)
CREATE TABLE IF NOT EXISTS user_permissions (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    granted_by    BIGINT NOT NULL,
    granted_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_user_permissions_user ON user_permissions(user_id);

-- Permissões de Suporte (módulo novo)
INSERT INTO permissions (name, description, category, is_active, is_system)
VALUES
  ('SUPPORT_VIEW',   'Visualizar chamados de suporte',  'SUPPORT', true, true),
  ('SUPPORT_CREATE', 'Criar chamados de suporte',       'SUPPORT', true, true),
  ('SUPPORT_UPDATE', 'Atualizar chamados de suporte',   'SUPPORT', true, true),
  ('SUPPORT_MANAGE', 'Gerenciar todos os chamados',     'SUPPORT', true, true)
ON CONFLICT (name) DO NOTHING;

-- ROOT recebe as novas permissões
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROOT'
  AND p.name IN ('SUPPORT_VIEW','SUPPORT_CREATE','SUPPORT_UPDATE','SUPPORT_MANAGE')
ON CONFLICT DO NOTHING;

-- ADMIN recebe suporte básico
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('SUPPORT_VIEW','SUPPORT_CREATE','SUPPORT_UPDATE')
ON CONFLICT DO NOTHING;