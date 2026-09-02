-- V31: Isolamento multi-tenant em chamados e notificações

-- 1. congregation_id nos tickets (church_id já existe na entidade)
ALTER TABLE support_tickets ADD COLUMN IF NOT EXISTS congregation_id BIGINT;
ALTER TABLE support_tickets ADD COLUMN IF NOT EXISTS deleted        BOOLEAN DEFAULT FALSE;

-- 2. church_id nas notificações
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS church_id BIGINT;

-- 3. Índices de performance
CREATE INDEX IF NOT EXISTS idx_support_tickets_church_id        ON support_tickets(church_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_congregation_id  ON support_tickets(congregation_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_user_id          ON support_tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_org_church       ON support_tickets(organization_id, church_id);
CREATE INDEX IF NOT EXISTS idx_notifications_church_id          ON notifications(church_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_church        ON notifications(user_id, church_id);