CREATE TABLE IF NOT EXISTS congregations (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    church_id BIGINT NOT NULL REFERENCES churches(id),
    name VARCHAR(200) NOT NULL,
    city VARCHAR(100),
    state VARCHAR(2),
    address VARCHAR(255),
    pastor_id BIGINT,
    image_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT
);

CREATE INDEX idx_congregations_org_id ON congregations(organization_id);
CREATE INDEX idx_congregations_church_id ON congregations(church_id);
CREATE INDEX idx_congregations_status ON congregations(status);