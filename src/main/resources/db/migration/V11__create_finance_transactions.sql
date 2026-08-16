CREATE TABLE financial_transactions (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    church_id BIGINT REFERENCES churches(id),
    congregation_id BIGINT REFERENCES congregations(id),
    account_id BIGINT REFERENCES finance_accounts(id),
    category_id BIGINT REFERENCES finance_categories(id),
    type VARCHAR(20) NOT NULL, -- REVENUE, EXPENSE
    description VARCHAR(255) NOT NULL,
    amount_cents BIGINT NOT NULL,
    transaction_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, CONFIRMED, CANCELLED
    payment_method VARCHAR(30),
    reference VARCHAR(100),
    notes VARCHAR(500),
    approved_by BIGINT,
    approved_at DATE,
    confirmed_at DATE,
    cancelled_at DATE,
    cancelled_by BIGINT,
    accounting_entry_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT
);

CREATE INDEX idx_financial_transactions_org ON financial_transactions(organization_id);
CREATE INDEX idx_financial_transactions_org_status ON financial_transactions(organization_id, status);
CREATE INDEX idx_financial_transactions_date ON financial_transactions(transaction_date);

CREATE TABLE financial_transfers (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    from_account_id BIGINT NOT NULL REFERENCES finance_accounts(id),
    to_account_id BIGINT NOT NULL REFERENCES finance_accounts(id),
    amount_cents BIGINT NOT NULL,
    transfer_date DATE NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT
);

CREATE INDEX idx_financial_transfers_org ON financial_transfers(organization_id);