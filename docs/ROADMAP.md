# Roadmap do MECHANIUM

## Marco 0 — Fundação independente
- [x] Repositório próprio e branch de fundação
- [x] Maven multi-módulo e alvo Java 21
- [x] API pública versionada
- [x] Persistência SQLite
- [x] Motor inicial de eras e pesquisas
- [x] Módulos oficiais e módulos opcionais
- [x] Dependências declaradas entre módulos internos
- [x] Carregador de addons externos
- [x] Bootstrap e configuração Bukkit
- [x] Documentação e workflow de CI
- [x] Execução Maven verify aprovada em revisão anterior
- [ ] CI aprovado para a revisão atual
- [ ] Teste real em Paper 1.21.11
- [ ] Teste real em Purpur 1.21.11

## Marco 1 — Core de conteúdo
- [x] Registro persistente de categorias, itens e receitas
- [x] Itens com PDC, lore, modelo customizado e limite de pilha
- [x] Receitas shaped/shapeless e ingredientes personalizados
- [x] Integração do crafting com o motor central de progressão
- [x] Eventos públicos de registro e progressão para addons
- [x] Testes de persistência e validação do formato de receitas
- [ ] Livro/guia e interfaces de pesquisa
- [ ] Catálogo pesquisável de itens e tecnologias dentro do jogo

## Marco 2 — Sistemas econômicos e industriais
- [ ] Máquinas com inventário, proprietário, energia e manutenção
- [ ] Filas de produção com limites por tick
- [ ] Redes de energia e fluidos
- [ ] Armazéns, propriedade e logística
- [ ] Agricultura, geologia e processamento com gameplay funcional
- [ ] Persistência e recuperação de máquinas após reinício

## Marco 3 — Portabilidade
- [ ] Auditar individualmente licença, dependências, API e funcionalidades dos 31 projetos
- [ ] Implementar os domínios licenciados ou reimplementar de forma independente
- [ ] Adaptar sistemas ao contexto histórico-industrial e geopolítico
- [ ] Registrar recursos não portados, substituídos ou excluídos
- [ ] Criar testes de regressão por domínio
- [ ] Validar cada implementação em Paper e Purpur

## Marco 4 — Addon SDK
- [ ] Exemplo de addon Maven independente e compilável
- [x] API de eventos e registro de conteúdos
- [x] Validação de descriptor, dependências e ciclo de vida
- [ ] Ferramenta independente para validar o descriptor de um JAR
- [ ] Testes de dependências, ciclos, isolamento de classloader e falha de carregamento
- [ ] Documentação de migração entre versões da API
- [ ] Publicação da API em registry Maven

## Critérios de aceite por recurso
- Teste unitário da regra de negócio
- Teste de persistência e reinício
- Teste de integração em servidor
- Bloqueio antes e liberação após pesquisa
- Autorização de proprietário online/offline quando aplicável
- Proveniência e licença registradas quando houver código externo

O projeto só deve ser considerado pronto para produção quando os testes de servidor real e os critérios de aceite forem concluídos.
