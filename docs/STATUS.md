# Estado real do projeto

**Versão:** 0.1.0-SNAPSHOT  
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
- Menu de inventário com baús, árvore de pesquisas por era, páginas, pré-requisitos e desbloqueio por clique.\n- Livro-guia entregue automaticamente no inventário ao entrar; clique direito abre o menu.\n- Integração Towny opcional: sincroniza UUID/nome da nação do jogador e publica o serviço `NationIdentityProvider`.\n- Administração simplificada: lista de UUIDs em `staff.admins`, comandos para adicionar/remover equipe e alterar era padrão, de todos online ou de um jogador.\n- Módulo `culinary`: primeira cadeia de horticultura, culinária e conservação de alimentos inspirada em referências agrícolas/culinárias.\n- Módulo `ore-processing`: primeira cadeia de peneiramento, britagem e refino; sem geração de depósitos regionais ainda.
- Receitas criadas por addons após a inicialização são encaminhadas automaticamente ao registro de crafting; receitas do MECHANIUM são removidas no desligamento.
- Primeiro sistema funcional do domínio PrivateStorage: barril privado com proprietário UUID persistente, até 32 usuários autorizados, comandos de confiança, proteção contra quebra não autorizada, funis, pistões e explosões.
- API extensível de drops de criaturas: definições públicas com entidade, item de saída, intervalo de quantidade, chance e tecnologia exigida; listener acrescenta subprodutos sem remover drops vanilla.
- Primeira máquina funcional: prensa mecânica em dispenser com inventário de entrada/saída, proprietário e progresso persistentes em PDC, processamento a cada segundo e autorização central por pesquisa/era. A API rejeita tipos de bloco que o runtime atual ainda não consegue processar.
- **Prospecção geológica inicial:** módulo oficial `geology`, pesquisa `geological_surveying` e kit de prospecção que conta minérios em uma área de 9×9×9 ao redor do bloco selecionado. O scanner ignora chunks não carregados para não gerar terreno; há cooldown e testes unitários da agregação.
- Empacotamento Maven em um JAR com módulos e dependências de runtime.

## Limitações e trabalho pendente

- A prospecção atual é um levantamento local simples; ainda não há geração de depósitos, veios regionais, amostras, mapas geológicos ou integração com mineração.
- O runtime de máquinas suporta somente a prensa em bloco dispenser, com uma entrada e uma saída; ainda faltam tipos de máquinas, receitas múltiplas, energia, manutenção e filas de produção.
- Redes de energia, fluidos, logística e transferência entre blocos.
- Detalhes visuais e páginas pesquisáveis para catálogo de itens, receitas e máquinas.
- Economia de recursos e balanceamento completo.

- Publicação da API em registry Maven remoto e SDK de exemplo.
- Testes de integração com addons de terceiros.
- Testes de inicialização e gameplay em servidor real Paper/Purpur 1.21.11.
- Auditoria individual e portabilidade funcional dos 31 projetos de referência; acompanhar `docs/INTEGRACOES_ADDONS.md`.

## Validação

- Workflow: Java 21 / Maven `clean verify`.
- [GitHub Actions — run 43327115991](https://github.com/davitremea1-ops/MECHANIUM/actions/runs/43327115991): **BUILD SUCCESS** no commit `36169239279c0a7a5b5fef05cd4a9190ed08175d`.
- Testes automatizados: **47 executados, 0 falhas, 0 erros** (16 API/modelos, 10 SQLite, 8 motor de progressão/ownership, 4 módulos, 2 classloaders de addons, 7 testes do plugin).
- O workflow executou `mvn clean verify`, instalou a API no repositório Maven local e compilou/empacotou o addon de exemplo. As três etapas Maven terminaram com `BUILD SUCCESS`.
- O endpoint de artefatos deste run não retornou um JAR disponível para download; portanto, não há link de binário confirmado.
- Ainda não realizado: inicialização em Paper/Purpur real, testes funcionais de gameplay, integração de addons externos reais e validação em servidores de produção.

## Classificação

**Fundação em desenvolvimento; build e 47 testes automatizados aprovados no commit `3616923`, mas não pronta para produção.** A aprovação Maven não substitui smoke test nem validação funcional em servidor.
