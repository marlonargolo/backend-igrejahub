-- Padroniza o status de Membro em português. Escopo estrito: só a tabela
-- members — Congregation/Church/Asset continuam em ACTIVE/INACTIVE.
UPDATE members SET status = 'ATIVO'   WHERE status = 'ACTIVE';
UPDATE members SET status = 'INATIVO' WHERE status = 'INACTIVE';

ALTER TABLE members ALTER COLUMN status SET DEFAULT 'ATIVO';
