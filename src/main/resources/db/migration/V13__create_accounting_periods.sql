CREATE TABLE IF NOT EXISTS accounting_periods (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    closed_at TIMESTAMP,
    closed_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT
);

CREATE INDEX idx_accounting_periods_org_id ON accounting_periods(organization_id);
CREATE INDEX idx_accounting_periods_dates ON accounting_periods(start_date, end_date);
CREATE INDEX idx_accounting_periods_status ON accounting_periods(status);
