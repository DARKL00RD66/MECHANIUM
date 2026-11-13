# Módulos oficiais do MECHANIUM

## Módulos históricos e industriais padrão

| ID | Módulo | Padrão |
|---|---|---|
| `industry` | Máquinas e processos industriais | Ativado |
| `geology` | Prospecção e conhecimento mineral | Ativado |
| `agriculture` | Agricultura e produção alimentar | Ativado |
| `energy` | Progressão energética | Ativado |
| `storage` | Armazéns e logística de estoque | Ativado |
| `environment` | Gestão ambiental e resíduos | Ativado |

### Geologia

O módulo `geology` depende de `industry` e adiciona a pesquisa `geological_surveying`. Depois de desbloqueá-la, o jogador pode fabricar o `geological_survey_kit` e usá-lo em um bloco para contar minérios em um volume de 9×9×9 ao redor do ponto selecionado. Chunks descarregados são ignorados, evitando geração de terreno durante a varredura.

A ferramenta atual é uma prospecção local. Depósitos regionais, veios minerais, amostras e integração profunda com mineração ainda estão pendentes.

## Sete módulos adicionais — 0.3.0-SNAPSHOT

| ID | Funcionalidade jogável |
|---|---|
| `extra-gear` | Picareta reforçada concede breve Haste após mineração; bússola de levantamento informa coordenadas, biome, direção e bloco observado. |
| `eco-power` | Geradores eólicos/solares, baterias com capacidade e consumo real de energia por máquinas. |
| `sfcalc` | `/mechanium calc <receita> [quantidade]` calcula lotes, produção e materiais recursivos. |
| `liquid` | Filtro de água, tanques e tubulações conectadas que transferem água purificada. |
| `advanced-tech` | Montadora, britador e refinaria industrial com receitas e processamento. |
| `simple-storage` | Nós de armazenamento conectados por faces, com inventário virtual paginado e proteção por proprietário/permissões. |
| `sensible-toolbox` | Kit de campo com efeitos temporários e kit de manutenção que repara equipamentos. |

### EcoPower — geração e rede de energia

- O gerador eólico é colocado como bloco de fornalha e produz 1 unidade de energia por segundo.
- O gerador solar é colocado como defumador e produz 2 unidades por segundo durante o dia, sem tempestade.
- Uma bateria é colocada como barril e armazena até 10.000 unidades.
- Geradores carregam uma bateria adjacente do mesmo proprietário. Baterias adjacentes entre si formam uma rede local; máquinas conectadas a uma bateria dessa rede podem consumir energia.
- O filtro de água, a montadora, o britador e a refinaria exigem energia: cada ciclo concluído consome 1 unidade.
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

## Limites conhecidos antes do teste de servidor

- O build automatizado pode verificar compilação e testes unitários, mas não confirma comportamento real em Paper/Purpur 1.21.11.
- Máquinas continuam limitadas ao dispenser com um insumo e um produto por ciclo.
- Energia e fluidos são redes locais simples, não um simulador genérico de cabos ou líquidos.
- Armazenamento conectado exige nós face a face; não é uma rede logística de longa distância.
- Não há integração com economia para calcular preços monetários.
- Não há filas de produção, manutenção de máquinas ou múltiplas receitas simultâneas.
- O conteúdo relacionado a armas, ataques, mísseis ou interferência não faz parte do escopo. Sistemas civis e industriais permanecem no escopo.
