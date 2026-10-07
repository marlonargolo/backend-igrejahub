-- V48: catálogo de módulos do sistema + habilitação por Igreja (Admin
-- Externa → Módulos). Ausência de linha em church_modules = habilitado
-- (comportamento atual preservado para quem já existe).

CREATE TABLE IF NOT EXISTS modules (
    id          BIGSERIAL PRIMARY KEY,
    key         VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO modules (key, name, description) VALUES
  ('SECRETARIA',     'Secretaria',     'Gestão de membros, documentos e transferências'),
  ('TESOURARIA',     'Tesouraria',     'Receitas, despesas e transferências financeiras'),
  ('PATRIMONIO',     'Patrimônio',     'Cadastro e movimentação de bens'),
  ('CONTABILIDADE',  'Contabilidade',  'Fechamento mensal, exportação e demonstrações contábeis'),
  ('SUPORTE',        'Suporte',        'Chamados de suporte técnico')
ON CONFLICT (key) DO NOTHING;

CREATE TABLE IF NOT EXISTS church_modules (
    id        BIGSERIAL PRIMARY KEY,
    church_id BIGINT  NOT NULL REFERENCES churches(id),
    module_id BIGINT  NOT NULL REFERENCES modules(id),
    enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(church_id, module_id)
);

CREATE INDEX IF NOT EXISTS idx_church_modules_church ON church_modules(church_id);
