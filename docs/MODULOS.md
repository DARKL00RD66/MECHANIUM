# Módulos oficiais do MECHANIUM

### Novos módulos 0.6.0-SNAPSHOT

| ID | Conteúdo |
|---|---|
| `air-filtration` | Purificador de ar e cartuchos filtrantes; a receita do purificador consome um balde de água. |
| `advanced-metallurgy` | Componentes de aço e forno metalúrgico. |
| `pressure-engineering` | Placas compactadas e câmara de pressão. |
| `industrial-automation` | Circuitos de controle e montadora automática. |
| `industrial-logistics` | Caixas de transporte e roteador de carga. |

O catálogo de fabricação do menu central permite selecionar um resultado, conferir categoria, tecnologia e ingredientes e fabricar com um clique. A verificação e o consumo são feitos no servidor; itens personalizados são identificados pelo ID persistente, não apenas pelo nome visível. Máquinas são classificadas separadamente dos componentes e exibem seus processos registrados.\n\n## Módulos históricos e industriais padrão

| ID | Módulo | Padrão |
|---|---|---|
| `industry` | Máquinas e processos industriais | Ativado |
| `geology` | Prospecção e conhecimento mineral | Ativado |
| `agriculture` | Agricultura e produção alimentar | Ativado |\n| `exotic-garden` | Horticultura exótica, ervas, extratos e conservas | Ativado |\n| `dank-tech2` | Upgrade de armazém portátil para 54 espaços | Ativado |
| `energy` | Progressão energética | Ativado |
| `storage` | Armazéns e logística de estoque | Ativado |
| `environment` | Gestão ambiental e resíduos | Ativado |

### Geologia

O módulo `geology` depende de `industry` e adiciona a pesquisa `geological_surveying`. Depois de desbloqueá-la, o jogador pode fabricar o `geological_survey_kit` e usá-lo em um bloco para contar minérios em um volume de 9×9×9 ao redor do ponto selecionado. Chunks descarregados são ignorados, evitando geração de terreno durante a varredura.

A ferramenta atual é uma prospecção local. Depósitos regionais, veios minerais, amostras e integração profunda com mineração ainda estão pendentes.

## Módulos adicionais — 0.3.0 a 0.5.0-SNAPSHOT

| ID | Funcionalidade jogável |
|---|---|
| `extra-gear` | Picareta reforçada concede breve Haste após mineração; bússola de levantamento informa coordenadas, biome, direção e bloco observado. |
| `eco-power` | Geradores eólico/solar, baterias de 10.000 unidades e conduítes energéticos; rede por proprietário de até 256 nós em chunks carregados. |
| `sfcalc` | `/mechanium calc <receita> [quantidade]` calcula lotes, produção e materiais recursivos. |
| `liquid` | Filtro, tanques de fluido tipado e tubulações; água purificada e refrigerante em volumes de mB, sem mistura de tipos. |
| `advanced-tech` | Montadora, britador e refinaria industrial com receitas e processamento. |
| `simple-storage` | Nós de armazenamento conectados por faces, com inventário virtual paginado e proteção por proprietário/permissões. |
| `sensible-toolbox` | Kit de campo com efeitos temporários e kit de manutenção que repara equipamentos. |\n| `exotic-garden` | Sementes diversificadas, ervas desidratadas, extrato botânico, conservas tropicais e caixote de pomar. |\n| `dank-tech2` | Upgrade fabricável para um armazém portátil de 54 espaços; o item mantém os conteúdos no PDC. |

## Módulos adicionados em 0.5.0-SNAPSHOT

| ID | Funcionalidade jogável |
|---|---|
| `electric-spawners` | Incubadoras elétricas de galinha, vaca, porco e ovelha; exigem insumos, pesquisa e custo de energia por operação na rede do proprietário; produzem spawn eggs vanilla de animais passivos. |
| `fast-machines` | Prensa rápida (20 ticks) e enrolador de bobinas (40 ticks), ambos acelerados e alimentados pela rede de energia. |
| `hardcore-slimefun` | Pesquisas de confiabilidade e manutenção, kit reparador desbloqueável e regras hardcore opcionais para desgaste e custo de reparo. |

### ElectricSpawners — incubação pecuária

