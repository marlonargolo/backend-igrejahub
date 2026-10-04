-- V47: histórico de transferências de membro entre Congregações da mesma
-- Igreja (a regra que proíbe transferir entre Igrejas/Congregações
-- diferentes da mesma Igreja permanece — validada no backend, não aqui).

CREATE TABLE IF NOT EXISTS member_transfers (
    id                   BIGSERIAL PRIMARY KEY,
    organization_id      BIGINT    NOT NULL,
    church_id            BIGINT    NOT NULL REFERENCES churches(id),
    member_id            BIGINT    NOT NULL REFERENCES members(id),
    from_congregation_id BIGINT    REFERENCES congregations(id),
    to_congregation_id   BIGINT    NOT NULL REFERENCES congregations(id),
    reason               TEXT,
    transferred_by       BIGINT    NOT NULL REFERENCES users(id),
    transferred_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_member_transfers_org_church ON member_transfers(organization_id, church_id);
CREATE INDEX IF NOT EXISTS idx_member_transfers_member     ON member_transfers(member_id);
