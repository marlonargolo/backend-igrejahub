CREATE TABLE IF NOT EXISTS asset_movements (
    id BIGSERIAL PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    from_location VARCHAR(255),
    to_location VARCHAR(255) NOT NULL,
    movement_date DATE NOT NULL,
    responsible_user_id BIGINT,
    notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_asset_movements_asset_id ON asset_movements(asset_id);
CREATE INDEX idx_asset_movements_org_id ON asset_movements(organization_id);
