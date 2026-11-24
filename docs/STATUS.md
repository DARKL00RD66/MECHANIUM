# Estado real do projeto

**Versão alvo de consolidação:** 0.9.9.00-SNAPSHOT  
**Base auditada:** 0.7.4.02-SNAPSHOT  
**Fase atual:** primeira entrega da fundação industrial implementada; CI Java 21/Maven aprovado; teste em servidor real pendente
**Progresso 0.9.9:** máquinas multi-entrada/multi-saída (6+3), custos de energia por definição, migração SQLite v8, conduítes energéticos, preservação da carga em baterias e índice de geradores por chunks carregados implementados. CI #45502503078 aprovou 91 testes; novas alterações de energia e interface aguardam CI. Paper/Purpur ainda não testados.  
**Plano:** [PLANO-0.9.9.md](PLANO-0.9.9.md)  
**Plataforma-alvo:** Minecraft 1.21.11, Java 21, Paper/Purpur  
**Distribuição planejada:** um único `MECHANIUM.jar`

## Fundação implementada

- API pública para eras, tecnologias, progressão, módulos e addons.
- Registro persistente de categorias, itens, receitas, máquinas e regras de drops.
- Definições de máquinas multi-entrada/multi-saída (1–6 entradas e 1–3 saídas), com runtime de dispensador e buffers dedicados.
- Persistência SQLite com schema versionado até a versão 8; era global, sobrescritas de teste, máquinas e drops são restaurados no carregamento da API.
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
- **EcoPower:** geradores eólico/solar, baterias de 10.000 unidades e conduítes que conectam geradores, baterias e máquinas por redes isoladas por proprietário.
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

- **ElectricSpawners:** quatro incubadoras para animais passivos (galinha, vaca, porco e ovelha), com insumos, tempo de processamento, pesquisa e custo de energia por ciclo definido na própria máquina.
- **FastMachines:** prensa rápida (20 ticks) e enrolador de bobinas (40 ticks), com dois produtos por lote e custo de energia por operação.
- **HardcoreSlimefun:** kit de manutenção desbloqueado por pesquisa, parâmetros configuráveis de desgaste e modo hardcore opcional (multiplicador de desgaste e reparo com ferro).
- Os três domínios são reimplementações independentes; nenhum JAR upstream do Slimefun foi adicionado como dependência.

## Expansão 0.7.0.01-SNAPSHOT

- **Material Generators (SMG):** oito geradores passivos; baú diretamente acima; um item a cada 200 ticks por padrão; progresso persistido no bloco; propriedade e proteção contra desmontagem por terceiros, explosões e pistões.
- **Progressão:** quatro geradores básicos exigem `basic_material_generation`; quatro geradores avançados exigem `advanced_material_generation`.
- **Configuração:** `modules.material-generators.enabled` e `modules.material-generators.interval-ticks`.
- **Limite:** produção em um único baú superior, sem rede de distribuição, filtros ou upgrades de velocidade nesta etapa.

## Limitações conhecidas

- O runtime de máquinas usa dispenser com seis slots de entrada e três de saída; cada definição suporta até seis tipos de ingrediente e três produtos. Filas e processamento paralelo ainda não existem.
- O sistema energético usa conduítes HOPPER identificados, até 256 nós por rede, propriedade por UUID e somente chunks carregados. Geradores distribuem carga entre baterias conectadas; máquinas debitam o custo definido em sua MachineDefinition. Baterias preservam a carga ao serem desmontadas e recolocadas.
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


Expansão em revisão: Forestry, Textile Industry, Biofuel e Construction Materials foram adicionados como módulos opcionais. PR #10 aguarda CI Java 21/Maven.


## Snapshot 0.7.1.00-SNAPSHOT

Quatro novos módulos foram registrados no catálogo: Water Treatment, Greenhouse, Industrial Recycling e Ore Chunks. Eles acrescentam 18 itens novos, 4 categorias, 8 tecnologias e 18 receitas de progressão. Os módulos são configuráveis individualmente. A validação Maven/CI e os testes de gameplay Paper/Purpur ainda precisam confirmar o funcionamento integrado.


## Snapshot 0.7.2.00-SNAPSHOT

Cinco módulos adicionais foram criados e registrados: Aquaculture, Glassworks, Rail Freight, Battery Industry e Cold Chain. Cada módulo inclui tecnologias de pesquisa, categoria, itens e receitas; as dependências entre módulos foram declaradas e os cinco IDs foram adicionados ao teste do catálogo e à configuração padrão. Aquaculture, Glassworks e Cold Chain também declaram máquinas no runtime genérico com buffers de até seis entradas e três saídas. Build CI e testes funcionais em Paper/Purpur 1.21.11 ainda precisam ser concluídos antes de considerar o snapshot validado.


## Auditoria de consistência — correções aplicadas
- Corrigidas dependências internas ausentes em Culinary, Air Filtration, Advanced Metallurgy, Pressure Engineering, Industrial Automation e Industrial Logistics, evitando falha de inicialização quando o módulo Industry estiver desativado.
- Battery Industry agora declara também a dependência direta de Energy, exigida por `electrical_grid`.
- Removida a colisão de `filter_cartridge` e `filter_cartridge_recipe` entre Liquid e Water Treatment por meio dos IDs exclusivos `water_filter_cartridge` e `water_filter_cartridge_recipe`.
- Geradores de materiais agora conseguem adicionar unidades a pilhas existentes mesmo quando o inventário do baú não possui slots vazios.
- Testes automatizados verificam que todos os pré-requisitos tecnológicos entre módulos possuem dependências declaradas e que módulos dependentes de módulos opcionais também são opcionais.
- Java 21 / Maven `verify` passou no workflow 45262585480. Testes reais de gameplay em Paper/Purpur 1.21.11 ainda estão pendentes.


