-- V46: documentos anexados ao membro (Ficha/Termo assinados, RG, CPF,
-- Comprovante de Residência, Outros), separado da tabela "documents" (que é
-- só para documentos enviados pelo ROOT a nível de Igreja/Congregação).

CREATE TABLE IF NOT EXISTS member_documents (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT       NOT NULL,
    member_id       BIGINT       NOT NULL REFERENCES members(id),
    document_type   VARCHAR(30)  NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_url        VARCHAR(500) NOT NULL,
    file_type       VARCHAR(100),
    file_size       BIGINT,
    uploaded_by     BIGINT       NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    version         BIGINT       DEFAULT 0,
    deleted         BOOLEAN      DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_member_documents_member ON member_documents(member_id) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_member_documents_org     ON member_documents(organization_id);
