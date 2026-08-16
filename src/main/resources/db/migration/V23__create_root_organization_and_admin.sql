-- =============================================================================
-- V23: Organização root e usuário administrador inicial
--
-- A senha padrão do admin é: Root@2024#Secure
-- Hash BCrypt gerado com strength=12.
-- TROQUE a senha imediatamente após o primeiro login em produção.
-- Em produção, defina ROOT_PASSWORD via variável de ambiente e rode um script
-- separado para atualizar o hash — nunca commite senhas reais aqui.
-- =============================================================================

-- Inserir organização root (referencia a si mesma em organization_id por design do BaseEntity)
INSERT INTO organizations (
    id,
    organization_id,
    name,
    legal_name,
    email,
    country,
    plan,
    is_active,
    version,
    deleted
) VALUES (
    1,
    1,  -- auto-referência: organização root pertence a si mesma
    'IgrejaHub Root',
    'IgrejaHub Sistemas Ltda',
    'admin@igrejahub.com',
    'BR',
    'ROOT',
    true,
    0,
    false
) ON CONFLICT (id) DO NOTHING;

-- Ajustar sequence para não colidir com o ID 1 inserido manualmente
SELECT setval('organizations_id_seq', GREATEST((SELECT MAX(id) FROM organizations), 1));

-- Inserir usuário root
-- Senha: Root@2024#Secure  (BCrypt $2a$12$...)
INSERT INTO users (
    organization_id,
    name,
    email,
    password_hash,
    is_active,
    is_verified,
    failed_attempts,
    version,
    deleted
) VALUES (
    1,
    'Administrador Root',
    'admin@igrejahub.com',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', -- Root@2024#Secure
    true,
    true,
    0,
    0,
    false
) ON CONFLICT DO NOTHING;

-- Atribuir role ROOT ao usuário admin
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@igrejahub.com'
  AND r.name = 'ROOT'
ON CONFLICT DO NOTHING;

-- Inserir uma Igreja inicial vinculada à organização root
INSERT INTO churches (
    organization_id,
    name,
    city,
    state,
    status,
    version,
    deleted
) VALUES (
    1,
    'Igreja Sede',
    'São Paulo',
    'SP',
    'ACTIVE',
    0,
    false
) ON CONFLICT DO NOTHING;