As quatro incubadoras usam o runtime comum de dispenser: sementes geram ovos de galinha; feno gera ovos de vaca; cenouras geram ovos de porco; trigo gera ovos de ovelha. A produção exige `livestock_incubation` e uma rede energética do mesmo proprietário com pelo menos uma unidade disponível por operação. O escopo é limitado a animais passivos e não inclui spawners de criaturas hostis.

### FastMachines — produção acelerada

A prensa rápida consome uma barra de ferro e produz duas peças mecânicas em 20 ticks; o enrolador consome cobre e produz duas bobinas em 40 ticks. Ambos exigem energia (duas unidades por operação), propriedade e a pesquisa `fast_machinery`. Ainda não há filas ou paralelismo.

### HardcoreSlimefun — manutenção e dificuldade

O item `machine_maintenance_kit` é desbloqueado por `preventive_maintenance` e restaura o desgaste da máquina quando o proprietário agacha e clica nela com o kit. O modo de dificuldade pode ser ativado em `machines.hardcore.enabled: true`; isso multiplica o desgaste por operação e aumenta o custo da reparação com barras de ferro. A configuração padrão mantém o modo hardcore desligado.

### DankTech2 — expansão de armazenamento portátil

O módulo `dank-tech2` depende de `simple-storage` e adiciona uma tecnologia de Era da Informação e um upgrade de armazém portátil:

- O armazém original `portable_storage` continua com 27 espaços.
- O novo `portable_storage_advanced` abre um inventário de 54 espaços.
- A receita de upgrade consome o armazém original e materiais de armazenamento avançado.
- Os dois níveis continuam salvando seus conteúdos no próprio item, sem banco externo por jogador.

Esta é a primeira etapa da referência DankTech2. Outras capacidades, filtros e tiers permanecem pendentes.

### ExoticGarden — horticultura exótica

O módulo depende de `agriculture` e adiciona uma primeira cadeia de horticultura inspirada na referência ExoticGarden:

- Mistura de sementes diversificadas, ervas desidratadas e extrato botânico.
- Conserva tropical produzida a partir de frutas e insumos agrícolas.
- Caixote de pomar para embalar a conserva.
- Tecnologias distribuídas entre Renascimento, Era Moderna e Era da Informação.

Esta etapa amplia itens e receitas; não adiciona ainda novas plantas com crescimento próprio, estufas funcionais ou colheita automática. Esses sistemas permanecem no roadmap.

### EcoPower — geração e rede de energia

- O conduíte energético é colocado como funil e conecta geradores, baterias e máquinas pelas faces.
- O gerador eólico é colocado como bloco de fornalha e produz 1 unidade de energia por segundo.
- O gerador solar é colocado como defumador e produz 2 unidades por segundo durante o dia, sem tempestade.
- Uma bateria é colocada como barril e armazena até 10.000 unidades.
- Geradores carregam baterias ligadas diretamente ou por conduítes HOPPER `energy_conduit`; máquinas conectadas à rede podem consumir energia. A rede é isolada por proprietário, suporta até 256 nós e examina apenas chunks carregados.
- Cada definição de máquina declara seu próprio `energyCostPerOperation` em `MachineDefinition`; custos típicos variam de 1 a 2 unidades por operação. A lista de máquinas não depende mais de nomes fixos em código.
- Máquinas acumulam desgaste por ciclo até 100; ao atingir o limite, param. O proprietário pode agachar e clicar com uma barra de ferro para reparar e zerar o desgaste.
- A rede atual é local, limitada a 256 nós e não carrega chunks automaticamente. Ao desmontar uma bateria, a carga é preservada no PDC do item e restaurada ao recolocar.

### Liquid — tanques de fluido tipado

