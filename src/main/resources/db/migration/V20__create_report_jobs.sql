CREATE TABLE IF NOT EXISTS report_jobs (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(200) NOT NULL,
    type VARCHAR(50) NOT NULL,
    format VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    filters JSONB,
    file_url VARCHAR(500),
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX idx_report_jobs_org_id ON report_jobs(organization_id);
CREATE INDEX idx_report_jobs_user_id ON report_jobs(user_id);
CREATE INDEX idx_report_jobs_status ON report_jobs(status);