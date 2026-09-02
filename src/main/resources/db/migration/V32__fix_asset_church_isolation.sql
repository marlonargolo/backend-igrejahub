-- V32: Isolamento multi-tenant de patrimônio

-- Índices para queries de isolamento (colunas já existem na entidade)
CREATE INDEX IF NOT EXISTS idx_assets_church_id         ON assets(church_id)        WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_assets_congregation_id   ON assets(congregation_id)  WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_assets_org_church        ON assets(organization_id, church_id) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_assets_org_congregation  ON assets(organization_id, congregation_id) WHERE NOT deleted;

-- Corrigir bens sem church_id: vincular à primeira igreja da organização como fallback
-- (executar manualmente em produção se houver dados legados sem church_id)
-- UPDATE assets SET church_id = (
--     SELECT id FROM churches WHERE organization_id = assets.organization_id AND deleted = false LIMIT 1
-- ) WHERE church_id IS NULL AND deleted = false;