- O filtro transforma um balde de água e um cartucho filtrante em um item MECHANIUM `purified_water`.
- Tanques usam barris e guardam o fluido no PDC como ID + volume em milibuckets (mB), com capacidade padrão de 16.000 mB.
- Cada unidade `purified_water` representa 250 mB; `purified_water_cell` representa 1.000 mB; `coolant_cell` representa 250 mB de `industrial_coolant`.
- O tanque aceita somente um tipo de fluido por vez. Água purificada e refrigerante não se misturam; o volume não ultrapassa a capacidade configurada.
- Para abastecer, clique no tanque com um item de fluido desbloqueado. Para retirar, agache e clique com a mão vazia; o sistema escolhe o maior recipiente de saída desbloqueado cujo volume caiba no tanque (250 mB, 500 mB, 1.000 mB ou outro volume registrado).
- Tubulações são dispensers identificados. Tanques do mesmo proprietário conectados por faces formam uma rede; a cada dois segundos podem transferir até 250 mB para outro tanque compatível.
- As redes examinam até 128 tubulações indexadas nos chunks carregados. Não carregam chunks automaticamente, não transportam fluidos vanilla e ainda não possuem válvulas ou bombas. Módulos e addons podem registrar fluidos e recipientes pela API pública `FluidDefinition`/`FluidContainerDefinition`. O item do recipiente e a tecnologia exigida devem estar registrados primeiro; o recipiente declara seu volume em múltiplos de 250 mB e se pode encher e/ou esvaziar.
- Itens de fluido legados encontrados no inventário de um tanque são convertidos para volume tipado quando o tanque é acessado/inspecionado; ao desmontar o tanque, tipo e volume são preservados no PDC do próprio item de tanque.
- O acesso aos tanques respeita o proprietário; a tubulação é um bloco de conexão, não um inventário acessível.

### SFCalc — cálculo de produção

`/mechanium calc <id-da-receita> [quantidade-final]` calcula a quantidade de lotes necessária, produção final e materiais. Receitas intermediárias MECHANIUM são expandidas recursivamente até oito níveis; receitas shaped contam as ocorrências de cada símbolo e receitas shapeless contam uma unidade de cada ingrediente registrado. Ciclos ou intermediários sem receita são exibidos como itens MECHANIUM. O comando calcula materiais, não preços monetários, e não substitui o bloqueio de progressão no crafting.

### AdvancedTech — indústria avançada

O britador converte minério britado em concentrado de ferro; a refinaria converte concentrado em liga refinada; a montadora converte peças mecânicas em peças de precisão. As três máquinas usam o runtime atual de dispenser, têm entrada/saída, progresso e proprietário persistentes e exigem energia. O runtime agora suporta até seis tipos de insumo e três produtos por lote; filas de produção e processamento paralelo continuam pendentes.

### Simple Storage — inventário conectado

Nós `simple_storage_node` colocados face a face, do mesmo proprietário e com permissões compatíveis, abrem um inventário virtual paginado. O limite é 64 nós por rede. O acesso respeita proprietário, jogadores confiáveis e administradores; a rede não conecta depósitos distantes nem atravessa blocos intermediários. Apenas chunks já carregados são inspecionados.

### ExtraGear e SensibleToolbox — habilidades utilitárias

- Picareta reforçada: aplica Haste breve ao jogador após uma mineração válida.
- Bússola de levantamento: exibe coordenadas, biome, direção e bloco apontado.
- Kit de campo: clique direito concede Speed temporária e Night Vision, com cooldown de 30 segundos.
- Kit de manutenção: consome o kit para reparar um item danificado do inventário em 25% da durabilidade máxima; cooldown de 10 segundos.
- Manual técnico: item de referência, sem interface de livro personalizada nesta versão.

## Módulos opcionais

| ID | Conteúdo | Padrão |
|---|---|---|
| `optional-fantasy` | Alquimia fictícia e tecnologias arcanas | Desativado |
| `optional-futuristic` | Tecnologias especulativas de futuro | Desativado |

Cada módulo opcional possui classe, IDs, era e pesquisas próprios. Não altera a progressão histórica padrão. Administradores podem ativá-los explicitamente em `plugins/MECHANIUM/config.yml`.

### Expansão agrícola e alimentar
- Agricultura: pacotes de sementes, substrato fértil, caixotes de colheita e progressão de cultivo protegido/colheita mecanizada. Nesta entrega, itens e receitas estão integrados à progressão; crescimento de plantações customizadas e colheita automática ainda não existem.
- Culinária: queijo artesanal, ensopado, carne conservada e caixas de rações, além das receitas anteriores.
- Maquinário alimentar: moinho industrial converte trigo em farinha MECHANIUM; conservadora converte frutas doces em conserva. Ambas usam o runtime de máquinas atual e exigem energia.

