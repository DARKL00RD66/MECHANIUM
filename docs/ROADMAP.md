# Roadmap do MECHANIUM

## Marco 0 — Fundação independente
- [x] Repositório próprio e branch de fundação
- [x] Maven multi-módulo e alvo Java 21
- [x] API pública versionada
- [x] Persistência SQLite
- [x] Motor inicial de eras e pesquisas
- [x] Módulos oficiais e módulos opcionais
- [x] Carregador de addons externos
- [x] Bootstrap e configuração Bukkit
- [x] Documentação e workflow de CI
- [ ] CI aprovado
- [ ] Teste real em Paper 1.21.11
- [ ] Teste real em Purpur 1.21.11

## Marco 1 — Core de conteúdo
- Registro persistente de itens por NamespacedKey/PDC
- Catálogo de materiais e categorias
- Registro de receitas e validação de crafting
- Livro/guia e interfaces de pesquisa
- Eventos públicos para addons
- Testes de persistência após reinício

## Marco 2 — Sistemas econômicos e industriais
- Máquinas com inventário, proprietário, energia e manutenção
- Filas e limites por tick
- Redes de energia e fluidos
- Armazéns, propriedade e logística
- Agricultura, geologia e processamento

## Marco 3 — Portabilidade
- Auditoria individual de licença, dependências, API e funcionalidades dos 31 projetos
- Implementação por domínio com testes
- Relatório de recursos não portados ou excluídos
- Nenhum status COMPATÍVEL baseado apenas em compilação

## Marco 4 — Addon SDK
- Exemplo de addon Maven independente
- API de eventos e registro de conteúdos
- Ferramenta de validação do descriptor
- Testes de dependência, ciclos e falha de carregamento
- Documentação de migração entre versões da API

## Critérios de aceite por recurso
- Teste unitário da regra de negócio
- Teste de persistência e reinício
- Teste de integração em servidor
- Bloqueio antes e liberação após pesquisa
- Autorização de proprietário online/offline
- Proveniência e licença registradas quando houver código externo
