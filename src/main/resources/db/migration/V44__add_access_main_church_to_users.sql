-- V44: permite restringir um usuário a só suas Congregações vinculadas,
-- sem acesso à Igreja (sede) inteira. Default true preserva 100% do
-- comportamento atual para todo usuário já existente.

ALTER TABLE users ADD COLUMN IF NOT EXISTS access_main_church BOOLEAN NOT NULL DEFAULT true;
