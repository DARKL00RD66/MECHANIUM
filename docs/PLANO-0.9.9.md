# Plano de consolidação — MECHANIUM 0.9.9

**Versão de trabalho:** `0.9.9.00-SNAPSHOT`  
**Base auditada:** `0.7.4.02-SNAPSHOT`  
**Alvo:** Minecraft 1.21.11, Java 21, Paper e Purpur  
**Regra de publicação:** não publicar snapshots intermediários. As etapas 0.8.x e 0.9.x são marcos internos de execução; a linha pública de trabalho permanece 0.9.9.00-SNAPSHOT até o pré-lançamento.

Este documento separa o que existe no código, o que está limitado e o que ainda precisa ser implementado. O CI Java 21/Maven passou no commit `7b62dc782903f162b797d900a10c67b028428d1e` com 91 testes; as alterações posteriores de preservação de carga e interface ainda aguardam CI. Nenhuma aprovação automatizada substitui testes dentro de Paper e Purpur reais.

## 1. Auditoria inicial do código existente

| Sistema | Estado confirmado na base | Lacuna para 0.9.9 |
|---|---|---|
| Definição de máquinas | Cada definição aceita 1–6 tipos de entrada e 1–3 produtos; custo energético por operação é parte da definição | Filas, processamento em lote contínuo, concorrência e blocos físicos adicionais |
| Bloco de máquina | O runtime atual aceita dispensadores | Separar o modelo de processo do tipo de bloco e ampliar os blocos suportados com segurança |
| Inventário de máquina | Primeiro slot é entrada e último slot é saída | Slots e buffers configuráveis para múltiplos insumos e produtos |
| Processamento | Um ciclo central por máquina, com progresso salvo no PDC | Filas, lotes, concorrência limitada e controle de throughput |
| Energia | Geradores, baterias e máquinas podem ser conectados por conduítes energéticos do mesmo proprietário; até 256 nós, somente chunks carregados | Testes funcionais em servidor, métricas de rede, telemetria e limites de throughput configuráveis |
| Fluidos | Tubos conectam tanques; a rede transporta o item MECHANIUM `purified_water` | Modelo de fluido tipado, quantidade, capacidade, válvulas e múltiplos fluidos |
| Logística | Conduítes e caixas transportam itens MECHANIUM entre caixas do mesmo proprietário | Filtros, prioridades, limites de transferência, múltiplos itens por ciclo e escalabilidade |
| Persistência | Definições ficam no SQLite; identidade, dono e progresso da máquina ficam no PDC do bloco | Migrações compatíveis, recuperação após reinício e testes de chunks/mundos |
| EMC | Saldo individual, dissolução e síntese já existem | Integração com energia, máquinas e logística; revisar limites e operações atômicas |
| Pesquisa | Pontos, tecnologias, pré-requisitos e pesquisas temporizadas existem | Auditar aquisição, custos, cancelamentos, offline e sincronização com módulos |
| Addons | Loader, API e dependências existem | Matriz de compatibilidade, falhas isoladas e testes de ciclo de vida |
| Módulos de conteúdo | Muitos domínios têm primeira implementação | Completar os módulos parciais; não confundir presença no catálogo com paridade funcional |

### Limitações técnicas importantes encontradas

- `MachineDefinition` e o runtime agora aceitam 1–6 tipos de entrada e 1–3 tipos de saída, distribuídos em seis slots de entrada e três de saída; o bloco físico ainda é `DISPENSER`.
- A rede elétrica agora possui conduítes HOPPER identificados, BFS por até 256 nós e isolamento por proprietário. O débito agregado calcula o plano antes de alterar carga; faltam testes em servidor e telemetria da rede.
- O gerenciador de fluidos movimenta somente o item customizado `purified_water` entre tanques, por tubos adjacentes.
- A rede industrial percorre até 128 conduítes e movimenta um item MECHANIUM por ciclo de transferência.
- O processamento atual não possui fila de ordens configurável nem um plano genérico de entradas/saídas.
- Ainda não há confirmação de teste funcional completo em Paper e Purpur 1.21.11. As verificações Maven/CI devem ser relatadas separadamente.

