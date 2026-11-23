# Estado real do projeto

**Versão:** 0.5.0-SNAPSHOT  
**Plataforma-alvo:** Minecraft 1.21.11, Java 21, Paper/Purpur  
**Distribuição planejada:** um único `MECHANIUM.jar`

## Fundação implementada

- API pública para eras, tecnologias, progressão, módulos e addons.
- Registro persistente de categorias, itens, receitas, máquinas e regras de drops.
- Persistência SQLite com schema versionado até a versão 6; era global, sobrescritas de teste, máquinas e drops são restaurados no carregamento da API.
- Criação de `ItemStack` com identificador PDC, lore, modelo customizado e limite de pilha configurável.
- Receitas Bukkit nativas (shaped e shapeless), incluindo ingredientes MECHANIUM exatos.
- Verificação central de tecnologia ao fabricar, usar, consumir e posicionar itens personalizados.
- Estado separado de pesquisa desbloqueada e autorização de uso segundo a era.
- Eventos públicos de registro de conteúdo e eventos de mudança de era, saldo científico e tecnologia desbloqueada.
- Persistência SQLite com WAL, transações para débito de pontos e proteção contra overflow.
- Motor inicial de eras, pontos científicos, pré-requisitos e autorização tecnológica.
- Módulos oficiais internos, com dependências declaradas e módulos opcionais.
- Carregador de addons externos com descriptor, validação de versão de API, dependências, classloaders e ciclo de vida.
- Gerenciador de módulos com ordenação topológica automática por dependências, detecção de ciclos e limpeza de módulos parcialmente inicializados.
- Registro transacional de conteúdo por proprietário, rollback em falhas e reconciliação de definições que deixam de ser declaradas.
- Migração SQLite v4 com tabela de ownership; conteúdo externo legado sem ownership é preservado por segurança.
- Bootstrap Bukkit/Paper, comandos iniciais e configuração.
- Menu de inventário com baús, árvore de pesquisas por era, páginas, pré-requisitos e desbloqueio por clique.
- Livro-guia entregue automaticamente no inventário ao entrar; clique direito abre o menu.
- Integração Towny opcional: sincroniza UUID/nome da nação do jogador e publica o serviço `NationIdentityProvider`.
- Administração simplificada: equipe adicional e alteração da era padrão, de todos online ou de um jogador.
- Módulos `culinary` e `ore-processing`: primeiras cadeias de culinária e beneficiamento mineral.
- API extensível de drops de criaturas: subprodutos sem remover drops vanilla.
- Máquina inicial de prensa em dispenser com entrada/saída, proprietário, progresso persistente e autorização por pesquisa/era.
- Prospecção geológica local em volume 9×9×9, ignorando chunks descarregados.
- Empacotamento Maven em um JAR com módulos e dependências de runtime.

## Sete domínios ampliados em 0.3.0-SNAPSHOT

- **ExtraGear:** picareta reforçada com Haste breve após mineração e bússola para levantamento de coordenadas/biome/direção/bloco apontado.
- **EcoPower:** geradores eólico/solar, bateria com capacidade de 10.000 unidades, carga em rede de baterias adjacentes e consumo em máquinas.
- **SFCalc:** comando `/mechanium calc <receita> [quantidade]`, cálculo de lotes, produção e expansão recursiva de materiais intermediários até oito níveis.
- **Liquid:** filtro alimentado por energia, tanques e tubulações; rede do mesmo proprietário transfere água purificada a cada dois segundos.
- **AdvancedTech:** britador, refinaria e montadora; todos usam o runtime de máquina e exigem energia.
- **Simple Storage:** inventário virtual paginado para até 64 nós de barril conectados por faces, respeitando propriedade e jogadores confiáveis.
- **SensibleToolbox:** kit de campo com Speed/Night Vision temporárias e kit consumível para reparar equipamento em 25% da durabilidade máxima.
- **Agricultura/culinária:** novos insumos agrícolas, sementes, substrato, caixotes, queijo, ensopado, carne conservada e rações.
- **Máquinas alimentares:** moinho de trigo para farinha e conservadora de frutas, ambas alimentadas por energia.
- **Balanceamento industrial:** máquinas acumulam desgaste até 100 ciclos; proprietário repara agachando e usando uma barra de ferro. A pesquisa industrial_efficiency dobra o avanço do processamento por atualização.
- **Armazenamento portátil:** item único com 27 espaços, conteúdo serializado no PDC do item e salvo ao fechar a interface; depende do módulo opcional simple-storage.
- **Ambiente:** contador persistente de impacto industrial por mundo; cada ciclo de máquina soma uma unidade, exceto quando o proprietário possui emissions_control; comando /mechanium environment consulta o total.

## Expansões 0.4.0-SNAPSHOT

