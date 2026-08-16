CREATE TABLE IF NOT EXISTS journal_entries (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    church_id BIGINT,
    entry_number VARCHAR(30) NOT NULL,
    entry_date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, POSTED, CANCELLED
    reference VARCHAR(100)
);

CREATE UNIQUE INDEX idx_journal_entries_org_number ON journal_entries(organization_id, entry_number);
CREATE INDEX idx_journal_entries_org ON journal_entries(organization_id);
CREATE INDEX idx_journal_entries_date ON journal_entries(entry_date);
CREATE INDEX idx_journal_entries_status ON journal_entries(status);