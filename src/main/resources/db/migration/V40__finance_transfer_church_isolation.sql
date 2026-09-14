-- V40: Isolamento multi-tenant de transferências financeiras
--
-- financial_transfers não tinha church_id, permitindo que a listagem
-- (findByOrganizationId) retornasse transferências de TODAS as igrejas
-- da organização para qualquer usuário. Denormaliza church_id a partir
-- da conta de origem (contas nunca são compartilhadas entre igrejas).

ALTER TABLE financial_transfers ADD COLUMN IF NOT EXISTS church_id BIGINT REFERENCES churches(id);

UPDATE financial_transfers t
SET church_id = (
    SELECT a.church_id FROM finance_accounts a WHERE a.id = t.from_account_id
)
WHERE t.church_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_financial_transfers_church_id
    ON financial_transfers(church_id) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_financial_transfers_org_church
    ON financial_transfers(organization_id, church_id) WHERE NOT deleted;