### Progresso da primeira entrega 0.9.9

- Modelo multi-entrada/multi-saída adicionado à API.
- Construtor e acessores antigos preservados para módulos e addons existentes.
- Runtime de dispensador atualizado para seis slots de entrada e três de saída.
- Custos energéticos por operação movidos da lista fixa de IDs para `MachineDefinition`; SQLite v8 migra as máquinas antigas e mantém o consumo anterior.
- Conduíte energético adicionada como item craftável; geradores, baterias e máquinas usam rede conectada com limite de 256 nós.
- Carga da bateria é armazenada no PDC do item quando desmontada e restaurada ao recolocar.
- Geradores são indexados em chunks carregados, evitando a varredura de todos os chunks a cada segundo.
- O clique na máquina mostra insumos, produtos, duração e custo de energia.
- Produção valida todos os insumos e planeja todas as saídas antes de debitar recursos.
- Migração SQLite v7 grava entradas/saídas normalizadas e lê linhas legadas pelos campos antigos.
- CI GitHub Actions #45484853405 passou em Java 21/Maven `clean verify`; 84 testes automatizados passaram (API 19, Storage 13, Engine 12, Modules 9, Addon Loader 2, Plugin 29).

## 2. Marcos internos de implementação

### Marco A — Fundação industrial (equivalente interno a 0.8.0)

- [x] Introduzir modelo de processo de máquina com 1–6 ingredientes e 1–3 produtos.
- [x] Manter compatibilidade de código com o construtor antigo de `MachineDefinition`.
- [x] Migrar SQLite de schema 6 para schema 7 com tabelas normalizadas de ingredientes/produtos; não perder definições antigas.
- [x] Atualizar validação de conteúdo para conferir todos os ingredientes, produtos, tecnologias e limites de pilha.
- [x] Reservar buffers separados de entrada e saída no inventário da máquina.
- [x] Fazer pré-validação integral dos ingredientes e da capacidade de saída antes de consumir qualquer item ou energia.
- [ ] Manter progresso e propriedade seguros ao quebrar, carregar e descarregar chunks.
- [x] Adicionar e executar testes de validação do modelo, persistência e migração de dados legados.

### Marco B — Rede de energia (equivalente interno a 0.8.1)

- [x] Introduzir conduíte energético identificado e protegido por proprietário.
- [x] Construir topologia por componentes conectados sem carregar chunks descarregados (limite atual: 256 nós).
- [x] Permitir geradores, baterias e máquinas conectados através de conduítes.
- [ ] Definir capacidade, taxa de transferência, perda/eficiência de gameplay e política de distribuição.
- [ ] Garantir débito atômico: uma máquina não pode consumir parcialmente energia nem duplicar energia em uma falha.
- [ ] Exibir carga, geração, consumo e bloqueios da rede para jogadores autorizados.
- [ ] Testar redes fragmentadas, limite de tamanho, chunks, explosões, pistões e propriedade.

### Marco C — Fluidos tipados (equivalente interno a 0.8.2)

- [ ] Criar modelo de fluido independente de ItemStack.
- [ ] Definir identificador, volume, capacidade, entrada/saída e compatibilidade de conexão.
- [ ] Introduzir válvulas, bombas e reservatórios com regras explícitas.
- [ ] Manter compatibilidade com tanques e tubos existentes; migrar o estado legado de água sem perda.
- [ ] Testar limites, mistura inválida, fluxo reverso, bloqueios e reinícios.

### Marco D — Logística e produção (equivalente interno a 0.8.3–0.8.4)

