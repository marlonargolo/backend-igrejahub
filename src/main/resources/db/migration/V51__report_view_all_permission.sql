-- V51: permissão individual para o pastor responsável pela sede consultar
-- relatórios de todas as Congregações da Igreja mesmo quando sua sessão
-- está "dentro" de uma Congregação específica (ex.: acesso restrito a
-- Congregações vinculadas). Concedida manualmente via a tela de Permissões
-- (nenhum perfil ganha isso por padrão).

INSERT INTO permissions (name, description, category, is_active, is_system) VALUES
('REPORT_VIEW_ALL', 'Visualizar relatórios de todas as Congregações', 'REPORTS', true, true)
ON CONFLICT (name) DO NOTHING;
