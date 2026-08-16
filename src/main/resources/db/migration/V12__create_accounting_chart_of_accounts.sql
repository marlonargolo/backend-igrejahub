CREATE TABLE IF NOT EXISTS chart_of_accounts (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(150) NOT NULL,
    account_type VARCHAR(20) NOT NULL, -- ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    parent_id BIGINT REFERENCES chart_of_accounts(id),
    level INTEGER NOT NULL DEFAULT 1,
    normal_balance VARCHAR(10) NOT NULL, -- DEBIT, CREDIT
    analytical BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX idx_chart_of_accounts_org_code ON chart_of_accounts(organization_id, code);
CREATE INDEX idx_chart_of_accounts_org ON chart_of_accounts(organization_id);
CREATE INDEX idx_chart_of_accounts_parent ON chart_of_accounts(parent_id);