- [ ] Implementar filas e ordens de produção por máquina.
- [ ] Adicionar filtros de item, prioridades, throughput e limites por tick.
- [ ] Melhorar o balanceamento de transferência entre caixas.
- [ ] Permitir redes com mais de um tipo de recurso, mantendo isolamento por proprietário.
- [ ] Evitar varrer todos os chunks a cada ciclo; manter índice de nós carregados e reconstrução incremental.
- [ ] Integrar máquinas, energia e logística com contratos comuns e eventos observáveis.

### Marco E — Completar módulos (equivalente interno a 0.8.5)

- [ ] Mining Automation: definir e implementar a extração de jogo prometida; não anunciar mineração automática antes do teste.
- [ ] Slime Customizer: finalizar a interface administrativa para criação, edição e validação de conteúdo.
- [x] Head Collection: renomear o módulo cosmético para escopo local; remover qualquer dependência de plataforma externa e preservar os IDs persistentes legados por migração.
- [ ] Agricultura/estufas: validar crescimento, colheita e automação, não somente receitas e itens.
- [ ] Revisar máquinas e receitas declaradas pelos módulos para usar os novos buffers/processos.
- [ ] Criar testes de regressão para dependências, IDs, progressão e propriedade.

### Marco F — Integração geral (equivalente interno a 0.9.0)

- [ ] Integrar EMC a máquinas e fluxos de energia/logística sem permitir criação ou débito duplicado.
- [ ] Auditar aquisição e gasto de pontos científicos e pesquisas temporizadas.
- [ ] Unificar consultas de máquina, energia, fluidos, armazenamento, pesquisa e EMC na Wiki/HUD.
- [ ] Revisar todas as eras e requisitos de conteúdo com critérios consistentes.
- [ ] Revisar permissões administrativas e limites configuráveis.

### Marco G — Congelamento e estabilização (equivalente interno a 0.9.5)

- [ ] Congelar a inclusão de novos módulos e novas mecânicas.
- [ ] Executar testes integrados de todos os módulos ativados e desativados.
- [ ] Executar testes de migração a partir de bancos das versões anteriores.
- [ ] Executar teste prolongado de carga com máquinas, redes e inventários.
- [ ] Corrigir duplicação/perda de itens, bypass de progressão, permissões e operações não atômicas.
- [ ] Fechar documentação de configuração, API e instalação.

### Marco H — Pré-lançamento 0.9.9

- [ ] Maven `clean verify` verde no Java 21.
- [ ] Inicialização e desligamento testados em Paper 1.21.11.
- [ ] Inicialização e desligamento testados em Purpur 1.21.11.
- [ ] Teste de upgrade com cópia de banco legado; validar backup e migrações.
- [ ] Testar produção, filas, energia, fluidos e logística após reinícios.
- [ ] Testar propriedade, permissões, chunks descarregados e múltiplos jogadores.
- [ ] Confirmar que não há erros críticos ou graves conhecidos.
- [ ] Revisar LICENSE, atribuições e licenças de terceiros.
- [ ] Atualizar README, Wiki, changelog, status e notas do JAR.
- [ ] Gerar artefato de pré-lançamento associado ao commit validado.

## 3. Critérios de aceite obrigatórios

Uma funcionalidade só pode ser marcada como concluída quando tiver:

1. Implementação verificável no código.
2. Teste automatizado da regra principal e de seus casos-limite.
3. Teste de persistência/migração quando armazenar estado.
4. Verificação de autorização por era, tecnologia e proprietário, quando aplicável.
5. Teste integrado em Paper e Purpur 1.21.11.
6. Documentação que descreva limitações remanescentes.

## 4. Regras de execução

- Não criar snapshots públicos intermediários: consolidar o desenvolvimento na linha `0.9.9.00-SNAPSHOT`.
- Não alterar ou apagar dados de servidores durante migrações; sempre preservar o formato anterior ou fornecer migração reversível/backup.
- Não introduzir código de terceiros sem auditoria de licença, origem e atribuição.
- Não marcar integração como completa apenas por compilar.
- Priorizar correções estruturais sobre aumentar a quantidade de módulos.
- Manter o PR aberto e não criar release/tag final sem autorização explícita.
