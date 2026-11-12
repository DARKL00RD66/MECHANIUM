# Estado real do projeto

**Versão:** 0.1.0-SNAPSHOT  
**Plataforma-alvo:** Minecraft 1.21.11, Java 21, Paper/Purpur  
**Distribuição planejada:** um único `MECHANIUM.jar`

## Implementado na fundação

- API pública para eras, tecnologias, progressão, módulos e addons.
- Registro persistente de categorias, itens e receitas.
- Criação de `ItemStack` com identificador PDC, lore, modelo customizado e limite de pilha configurável.
- Receitas Bukkit nativas (shaped e shapeless), incluindo ingredientes MECHANIUM exatos.
- Verificação central de tecnologia ao fabricar, usar, consumir e posicionar itens personalizados.
- Estado separado de pesquisa desbloqueada e autorização de uso segundo a era.
- Eventos públicos de registro de conteúdo e eventos de mudança de era, saldo científico e tecnologia desbloqueada.
- Persistência SQLite com WAL, transações para débito de pontos e proteção contra overflow.
- Motor inicial de eras, pontos científicos, pré-requisitos e autorização tecnológica.
- Módulos oficiais internos, com dependências declaradas e módulos opcionais.
- Módulos opcionais de fantasia e futuro, desativados por padrão.
- Carregador de addons externos com descriptor, validação de versão de API, dependências, classloaders e ciclo de vida.
- Bootstrap Bukkit/Paper, comandos iniciais e configuração.
- Menu paginado de pesquisas dentro do jogo, com estados de era, pré-requisitos, saldo científico e desbloqueio via clique.
- Receitas criadas por addons após a inicialização são encaminhadas automaticamente ao registro de crafting; receitas do MECHANIUM são removidas no desligamento.
- Primeiro sistema funcional do domínio PrivateStorage: barril privado com proprietário UUID persistente, até 32 usuários autorizados, comandos de confiança, proteção contra quebra não autorizada, funis, pistões e explosões.
- API extensível de drops de criaturas: definições públicas com entidade, item de saída, intervalo de quantidade, chance e tecnologia exigida; listener acrescenta subprodutos sem remover drops vanilla.
- Primeira máquina funcional: prensa mecânica em dispenser com inventário de entrada/saída, proprietário e progresso persistentes em PDC, processamento a cada segundo e autorização central por pesquisa/era.
- Empacotamento Maven em um JAR com módulos e dependências de runtime.

## Limitações e trabalho pendente

- O runtime atual suporta somente a prensa em bloco dispenser, com uma entrada e uma saída; ainda faltam tipos de máquinas, receitas múltiplas, energia, manutenção e filas de produção.
- Redes de energia, fluidos, logística e transferência entre blocos.
- Livro/guia e interfaces gráficas de pesquisa.
- Economia de recursos e balanceamento completo.
- Persistência de configurações avançadas e migrações de conteúdo entre versões.
- Auditoria individual e portabilidade funcional dos 31 projetos de referência.
- Publicação da API em registry Maven remoto e SDK de exemplo.
- Testes de integração com addons de terceiros.
- Testes de inicialização e gameplay em servidor real Paper/Purpur 1.21.11.

## Validação

- Workflow: Java 21 / Maven `clean verify`.
- [GitHub Actions — run 43228562070](https://github.com/davitremea1-ops/MECHANIUM/actions/runs/43228562070): **BUILD SUCCESS** no commit `6104caeb14e17cc4d62d39c240ce73eb15be6de4`.
- Testes automatizados: **31 executados, 0 falhas, 0 erros** (13 API/modelos, 5 SQLite, 6 motor de progressão/conteúdo/máquinas/drops, 2 classloader de addons, 5 política de acesso ao armazenamento privado).
- O projeto principal passou por `mvn clean verify`; a API foi instalada no repositório Maven local e o addon de exemplo compilou e empacotou.
- O JAR sombreado foi gerado. O upload do artefato falhou por limite de armazenamento do GitHub Actions e está configurado como não bloqueante; não há download do JAR disponível neste run.
- Ainda não realizado: inicialização em Paper/Purpur real, testes funcionais de gameplay, integração de addons externos reais e validação em servidores de produção.

## Classificação

**Fundação em desenvolvimento; build e testes unitários aprovados no commit `6104cae`, mas não pronta para produção.** A aprovação Maven não substitui smoke test nem validação funcional em servidor.
