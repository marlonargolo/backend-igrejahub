CREATE TABLE IF NOT EXISTS assets (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    church_id BIGINT,
    congregation_id BIGINT,
    code VARCHAR(50),
    description VARCHAR(255) NOT NULL,
    category_id BIGINT,
    original_value_cents BIGINT,
    current_value_cents BIGINT,
    acquisition_date DATE,
    location VARCHAR(255),
    responsible_member_id BIGINT,
    responsible_user_id BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    notes VARCHAR(500),
    serial_number VARCHAR(100),
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    warranty_end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT
);

CREATE INDEX idx_assets_org_id ON assets(organization_id);
CREATE INDEX idx_assets_church_id ON assets(church_id);
CREATE INDEX idx_assets_congregation_id ON assets(congregation_id);
CREATE INDEX idx_assets_category_id ON assets(category_id);
CREATE INDEX idx_assets_status ON assets(status);
CREATE INDEX idx_assets_code ON assets(code);
