CREATE TABLE IF NOT EXISTS churches (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    name VARCHAR(200) NOT NULL,
    city VARCHAR(100),
    state VARCHAR(2),
    address VARCHAR(255),
    zip_code VARCHAR(10),
    phone VARCHAR(20),
    email VARCHAR(100),
    cnpj VARCHAR(18),
    logo_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    pastor_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT
);

CREATE INDEX idx_churches_org_id ON churches(organization_id);
CREATE INDEX idx_churches_cnpj ON churches(cnpj);
CREATE INDEX idx_churches_status ON churches(status);