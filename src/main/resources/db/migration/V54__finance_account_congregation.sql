-- Vídeo 2 (transferências financeiras): contas financeiras precisam distinguir
-- a sede das congregações da mesma Igreja, para permitir "Caixa da sede" e
-- "Caixa de Paranaguá" como destinos diferentes dentro da mesma Igreja.
--
-- Classificação das contas existentes: toda conta já cadastrada permanece
-- com congregation_id NULL, ou seja, segue pertencendo à sede (comportamento
-- idêntico ao atual). Nenhum saldo é movido ou redistribuído. Associar uma
-- conta existente a uma congregação específica é uma ação manual posterior,
-- não feita automaticamente por esta migração.
ALTER TABLE finance_accounts
    ADD COLUMN congregation_id BIGINT NULL REFERENCES congregations(id);

CREATE INDEX idx_finance_accounts_congregation_id ON finance_accounts(congregation_id);
