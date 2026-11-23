# Módulos oficiais do MECHANIUM

## Módulos históricos e industriais padrão

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
| `eco-power` | Geradores eólicos/solares, baterias com capacidade e consumo real de energia por máquinas. |
| `sfcalc` | `/mechanium calc <receita> [quantidade]` calcula lotes, produção e materiais recursivos. |
| `liquid` | Filtro de água, tanques e tubulações conectadas que transferem água purificada. |
| `advanced-tech` | Montadora, britador e refinaria industrial com receitas e processamento. |
| `simple-storage` | Nós de armazenamento conectados por faces, com inventário virtual paginado e proteção por proprietário/permissões. |
| `sensible-toolbox` | Kit de campo com efeitos temporários e kit de manutenção que repara equipamentos. |\n| `exotic-garden` | Sementes diversificadas, ervas desidratadas, extrato botânico, conservas tropicais e caixote de pomar. |\n| `dank-tech2` | Upgrade fabricável para um armazém portátil de 54 espaços; o item mantém os conteúdos no PDC. |

## Módulos adicionados em 0.5.0-SNAPSHOT

| ID | Funcionalidade jogável |
|---|---|
| `electric-spawners` | Incubadoras elétricas de galinha, vaca, porco e ovelha; exigem insumos, pesquisa e bateria adjacente; produzem spawn eggs vanilla de animais passivos. |
| `fast-machines` | Prensa rápida (20 ticks) e enrolador de bobinas (40 ticks), ambos acelerados e alimentados pela rede de energia. |
| `hardcore-slimefun` | Pesquisas de confiabilidade e manutenção, kit reparador desbloqueável e regras hardcore opcionais para desgaste e custo de reparo. |

### ElectricSpawners — incubação pecuária

As quatro incubadoras usam o runtime comum de dispenser: sementes geram ovos de galinha; feno gera ovos de vaca; cenouras geram ovos de porco; trigo gera ovos de ovelha. A produção exige `livestock_incubation` e uma bateria adjacente do mesmo proprietário. O escopo é limitado a animais passivos e não inclui spawners de criaturas hostis.

### FastMachines — produção acelerada

A prensa rápida consome uma barra de ferro e produz duas peças mecânicas em 20 ticks; o enrolador consome cobre e produz duas bobinas em 40 ticks. Ambos exigem energia, propriedade e a pesquisa `fast_machinery`. Ainda não há filas ou paralelismo.

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

- O gerador eólico é colocado como bloco de fornalha e produz 1 unidade de energia por segundo.
- O gerador solar é colocado como defumador e produz 2 unidades por segundo durante o dia, sem tempestade.
- Uma bateria é colocada como barril e armazena até 10.000 unidades.
- Geradores carregam uma bateria adjacente do mesmo proprietário. Baterias adjacentes entre si formam uma rede local; máquinas conectadas a uma bateria dessa rede podem consumir energia.
- O filtro de água, a montadora, o britador, a refinaria, o moinho, a conservadora, as incubadoras e as máquinas rápidas exigem energia: cada ciclo concluído consome 1 unidade.
- Máquinas acumulam desgaste por ciclo até 100; ao atingir o limite, param. O proprietário pode agachar e clicar com uma barra de ferro para reparar e zerar o desgaste.
- A rede atual é local, limitada a 64 baterias na busca por consumo e não carrega chunks automaticamente. Quebrar a bateria recupera o item, mas não preserva a carga restante.

### Liquid — distribuição de água

- O filtro transforma um balde de água em um item MECHANIUM `purified_water`.
- O tanque é um barril e a tubulação é um dispenser identificado pelo MECHANIUM.
- Tanques do mesmo proprietário conectados por tubulações adjacentes pelas faces formam uma rede. A cada 2 segundos, um item de água purificada é transferido de um tanque para outro tanque que tenha espaço.
- Cada rede examina até 128 tubulações e apenas chunks carregados. Tubulações e tanques não transferem itens vanilla por funis; a rede transporta apenas o item `purified_water`.
- O acesso aos tanques respeita o proprietário; a tubulação é um bloco de conexão, não um inventário acessível.

### SFCalc — cálculo de produção

`/mechanium calc <id-da-receita> [quantidade-final]` calcula a quantidade de lotes necessária, produção final e materiais. Receitas intermediárias MECHANIUM são expandidas recursivamente até oito níveis; receitas shaped contam as ocorrências de cada símbolo e receitas shapeless contam uma unidade de cada ingrediente registrado. Ciclos ou intermediários sem receita são exibidos como itens MECHANIUM. O comando calcula materiais, não preços monetários, e não substitui o bloqueio de progressão no crafting.

### AdvancedTech — indústria avançada

O britador converte minério britado em concentrado de ferro; a refinaria converte concentrado em liga refinada; a montadora converte peças mecânicas em peças de precisão. As três máquinas usam o runtime atual de dispenser, têm entrada/saída, progresso e proprietário persistentes e exigem energia. O runtime ainda é de entrada única e saída única; não há filas de produção ou vários insumos por máquina.

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
- Máquinas continuam limitadas ao dispenser com um insumo e um produto por ciclo.
- Energia e fluidos são redes locais simples, não um simulador genérico de cabos ou líquidos.
- Armazenamento conectado exige nós face a face; não é uma rede logística de longa distância.
- Não há integração com economia para calcular preços monetários.
- Não há filas de produção, processamento paralelo ou múltiplas receitas simultâneas; manutenção de máquinas já existe via ferro e kit pesquisável.
- O conteúdo relacionado a armas, ataques, mísseis ou interferência não faz parte do escopo. Sistemas civis e industriais permanecem no escopo.