### Armazenamento e logística
- Simple Storage continua oferecendo redes locais de até 64 nós conectados por faces.
- Armazém portátil: item único com inventário de 27 espaços persistido no próprio item; clique direito abre e fechar salva o conteúdo. O módulo simple-storage é opcional na arquitetura e vem ativado na configuração padrão; pode ser controlado por modules.simple-storage.enabled.

### Ambiente e impacto industrial
- Cada ciclo de produção de máquina acrescenta uma unidade abstrata de impacto industrial ao mundo.
- O comando /mechanium environment consulta o contador do mundo atual (ou todos os mundos pelo console).
- Pesquisar emissions_control interrompe a contabilização de novas emissões das máquinas daquele jogador. O contador já acumulado não é reduzido.
- Filtro de carbono e kit de recuperação possuem receitas, mas ainda não têm efeito de instalação/uso no mundo.

## Limites conhecidos antes do teste de servidor

- O build automatizado pode verificar compilação e testes unitários, mas não confirma comportamento real em Paper/Purpur 1.21.11.
- Máquinas continuam hospedadas em dispenser; o runtime suporta até seis entradas e três saídas, mas ainda não possui filas nem processamento paralelo.
- Energia e fluidos são redes locais tipadas e limitadas; não são simuladores universais. A energia ainda não tem telemetria, perdas ou throughput configurável; fluidos usam definições customizadas registradas pela API pública (água purificada e refrigerante são exemplos padrão), sem fluidos vanilla, válvulas ou bombas.
- Armazenamento conectado exige nós face a face; não é uma rede logística de longa distância.
- Não há integração com economia para calcular preços monetários.
- Não há filas de produção, processamento paralelo ou múltiplas receitas simultâneas; manutenção de máquinas já existe via ferro e kit pesquisável.
- O conteúdo relacionado a armas, ataques, mísseis ou interferência não faz parte do escopo. Sistemas civis e industriais permanecem no escopo.


## Novos módulos 0.7.0-SNAPSHOT

- industrial-networks: conduítes industriais e caixas de rede com transporte automático de itens MECHANIUM.
- better-reactor: gerador cristalino fictício de alta potência, conectado à rede de baterias.

- `forestry`: fibras de madeira, resina e estação florestal.
- `textile-industry`: fibras, tecidos e tear mecanizado.
- `biofuel`: biomassa e célula de biocombustível abstrata.
- `construction-materials`: cerâmica, painéis e estruturas pré-fabricadas.

Todos são opcionais e podem ser configurados individualmente.

### Better Reactor — geração abstrata de energia

- O módulo usa um gerador cristalino fictício colocado como blast furnace.
- Clique com um cristal energético MECHANIUM na mão para abastecer o gerador; uma unidade dura cinco minutos.
- Enquanto abastecido, gera 8 unidades de energia por segundo para a rede de baterias conectada por faces/conduítes do mesmo proprietário.
- A energia é uma mecânica abstrata do jogo. Não há combustível nuclear real, parâmetros físicos, projeto técnico ou simulação de reator real.

### Industrial Networks — transporte de itens

- industrial_conduit é instalado como dispenser e network_crate como barril.
- Conduítes adjacentes formam uma rede de até 128 blocos; caixas adjacentes aos conduítes entram na rede se tiverem o mesmo proprietário.
- A cada 2 segundos, a rede tenta mover um item MECHANIUM de uma caixa para outra com espaço.
- Apenas itens com identidade persistente MECHANIUM são transportados; itens vanilla permanecem nos depósitos.
- Somente chunks carregados são examinados. A rede não atravessa distâncias sem conduítes nem conecta depósitos remotamente.
- As caixas são protegidas pelo proprietário e seus conteúdos são devolvidos ao desmontar o bloco. O transporte por funis externos é bloqueado nas caixas e conduítes.


## Expansão 0.7.1.00-SNAPSHOT — quatro novos addons