## Snapshot 0.7.2.01-SNAPSHOT

Versão de manutenção que incorpora a auditoria de consistência documentada acima e atualiza o versionamento em todos os oito POMs Maven, incluindo o exemplo de addon. O workflow Java 21/Maven `clean verify` está sendo executado para esta revisão. O JAR distribuível deve ser obtido do artefato do workflow após conclusão bem-sucedida; ainda não há confirmação de teste em servidor Paper/Purpur 1.21.11.


## Expansão 0.7.3.00-SNAPSHOT

- **FN Amplifications:** núcleos, bobinas, módulos e estação de amplificação; a estação usa o runtime genérico com buffers multi-entrada/multi-saída.
- **Head Collection:** fichas cosméticas, cabeças de coleção e expositores locais; não exige nem integra serviços externos.
- **Slime Customizer:** páginas de modelo, projetos em branco, ferramenta e catálogo de conteúdo; a interface administrativa completa de criação/edição permanece pendente.
- **Industrial Chemistry:** reagentes abstratos, resina, fibra sintética e misturador industrial; não simula processos químicos reais.
- **Mining Automation:** componentes fictícios de prospecção e módulo minerador com montadora; não implementa extração autônoma de blocos nesta etapa.
- Os cinco módulos são opcionais, ativados por padrão e podem ser desligados individualmente em `config.yml`. Cada um registra duas tecnologias, uma categoria, quatro itens e quatro receitas. Três declaram máquinas no runtime genérico.
- Build Maven/CI Java 21 aprovado no workflow 45484853405 (84 testes automatizados). Testes reais em Paper/Purpur 1.21.11 ainda precisam ser realizados.


## Atualização de interface 0.7.3.01-SNAPSHOT

- Menu principal reorganizado com quatro entradas: Árvore de progressão, Eras históricas, Crafting e Wiki.
- Árvore de progressão abre na era efetiva do jogador. A tela Eras históricas organiza as eras em relógios selecionáveis; o item Estrela do Nether é exibido acima do relógio da era atual.
- A seleção de outra era permite consultar a respectiva árvore, sem conceder avanço ou alterar a era efetiva do jogador.
- Wiki de consulta separada, com listas paginadas e telas de detalhe para itens, receitas e máquinas. Ela não fabrica itens nem consome materiais.
- Crafting permanece como a única área da interface que efetua fabricação e consome ingredientes.
- A atualização foi compilada para validação por CI; teste visual e funcional dentro de Paper/Purpur 1.21.11 permanece necessário.


## Atualização 0.7.4.00-SNAPSHOT — Equivalência EMC

- Implementado módulo nativo opcional `emc-exchange`, com saldo EMC persistente por jogador em SQLite separado.
- Dissolvedores e Materializadores em cinco níveis; cada item dissolvido recebe um valor de EMC configurado e é aprendido automaticamente na primeira conversão.
- Materializadores sintetizam apenas itens já aprendidos pelo jogador e debitam o valor unitário do saldo.
- Limites de valor unitário por tier: 16, 64, 256, 1.024 e 4.096 EMC.
- Estações são instaladas como dispensadores identificados pelo PDC; somente o proprietário ou administradores podem usar/desmontar.
- Valores padrão iniciais cobrem alguns materiais vanilla e dois itens MECHANIUM. Itens não listados não podem ser dissolvidos.
- Ainda pendentes: integração de consumo/produção com a rede de energia, transporte automático por redes logísticas, PlaceholderAPI e suporte amplo a todos os itens/addons sem configuração manual.
- Build e validação dentro de Paper/Purpur 1.21.11 ainda precisam concluir.


## HUD EMC — 0.7.4.01-SNAPSHOT

- O HUD principal mostra o saldo EMC atual do jogador na área de resumo.
- Novo item **Carteira EMC**, identificado por esmeralda, exibe saldo pessoal e quantidade de itens aprendidos.
- Ao clicar, abre o painel EMC com catálogo aprendido e acesso às regras de dissolução e síntese.
- Se `emc-exchange` estiver desativado, o item aparece como indisponível.
- O saldo continua pessoal, isolado por UUID e persistido em `emc-exchange.db`.


## Auditoria de distribuição histórica — 0.7.4.02-SNAPSHOT

- Corrigido o classificador vanilla: antes, qualquer resultado não reconhecido caía em Antiguidade; agora itens desconhecidos usam Renascimento como faixa neutra, enquanto materiais e tecnologias conhecidos recebem uma era específica.
- A nova classificação separa Antiguidade, Idade Média, Renascimento, Revolução Industrial, Eletrificação, Era Moderna, Informação, Digital, Espacial, Mítica e Futuro Especulativo.
- Adicionada migração única para corrigir regras já semeadas pelo classificador antigo e cinco defaults empacotados incorretos. O console informa a quantidade de regras alteradas e a limitação de não distinguir escolhas manuais que coincidiam com um default antigo.
- Nos módulos próprios, as três tecnologias explicitamente antigas no núcleo industrial são Bronze, Irrigação e Alvenaria. O comentário incorreto que classificava Mecânica básica como Antiga foi corrigido; ela permanece na Renascença.
- Prensa mecânica, receita e máquina agora exigem Energia a vapor, da Revolução Industrial, porque a receita depende de pistão.
- Testes unitários cobrem classificação por era, fallback neutro, distinção entre redstone e pedra simples e distribuição das tecnologias próprias. A validação de servidor Paper/Purpur 1.21.11 ainda é necessária.
