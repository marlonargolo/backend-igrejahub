ALTER TABLE members
    ADD COLUMN IF NOT EXISTS rg       VARCHAR(20),
    ADD COLUMN IF NOT EXISTS cpf      VARCHAR(14),
    ADD COLUMN IF NOT EXISTS cargo    VARCHAR(60),
    ADD COLUMN IF NOT EXISTS funcoes  VARCHAR(300);

UPDATE members SET cargo = role WHERE cargo IS NULL AND role IS NOT NULL;

CREATE TABLE IF NOT EXISTS member_occurrences (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    member_id       BIGINT NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    occurrence_date DATE   NOT NULL DEFAULT CURRENT_DATE,
    description     TEXT   NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    version         BIGINT DEFAULT 0,
    deleted         BOOLEAN DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    deleted_by      BIGINT
);

CREATE INDEX IF NOT EXISTS idx_member_occurrences_member ON member_occurrences(member_id);
CREATE INDEX IF NOT EXISTS idx_member_occurrences_org    ON member_occurrences(organization_id);
