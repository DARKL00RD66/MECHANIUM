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
- Empacotamento Maven em um JAR com módulos e dependências de runtime.

## Limitações e trabalho pendente

- Máquinas funcionais, inventários de máquinas e ciclos de produção.
- Redes de energia, fluidos, logística e transferência entre blocos.
- Livro/guia e interfaces gráficas de pesquisa.
- Economia de recursos e balanceamento completo.
- Persistência de configurações avançadas e migrações de conteúdo entre versões.
- Auditoria individual e portabilidade funcional dos 31 projetos de referência.
- Publicação da API em registry Maven remoto e SDK de exemplo.
- Testes de integração com addons de terceiros.
- Testes de inicialização e gameplay em servidor real Paper/Purpur 1.21.11.

## Validação

- Há um workflow Java 21/Maven `clean verify` no GitHub Actions.
- A execução previamente inspecionada concluiu o Maven com `BUILD SUCCESS` e empacotou o JAR sombreado.
- O passo de upload de artefatos falhou por limite de armazenamento do GitHub Actions, mas está marcado como não bloqueante; por isso o job pode aparecer como sucesso sem disponibilizar o JAR.
- Alterações posteriores incluem novos eventos, testes de receitas, proteção contra overflow e dependências entre módulos; aguardam confirmação do CI da revisão mais recente.
- Ainda não realizado: inicialização em Paper/Purpur real, testes funcionais de gameplay e validação de addons externos.

## Classificação

**Fundação em desenvolvimento, compilável em execução anterior do CI; não pronta para produção.** A aprovação Maven não substitui smoke test nem validação funcional em servidor.
