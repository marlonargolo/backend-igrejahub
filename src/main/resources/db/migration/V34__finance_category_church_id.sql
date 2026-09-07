-- V34: Adicionar church_id às categorias financeiras para isolamento multi-tenant

ALTER TABLE finance_categories ADD COLUMN IF NOT EXISTS church_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_finance_categories_church_id
    ON finance_categories(church_id) WHERE NOT deleted;

-- Categorias existentes sem church_id ficam como "globais" (visíveis para todos)
-- Novas categorias criadas por igrejas terão church_id preenchido