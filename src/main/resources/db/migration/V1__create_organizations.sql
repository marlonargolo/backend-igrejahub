-- Organizations é a entidade raiz do multi-tenant.
-- organization_id referencia a si mesma (auto-referência) para satisfazer
-- o BaseEntity que exige a coluna — a organização root tem organization_id = id.
CREATE TABLE IF NOT EXISTS organizations (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,   -- auto-referência: preenchido após insert
    name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(200),
    cnpj VARCHAR(18),
    email VARCHAR(100),
    phone VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(2),
    zip_code VARCHAR(10),
    country VARCHAR(50) DEFAULT 'BR',
    logo_url VARCHAR(500),
    plan VARCHAR(50) DEFAULT 'FREE',
    is_active BOOLEAN DEFAULT TRUE,
    trial_end_date DATE,
    subscription_end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_organizations_org_id ON organizations(organization_id);
CREATE INDEX IF NOT EXISTS idx_organizations_cnpj ON organizations(cnpj);
CREATE INDEX IF NOT EXISTS idx_organizations_email ON organizations(email);
