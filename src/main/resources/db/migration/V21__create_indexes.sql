-- Members: listagem filtrando por igreja + status (tela principal de membros)
CREATE INDEX idx_members_church_status ON members(church_id, status);
CREATE INDEX idx_members_congregation_status ON members(congregation_id, status);

-- Financeiro: relatórios por período e conta
CREATE INDEX idx_financial_transactions_account_date ON financial_transactions(account_id, transaction_date);
CREATE INDEX idx_financial_transactions_category ON financial_transactions(category_id);

-- Contábil: fechamento de período e razão por conta
CREATE INDEX idx_journal_entry_lines_account_entry ON journal_entry_lines(account_id, journal_entry_id);

-- Auditoria: consulta por usuário + ação num intervalo de tempo
CREATE INDEX idx_audit_logs_user_action ON audit_logs(user_id, action, created_at);

-- Congregações: geolocalização (usado pelo mapa de congregações, se aplicável)
CREATE INDEX idx_congregations_church_status ON congregations(church_id, status);