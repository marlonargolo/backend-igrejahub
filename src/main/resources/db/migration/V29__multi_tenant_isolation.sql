-- =============================================================================
-- V29: Isolamento Multi-Tenant por Igreja/Congregação
--
-- O que esta migration faz:
--   1. Adiciona congregation_id na tabela users
--   2. Preenche church_id e congregation_id para usuários existentes
--   3. Cria trigger de consistência (congregação deve pertencer à igreja do usuário)
--   4. Cria tabela de auditoria de mudanças de igreja/congregação
--   5. Adiciona índices para performance das queries de escopo
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Adicionar congregation_id em users (church_id já existe desde V26)
-- -----------------------------------------------------------------------------
ALTER TABLE users ADD COLUMN IF NOT EXISTS congregation_id BIGINT REFERENCES congregations(id);

-- -----------------------------------------------------------------------------
-- 2. Preencher church_id para usuários que ainda não têm
--    (usa user_church_access como fonte de verdade)
-- -----------------------------------------------------------------------------
UPDATE users u
SET church_id = (
    SELECT uca.church_id
    FROM user_church_access uca
    WHERE uca.user_id = u.id
    ORDER BY uca.id
    LIMIT 1
)
WHERE u.church_id IS NULL
  AND u.deleted = FALSE;

-- -----------------------------------------------------------------------------
-- 3. Preencher congregation_id para usuários vinculados a congregações
-- -----------------------------------------------------------------------------
UPDATE users u
SET congregation_id = (
    SELECT uca.congregation_id
    FROM user_congregation_access uca
    WHERE uca.user_id = u.id
    ORDER BY uca.id
    LIMIT 1
)
WHERE u.congregation_id IS NULL
  AND u.deleted = FALSE;

-- -----------------------------------------------------------------------------
-- 4. Índices de performance para queries de escopo multi-tenant
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_users_church_id       ON users(church_id)       WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_users_congregation_id  ON users(congregation_id) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_users_org_church       ON users(organization_id, church_id) WHERE NOT deleted;

-- -----------------------------------------------------------------------------
-- 5. Trigger: garante que congregation_id sempre pertença à church_id do usuário
--    Protege a consistência no nível do banco, independente da aplicação.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION validate_user_church_consistency()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.congregation_id IS NOT NULL THEN
        -- Se tem congregação, DEVE ter igreja
        IF NEW.church_id IS NULL THEN
            RAISE EXCEPTION 'Usuário com congregation_id deve ter church_id preenchido';
        END IF;
        -- A congregação deve pertencer à mesma igreja
        IF NOT EXISTS (
            SELECT 1 FROM congregations c
            WHERE c.id = NEW.congregation_id
              AND c.church_id = NEW.church_id
              AND c.deleted = FALSE
        ) THEN
            RAISE EXCEPTION
                'Congregação % não pertence à Igreja % do usuário',
                NEW.congregation_id, NEW.church_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Remover trigger antigo se existir, recriar
DROP TRIGGER IF EXISTS trg_validate_user_church ON users;
CREATE TRIGGER trg_validate_user_church
    BEFORE INSERT OR UPDATE OF church_id, congregation_id ON users
    FOR EACH ROW EXECUTE FUNCTION validate_user_church_consistency();

-- -----------------------------------------------------------------------------
-- 6. Tabela de auditoria de mudanças de igreja/congregação
--    Registra toda vez que um usuário é movido entre igrejas/congregações.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_church_audit (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    old_church_id       BIGINT,
    new_church_id       BIGINT,
    old_congregation_id BIGINT,
    new_congregation_id BIGINT,
    reason              VARCHAR(255),          -- ex: "Transferência solicitada"
    changed_by          BIGINT NOT NULL,       -- user_id de quem fez a mudança
    changed_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_church_audit_user ON user_church_audit(user_id);
CREATE INDEX IF NOT EXISTS idx_user_church_audit_date ON user_church_audit(changed_at);

-- -----------------------------------------------------------------------------
-- 7. Permissões do sistema para isolamento (se ainda não existem)
-- -----------------------------------------------------------------------------
INSERT INTO permissions (name, description, category, is_active, is_system)
VALUES
  ('CHURCH_VIEW_ALL',   'Ver todas as igrejas da organização', 'SYSTEM', TRUE, TRUE),
  ('CONGREGATION_MANAGE', 'Criar e gerenciar congregações da própria igreja', 'CHURCH', TRUE, TRUE),
  ('USER_CHURCH_TRANSFER', 'Transferir usuário entre igrejas/congregações', 'USERS', TRUE, TRUE)
ON CONFLICT (name) DO NOTHING;

-- ROOT recebe as novas permissões
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROOT'
  AND p.name IN ('CHURCH_VIEW_ALL','CONGREGATION_MANAGE','USER_CHURCH_TRANSFER')
ON CONFLICT DO NOTHING;

-- ADMIN recebe CONGREGATION_MANAGE e USER_CHURCH_TRANSFER (mas não CHURCH_VIEW_ALL)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('CONGREGATION_MANAGE','USER_CHURCH_TRANSFER')
ON CONFLICT DO NOTHING;
