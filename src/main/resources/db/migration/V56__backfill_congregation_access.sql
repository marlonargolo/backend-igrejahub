-- O Vídeo 4 corrigiu um bug de segurança: antes, access_main_church=true dava
-- acesso implícito a TODAS as Congregações da Igreja; agora, entrar numa
-- Congregação exige vínculo explícito em user_congregation_access, com ou
-- sem a sede habilitada.
--
-- Efeito colateral não previsto: usuários já existentes, que nunca tiveram
-- vínculo em user_congregation_access porque dependiam do comportamento
-- implícito antigo, ficaram sem conseguir entrar em nenhuma Congregação após
-- o deploy — sua configuração efetiva de acesso mudou sem ação deles.
--
-- Esta migration preserva o nível de acesso que cada usuário já tinha: para
-- todo usuário não-ROOT com access_main_church=true, sem congregation_id fixo
-- (não é usuário fixo de uma Congregação) e que AINDA NÃO tem nenhum vínculo
-- em user_congregation_access (ou seja, ninguém configurou nada
-- explicitamente para ele ainda), concede acesso a todas as Congregações da
-- própria Igreja — exatamente o que ele já conseguia acessar antes da
-- correção. Isso é só um ponto de partida: um administrador pode depois
-- restringir essa lista pela tela de "Unidades de acesso".
INSERT INTO user_congregation_access (user_id, congregation_id)
SELECT u.id, c.id
FROM users u
JOIN congregations c ON c.church_id = u.church_id AND c.deleted = false
WHERE u.deleted = false
  AND u.access_main_church = true
  AND u.congregation_id IS NULL
  AND u.church_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_congregation_access uca WHERE uca.user_id = u.id)
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur
    JOIN roles r ON r.id = ur.role_id
    WHERE ur.user_id = u.id AND r.name = 'ROOT'
  )
ON CONFLICT DO NOTHING;