- **Catálogo vanilla:** descobre automaticamente receitas com namespace `minecraft` expostas pelo registro do Paper 1.21.11 e preenche requisitos de era para novas entradas sem sobrescrever regras existentes.
- **Painel vanilla:** lista as receitas suportadas em páginas, mostra resultado/tipo/requisito e permite avançar o requisito entre as eras.
- **Estações cobertas:** crafting de jogador, livro de receitas, autocrafter, processamento em fornalhas, corte de pedra e ferraria.
- **Mesa de encantamentos:** receita de fabricação exige Era Mítica; valor antigo padrão é migrado sem apagar configurações personalizadas.
- **ExoticGarden:** primeira cadeia de horticultura com sementes diversificadas, ervas, extrato botânico, conserva tropical e caixote de pomar.\n- **DankTech2:** primeira expansão de armazenamento portátil com upgrade para 54 espaços, mantendo a persistência no PDC do item.

## Atualizações 0.4.1-SNAPSHOT

- **Animação de menus:** abertura mais lenta e uniforme, revelando 3 itens de moldura e 2 de conteúdo a cada 2 ticks.
- **Pesquisas temporizadas:** cada tecnologia tem uma duração calculada por custo e era; o mínimo absoluto é de 120 segundos.
- **Duração configurável:** `progression.research.minimum-seconds`, `base-seconds`, `seconds-per-research-point`, `seconds-per-era` e sobrescritas em `overrides.<technology_id>`.
- **Persistência offline:** jobs ativos ficam em `mechanium_active_researches`; o horário final é absoluto e continua avançando offline e durante reinicializações. Os jobs são finalizados no ciclo online ou no próximo login.
- **Economia de pesquisa:** pontos são gastos quando a pesquisa começa; o desbloqueio ocorre somente depois de atingir o prazo.
- **Interface:** exibe duração estimada, estado, barra percentual e tempo restante; atualiza a cada segundo sem reiniciar a animação.
- **Compatibilidade:** `unlockTechnology` imediato da API permanece disponível para addons antigos; menu e comando do jogador usam o caminho temporizado.

## Expansões 0.5.0-SNAPSHOT

- **ElectricSpawners:** quatro incubadoras para animais passivos (galinha, vaca, porco e ovelha), com insumos, tempo de processamento, pesquisa e energia de bateria adjacente.
- **FastMachines:** prensa rápida (20 ticks) e enrolador de bobinas (40 ticks), com dois produtos por lote e consumo de energia.
- **HardcoreSlimefun:** kit de manutenção desbloqueado por pesquisa, parâmetros configuráveis de desgaste e modo hardcore opcional (multiplicador de desgaste e reparo com ferro).
- Os três domínios são reimplementações independentes; nenhum JAR upstream do Slimefun foi adicionado como dependência.

## Limitações conhecidas

- O runtime de máquinas ainda suporta apenas dispenser, uma entrada e uma saída; não há filas de produção nem múltiplos insumos/produtos. As incubadoras e máquinas rápidas herdam esse limite.
- O sistema energético é uma rede local e simplificada: geradores carregam bateria adjacente; baterias face a face podem fornecer energia a máquinas próximas. A carga se perde ao desmontar a bateria.
- O catálogo de receitas não inclui processos cujo resultado não esteja no registro de receitas suportado (por exemplo, operações especiais de bancada e cozimento em fogueira); itens sem receita registrada não recebem desbloqueio por crafting.\n- O sistema de fluidos transporta somente o item MECHANIUM `purified_water`, não líquidos vanilla ou outros fluidos.
- O Simple Storage conecta somente nós adjacentes pelas faces e limita cada rede a 64 depósitos.
- SFCalc calcula materiais, não valores monetários, e não altera o sistema de economia do servidor.
- Habilidades de kits/ferramentas são utilitárias e simples; não há árvore de atributos ou durabilidade customizada.
- Depósitos minerais regionais, veios e integração geológica profunda permanecem pendentes.
- Auditoria e paridade funcional dos 31 projetos de referência permanecem incompletas; cada portabilidade precisa respeitar licença e escopo.
- Não implementa sistemas operacionais de armas, ataques, mísseis ou interferência.

## Validação

- A versão 0.2.0-SNAPSHOT teve workflow Maven bem-sucedido no run [43349904209](https://github.com/davitremea1-ops/MECHANIUM/actions/runs/43349904209), com 49 testes automatizados sem falhas ou erros.
- A versão 0.3.0-SNAPSHOT passou no workflow [43352161651](https://github.com/davitremea1-ops/MECHANIUM/actions/runs/43352161651): **52 testes executados, 0 falhas e 0 erros**; os módulos Maven, a instalação local da API e a compilação do addon de exemplo terminaram com sucesso.
- O workflow também publicou `MECHANIUM-0.3.0-SNAPSHOT.jar` no [GitHub Pre-release](https://github.com/davitremea1-ops/MECHANIUM/releases/tag/snapshot). SHA-256: `adf591250a1785680a1f13e60b735b77fa56f41370a749856866dc85fdfb745b`.
- As novas incubadoras e máquinas rápidas ainda precisam de smoke test em Paper/Purpur 1.21.11, inclusive a confirmação em jogo de que os spawn eggs personalizados continuam utilizáveis como itens vanilla. Não considerar pronta para produção antes dessas etapas.

## Classificação

**Fundação em desenvolvimento; funcionalidades 0.5.0-SNAPSHOT aguardam CI e validação no servidor real.**
