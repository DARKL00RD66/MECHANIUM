# Estado real do projeto

**Versão:** 0.2.0-SNAPSHOT  
**Plataforma-alvo:** Minecraft 1.21.11, Java 21, Paper/Purpur  
**Distribuição planejada:** um único `MECHANIUM.jar`

## Implementado na fundação

- API pública para eras, tecnologias, progressão, módulos e addons.
- Registro persistente de categorias, itens, receitas, máquinas e regras de drops.
- Persistência SQLite com schema versionado até a versão 4; máquinas e drops também são restaurados no carregamento da API.
- Criação de `ItemStack` com identificador PDC, lore, modelo customizado e limite de pilha configurável.
- Validação de materiais de itens customizados sem exigir inicialização de servidor Bukkit; materiais de ar são rejeitados.
- Receitas Bukkit nativas (shaped e shapeless), incluindo ingredientes MECHANIUM exatos.
- Verificação central de tecnologia ao fabricar, usar, consumir e posicionar itens personalizados.
- Estado separado de pesquisa desbloqueada e autorização de uso segundo a era.
- Eventos públicos de registro de conteúdo e eventos de mudança de era, saldo científico e tecnologia desbloqueada.
- Persistência SQLite com WAL, transações para débito de pontos e proteção contra overflow.
- Motor inicial de eras, pontos científicos, pré-requisitos e autorização tecnológica.
- Módulos oficiais internos, com dependências declaradas e módulos opcionais.
- Módulos opcionais de fantasia e futuro, desativados por padrão.
- Carregador de addons externos com descriptor, validação de versão de API, dependências, classloaders e ciclo de vida; JARs inválidos ou com falhas de linkage são isolados sem interromper a descoberta dos demais.
- Gerenciador de módulos com ordenação topológica automática por dependências, detecção de ciclos e limpeza de módulos parcialmente inicializados.
- Registro transacional de conteúdo por proprietário (`module:<id>` / `addon:<id>`), rollback de registros quando `onEnable` falha e reconciliação das definições que deixam de ser declaradas.
- Migração SQLite v4 com tabela de ownership; remove no bootstrap conteúdo pertencente a módulos/addons que não carregaram. A migração adota IDs conhecidos dos módulos oficiais antigos; conteúdo externo legado sem ownership é preservado por segurança.
- Bootstrap Bukkit/Paper, comandos iniciais e configuração.
- Menu de inventário com baús, árvore de pesquisas por era, páginas, pré-requisitos e desbloqueio por clique.
- Livro-guia entregue automaticamente no inventário ao entrar; clique direito abre o menu.
- Integração Towny opcional: sincroniza UUID/nome da nação do jogador e publica o serviço `NationIdentityProvider`.
- Administração simplificada: lista de UUIDs em `staff.admins`, comandos para adicionar/remover equipe e alterar era padrão, de todos online ou de um jogador.
- Módulo `culinary`: primeira cadeia de horticultura, culinária e conservação de alimentos inspirada em referências agrícolas/culinárias.
- Módulo `ore-processing`: primeira cadeia de peneiramento, britagem e refino; sem geração de depósitos regionais ainda.
- Sete novas integrações por módulos independentes: `extra-gear`, `eco-power`, `sfcalc`, `liquid`, `advanced-tech`, `simple-storage` e `sensible-toolbox`.
- `liquid`: filtro de água funcional construído sobre o runtime atual de dispensers.
- `advanced-tech`: montadora industrial adicional, processando peças mecânicas em peças de precisão.
- `simple-storage`: segundo tipo de armazém protegido, mantendo proprietário e usuários autorizados após reinícios.
- `sfcalc`: comando `/mechanium calc <id-da-receita>` para consultar resultado, ingredientes e bloqueio de progressão.
- `extra-gear` e `sensible-toolbox`: equipamentos e kits com itens/receitas próprios; ainda utilizam atributos vanilla.
- `eco-power`: componentes e tecnologias eólicas/solares; geração contínua e rede elétrica ainda não implementadas.
- Receitas criadas por addons após a inicialização são encaminhadas automaticamente ao registro de crafting; receitas do MECHANIUM são removidas no desligamento.
- Primeiro sistema funcional do domínio PrivateStorage: barril privado com proprietário UUID persistente, até 32 usuários autorizados, comandos de confiança, proteção contra quebra não autorizada, funis, pistões e explosões.
- API extensível de drops de criaturas: definições públicas com entidade, item de saída, intervalo de quantidade, chance e tecnologia exigida; listener acrescenta subprodutos sem remover drops vanilla.
- Primeira máquina funcional: prensa mecânica em dispenser com inventário de entrada/saída, proprietário e progresso persistentes em PDC, processamento a cada segundo e autorização central por pesquisa/era. A API rejeita tipos de bloco que o runtime atual ainda não consegue processar.
- **Prospecção geológica inicial:** módulo oficial `geology`, pesquisa `geological_surveying` e kit de prospecção que conta minérios em uma área de 9×9×9 ao redor do bloco selecionado. O scanner ignora chunks não carregados para não gerar terreno; há cooldown e testes unitários da agregação.
- Empacotamento Maven em um JAR com módulos e dependências de runtime.

## Limitações e trabalho pendente

- A prospecção atual é um levantamento local simples; ainda não há geração de depósitos, veios regionais, amostras, mapas geológicos ou integração com mineração.
- Os sete módulos recém-adicionados são primeiras integrações funcionais de conteúdo, não substituições completas dos addons de referência. Consulte `docs/MODULOS.md` e `docs/INTEGRACOES_ADDONS.md`.
- O runtime de máquinas suporta somente a prensa em bloco dispenser, com uma entrada e uma saída; ainda faltam tipos de máquinas, receitas múltiplas, energia, manutenção e filas de produção.
- Redes de energia e fluidos ainda não existem; EcoPower fornece componentes e Liquid contém somente filtragem de água. Simple Storage ainda não conecta inventários entre locais.
- Detalhes visuais e páginas pesquisáveis para catálogo de itens, receitas e máquinas.
- Economia de recursos e balanceamento completo.

- Publicação da API em registry Maven remoto e SDK de exemplo.
- Testes de integração com addons de terceiros.
- Testes de inicialização e gameplay em servidor real Paper/Purpur 1.21.11.
- Auditoria individual e portabilidade funcional dos 31 projetos de referência; acompanhar `docs/INTEGRACOES_ADDONS.md`.

## Validação

- Workflow [GitHub Actions — run 43349904209](https://github.com/davitremea1-ops/MECHANIUM/actions/runs/43349904209): **BUILD SUCCESS** no commit `a2227e55f6820f3866a86ea1b848f96a64b09899`.
- Testes automatizados: **49 executados, 0 falhas, 0 erros** (16 API/modelos, 10 SQLite, 8 motor de progressão/ownership, 6 módulos, 2 classloaders de addons e 7 testes do plugin).
- As três etapas Maven de verificação, instalação local da API e compilação do addon de exemplo terminaram com `BUILD SUCCESS`.
- O job de distribuição também terminou com sucesso e publicou `MECHANIUM-0.2.0-SNAPSHOT.jar` no [GitHub Pre-release](https://github.com/davitremea1-ops/MECHANIUM/releases/tag/snapshot).
- Ainda não realizado: inicialização em Paper/Purpur real, testes funcionais de gameplay e validação em servidor de produção.

## Classificação

**Fundação em desenvolvimento; build e 47 testes automatizados aprovados no commit `3616923`, mas não pronta para produção.** A aprovação Maven não substitui smoke test nem validação funcional em servidor.
