-- Tabela de acesso do usuário a igrejas específicas
CREATE TABLE IF NOT EXISTS user_church_access (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    church_id BIGINT NOT NULL REFERENCES churches(id) ON DELETE CASCADE,
    UNIQUE(user_id, church_id)
);

-- Tabela de acesso do usuário a congregações específicas
CREATE TABLE IF NOT EXISTS user_congregation_access (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    congregation_id BIGINT NOT NULL REFERENCES congregations(id) ON DELETE CASCADE,
    UNIQUE(user_id, congregation_id)
);

CREATE INDEX idx_user_church_access_user ON user_church_access(user_id);
CREATE INDEX idx_user_congregation_access_user ON user_congregation_access(user_id);
