CREATE TABLE finance_accounts (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    church_id BIGINT REFERENCES churches(id),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL, -- CHECKING, SAVINGS, CASH, OTHER
    bank_name VARCHAR(100),
    agency VARCHAR(20),
    account_number VARCHAR(30),
    initial_balance_cents BIGINT NOT NULL DEFAULT 0,
    current_balance_cents BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT
);

CREATE INDEX idx_finance_accounts_org ON finance_accounts(organization_id);