- `water-treatment`: filtragem básica, células de água purificada, fluido refrigerante e recirculação industrial.
- `greenhouse`: cultivo protegido, painéis, solução nutritiva, bandejas hidropônicas e o novo `greenhouse_controller`. O controlador dá +1 estágio de crescimento a plantas compatíveis após o crescimento normal, no raio configurado, se o proprietário tiver a pesquisa e a rede energética conseguir debitar energia. O controlador antigo `climate_controller` e sua receita são preservados como legado. Ainda não há hidroponia automatizada, colheita automática nem suporte a culturas customizadas; depende de Agriculture e Water Treatment.
- `industrial-recycling`: sucata mista, materiais separados e triagem de resíduos para cadeias circulares.
- `ore-chunks`: fragmentos minerais de ferro, cobre e ouro e lotes compactados; depende de Geology e Ore Processing.

Todos possuem receitas e pesquisas próprias e podem ser ativados ou desativados individualmente em `config.yml`.


## Novos módulos — 0.7.2.00-SNAPSHOT

| ID | Conteúdo | Dependências |
|---|---|---|
| `aquaculture` | Ração aquícola, rede de criação, fertilizante reaproveitado, caixote de pescado e incubadora | Agriculture, Culinary |
| `glassworks` | Carga de sílica, vidro industrial, painéis temperados, lentes de precisão e forno de vidro | Industry |
| `rail-freight` | Dormentes, trilhos reforçados, paletes e manifestos para transporte intermodal | Industry, Industrial Logistics |
| `battery-industry` | Separadores, invólucros, células montadas e bancos de baterias industriais | EcoPower |
| `cold-chain` | Fibras isolantes, células de refrigeração, caixotes térmicos e unidade refrigeradora | Industrial Logistics, Water Treatment, EcoPower |

Os cinco módulos possuem categorias, tecnologias, itens e receitas próprias e podem ser habilitados/desabilitados individualmente em `config.yml`. Aquaculture, Glassworks e Cold Chain também registram definições no runtime genérico de máquinas. Isso ainda não equivale a teste de gameplay nem a máquinas especializadas com múltiplas entradas/saídas.


### Equivalência EMC — 0.7.4.00-SNAPSHOT

O módulo opcional `emc-exchange` adiciona um ledger individual por UUID e dez estações colocáveis (Dissolvedor e Materializador nos níveis I a V).

- O Dissolvedor consome uma pilha de item com valor positivo definido em `emc-exchange.values`, credita o valor unitário multiplicado pela quantidade e registra o item no catálogo aprendido do jogador.
- O Materializador mostra apenas itens aprendidos e compatíveis com seu nível; cada interação sintetiza uma unidade e debita seu valor EMC.
- Limites por item: nível I = 16 EMC; II = 64; III = 256; IV = 1.024; V = 4.096.
- Saldo e itens aprendidos são salvos em `plugins/MECHANIUM/emc-exchange.db`, separado do banco de progressão.
- Estações são dispensadores protegidos por proprietário; apenas o dono ou um administrador pode abrir ou desmontar.
- `/mechanium emc` abre o painel; `/mechanium emc balance` mostra o saldo.

A primeira etapa usa uma lista explícita de valores para evitar conversão de itens não balanceados. Rede de energia, automação por conduítes/funis, PlaceholderAPI e cobertura automática de todos os itens ainda não estão implementadas.

## Slime Customizer — oficina administrativa

O módulo opcional `slime-customizer` disponibiliza `/mechanium admin customizer` e um atalho no HUD administrativo. O editor atual permite:

- Criar itens personalizados usando IDs únicos, material vanilla, categoria própria, pesquisa existente opcional e limite de pilha.
- Listar, entregar, editar nome/material/pesquisa/pilha e remover os itens criados no editor.
- Criar e remover receitas shapeless de até nove ingredientes vanilla ou MECHANIUM, apontando para itens e pesquisas já registrados.
- Persistir cada item e receita com ownership SQLite individual; a remoção de um registro não apaga os outros. Itens referenciados em receitas, máquinas, drops ou recipientes de fluidos não podem ser removidos.

As listas `slime-customizer.user-item-ids` e `slime-customizer.user-recipe-ids` são geridas pelo plugin. Não as edite manualmente. Ainda faltam receitas shaped, edição de receitas existentes e formulários para criar categorias ou tecnologias. O editor não altera nem depende do Geyser/Bedrock.
