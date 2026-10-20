-- V50: Transferência financeira entre Igrejas diferentes
--
-- financial_transfers só guardava church_id (a Igreja de origem, via
-- from_account_id). Para permitir e listar corretamente transferências
-- entre contas de Igrejas diferentes, é preciso saber também a Igreja
-- de destino (a conta de destino pode pertencer a outra Igreja).

ALTER TABLE financial_transfers ADD COLUMN IF NOT EXISTS to_church_id BIGINT REFERENCES churches(id);

UPDATE financial_transfers t
SET to_church_id = (
    SELECT a.church_id FROM finance_accounts a WHERE a.id = t.to_account_id
)
WHERE t.to_church_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_financial_transfers_to_church_id
    ON financial_transfers(to_church_id) WHERE NOT deleted;
