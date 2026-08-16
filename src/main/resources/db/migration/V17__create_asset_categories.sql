CREATE TABLE IF NOT EXISTS asset_categories (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    parent_id BIGINT,
    depreciation_rate DOUBLE PRECISION,
    useful_life_years INTEGER,
    is_active BOOLEAN DEFAULT TRUE,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT
);

CREATE INDEX idx_asset_categories_org_id ON asset_categories(organization_id);
CREATE INDEX idx_asset_categories_parent_id ON asset_categories(parent_id);
CREATE INDEX idx_asset_categories_active ON asset_categories(is_active);
