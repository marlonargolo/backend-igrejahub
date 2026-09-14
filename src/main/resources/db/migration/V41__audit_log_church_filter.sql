-- V41: Adiciona church_id aos logs de auditoria para permitir que a
-- administração externa (ROOT) filtre por Igreja e por período.

ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS church_id BIGINT REFERENCES churches(id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_church_id ON audit_logs(church_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_org_church ON audit_logs(organization_id, church_id);
