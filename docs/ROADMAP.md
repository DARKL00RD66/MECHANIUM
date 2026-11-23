# Roadmap do MECHANIUM

## Marco 0 — Fundação independente
- [x] Repositório próprio e branch de fundação
- [x] Maven multi-módulo e alvo Java 21
- [x] API pública versionada
- [x] Persistência SQLite com ownership transacional de conteúdo (schema v4)
- [x] Motor inicial de eras e pesquisas
- [x] Módulos oficiais e módulos opcionais
- [x] Dependências declaradas entre módulos internos
- [x] Ordenação topológica, rejeição de ciclos e rollback de módulos parcialmente iniciados
- [x] Carregador de addons externos e validação de descriptor/dependências
- [x] Bootstrap Bukkit, configuração e CI
- [ ] Teste real em Paper 1.21.11
- [ ] Teste real em Purpur 1.21.11

## Marco 1 — Core de conteúdo
- [x] Registro persistente de categorias, itens, receitas, máquinas e drops
- [x] Receitas shaped/shapeless e ingredientes personalizados
- [x] Integração do crafting com o motor central de progressão
- [x] Menu paginado de pesquisas por era
- [x] Livro-guia entregue automaticamente
- [ ] Catálogo pesquisável de itens, receitas e tecnologias dentro do jogo

## Marco 2 — Sistemas econômicos e industriais
- [x] Prensa mecânica inicial em dispenser
- [x] Montadora industrial, britador, refinaria e filtro de água
- [x] Controle de máquinas por proprietário e progressão
- [x] Geradores eólicos/solares e baterias com armazenamento e consumo nas máquinas
- [x] Tubulações adjacentes e tanques com transporte de água purificada
- [x] Cofres privados com proprietário e lista de acesso
- [x] Rede local paginada de armazenamento com até 64 nós adjacentes
- [x] Cálculo recursivo de lotes e materiais por receita
- [x] Habilidades utilitárias para ferramentas e kits
- [ ] Framework geral de máquinas com múltiplos tipos de bloco, múltiplos insumos e múltiplos produtos
- [ ] Filas de produção, múltiplos insumos/produtos e limites configuráveis por tick
- [ ] Redes de energia extensíveis por cabos e distribuição entre várias máquinas
- [ ] Sistema geral de fluidos com líquidos adicionais e válvulas
- [x] Primeira rede local de conduítes e caixas para transporte de itens MECHANIUM (limite 128 conduítes)
- [ ] Rede de armazéns entre locais distantes e logística automatizada
- [ ] Ampliar armazenamento portátil com mais tiers, filtros e upgrades adicionais (primeiro upgrade de 54 espaços implementado)
- [ ] Economia monetária, preços e integração com plugin de economia
- [ ] Depósitos minerais regionais, veios, amostras e integração com mineração
- [x] Primeira expansão de agricultura e culinária: sementes, substrato, colheita embalada, conservas e alimentos processados\n- [x] Primeira expansão ExoticGarden: mistura de sementes, ervas, extrato botânico, conserva tropical e caixote de pomar\n- [x] Primeira expansão DankTech2: upgrade de armazém portátil persistente de 27 para 54 espaços
- [x] Moinho e conservadora industriais alimentados por energia
- [x] Armazém portátil individual de 27 espaços persistido no item
- [x] Contador abstrato de impacto industrial por mundo e comando de consulta
- [x] Desgaste de máquinas, manutenção com ferro e bônus de eficiência por pesquisa
- [x] Incubadoras elétricas para animais passivos, com ovos produzidos por máquinas e energia
- [x] Prensa rápida e enrolador de bobinas com energia e ciclos acelerados
- [x] Kit de manutenção por pesquisa e regras configuráveis de dificuldade hardcore
- [ ] Agricultura completa: crescimento de culturas customizadas, estufas funcionais e colheita mecanizada
- [ ] Persistência/reconstrução de máquinas e redes após reinício com teste de servidor
- [ ] Efeitos ativos para filtros de carbono, reciclagem e recuperação ambiental
- [ ] Balanceamento avançado: níveis de desgaste, falhas configuráveis e manutenção escalonada

## Marco 3 — Portabilidade
- [x] Matriz inicial de 31 referências e auditoria preliminar de licenças
- [x] Primeiras cadeias independentes de culinária e beneficiamento mineral
- [x] Hook Towny opcional com UUID da nação exposto pela API
- [x] Primeiras funcionalidades jogáveis dos domínios ExtraGear, EcoPower, SFCalc, Liquid, AdvancedTech, Simple Storage, SensibleToolbox e ExoticGarden
- [x] Primeira etapa de ElectricSpawners, FastMachines e HardcoreSlimefun
- [ ] Completar paridade de funcionalidades e sistemas transversais dos addons, incluindo crescimento de culturas exóticas
- [ ] Auditar individualmente licença, dependências, API e funcionalidades dos 31 projetos
- [ ] Adaptar sistemas ao contexto histórico-industrial e geopolítico
- [ ] Criar testes de regressão por domínio
- [ ] Validar cada implementação em Paper e Purpur

## Marco 4 — Addon SDK
- [x] Exemplo Maven independente e compilável
- [x] API de eventos e registro de conteúdos
- [x] Validação de descriptor, dependências e ciclo de vida
- [ ] Ferramenta independente para validar descriptor de JAR
- [ ] Testes abrangentes de addons reais e falhas de carregamento em servidor
- [ ] Documentação de migração entre versões da API
- [ ] Publicação da API em registry Maven

## Critérios de aceite por recurso
- Teste unitário da regra de negócio
- Teste de persistência e reinício
- Teste de integração em servidor
- Bloqueio antes e liberação após pesquisa
- Autorização de proprietário online/offline quando aplicável
- Proveniência e licença registradas para qualquer código externo

O projeto só deve ser considerado pronto para produção depois da CI, dos testes funcionais em Paper/Purpur e da validação dos critérios de aceite.


## 0.7.0-SNAPSHOT — Expansão de addons
- Primeira etapa do domínio Networks: conduítes e caixas com transporte local de itens MECHANIUM.
- Primeira etapa de Better Nuclear Reactor: gerador cristalino abstrato de jogo, sem simulação real.
- Forestry, Textile Industry, Biofuel e Construction Materials como módulos oficiais opcionais.
- Próximos alvos de auditoria: Networks, Better Nuclear Reactor (somente abstração de jogo), GeyserHeads (compatibilidade de apresentação) e referências cuja identidade/licença ainda precisa ser confirmada.
