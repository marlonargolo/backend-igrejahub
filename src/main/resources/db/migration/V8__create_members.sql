CREATE TABLE IF NOT EXISTS members (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    church_id BIGINT NOT NULL REFERENCES churches(id),
    congregation_id BIGINT REFERENCES congregations(id),
    name VARCHAR(200) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    birth_date DATE,
    gender VARCHAR(20),
    marital_status VARCHAR(30),
    profession VARCHAR(100),
    baptism_date DATE,
    member_since DATE,
    address VARCHAR(255),
    notes VARCHAR(500),
    avatar_url VARCHAR(500),
    role VARCHAR(30),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT
);

CREATE INDEX idx_members_org_id ON members(organization_id);
CREATE INDEX idx_members_church_id ON members(church_id);
CREATE INDEX idx_members_congregation_id ON members(congregation_id);
CREATE INDEX idx_members_status ON members(status);
CREATE INDEX idx_members_name ON members(name);