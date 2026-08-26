-- Adicionar campo de comprovante nas transações financeiras
ALTER TABLE financial_transactions
    ADD COLUMN IF NOT EXISTS attachment_url VARCHAR(500);

-- Tabela de chamados de suporte
CREATE TABLE IF NOT EXISTS support_tickets (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    church_id       BIGINT,
    title           VARCHAR(300) NOT NULL,
    category        VARCHAR(100) NOT NULL,
    priority        VARCHAR(20) NOT NULL DEFAULT 'MEDIA',
    status          VARCHAR(30) NOT NULL DEFAULT 'ABERTO',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT DEFAULT 0
);

-- Tabela de mensagens dos chamados
CREATE TABLE IF NOT EXISTS support_messages (
    id        BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES support_tickets(id) ON DELETE CASCADE,
    user_id   BIGINT REFERENCES users(id),
    author    VARCHAR(200) NOT NULL,
    message   TEXT NOT NULL,
    type      VARCHAR(20) NOT NULL DEFAULT 'cliente',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de notificações do usuário
CREATE TABLE IF NOT EXISTS notifications (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    title           VARCHAR(300) NOT NULL,
    body            TEXT,
    type            VARCHAR(50) DEFAULT 'INFO',
    read            BOOLEAN DEFAULT FALSE,
    link            VARCHAR(500),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_support_tickets_org    ON support_tickets(organization_id);
CREATE INDEX IF NOT EXISTS idx_support_messages_ticket ON support_messages(ticket_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user      ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_unread    ON notifications(user_id, read);
