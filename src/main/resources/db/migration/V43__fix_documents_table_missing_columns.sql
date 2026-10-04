-- V43: a tabela "documents" (V37) foi criada sem as colunas que BaseEntity
-- espera em toda entidade (created_by, updated_by, version, deleted_at,
-- deleted_by) — qualquer SELECT feito pelo Hibernate falha com "column does
-- not exist", o que derrubava GET /documents com 500 para qualquer usuário.

ALTER TABLE documents ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE documents ADD COLUMN IF NOT EXISTS deleted_by BIGINT;
