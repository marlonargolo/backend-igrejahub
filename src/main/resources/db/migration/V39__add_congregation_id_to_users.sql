-- V39: Adicionar congregation_id à tabela users
-- Permite que PASTOR_CONGREGACAO seja identificado por congregação no JWT

ALTER TABLE users ADD COLUMN IF NOT EXISTS congregation_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_users_congregation_id
    ON users(congregation_id)
    WHERE congregation_id IS NOT NULL;

COMMENT ON COLUMN users.congregation_id IS
    'Congregação do usuário. null = acesso à Igreja toda. '
    'Preenchido para PASTOR_CONGREGACAO e usuários de congregação específica.';