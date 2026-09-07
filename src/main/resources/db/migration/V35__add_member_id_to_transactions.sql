-- V35: Adicionar member_id em financial_transactions para vincular contribuições ao membro

ALTER TABLE financial_transactions ADD COLUMN IF NOT EXISTS member_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_financial_transactions_member_id
    ON financial_transactions(member_id);
CREATE INDEX IF NOT EXISTS idx_financial_transactions_church_type
    ON financial_transactions(church_id, type) WHERE NOT deleted;