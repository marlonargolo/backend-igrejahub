-- Permite transferir um membro de volta para a sede (to_congregation_id NULL),
-- não só entre congregações. Histórico existente não é alterado.
ALTER TABLE member_transfers ALTER COLUMN to_congregation_id DROP NOT NULL;
