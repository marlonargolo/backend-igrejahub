  -- V37: Tabela de documentos enviados pelo ROOT para igrejas/congregações

CREATE TABLE IF NOT EXISTS documents (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT        NOT NULL,
    church_id       BIGINT        NOT NULL REFERENCES churches(id),
    congregation_id BIGINT        REFERENCES congregations(id),
    title           VARCHAR(255)  NOT NULL,
    description     TEXT,
    file_name       VARCHAR(255)  NOT NULL,
    file_url        VARCHAR(500)  NOT NULL,
    file_type       VARCHAR(100),
    file_size       BIGINT,
    uploaded_by     BIGINT        NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    deleted         BOOLEAN       DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_documents_org_church       ON documents(organization_id, church_id)       WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_documents_org_congregation ON documents(organization_id, congregation_id)  WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_documents_church           ON documents(church_id)                         WHERE NOT deleted;