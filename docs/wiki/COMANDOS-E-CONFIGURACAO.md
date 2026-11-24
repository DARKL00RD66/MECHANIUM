# Comandos, permissões e configuração

## Comandos de jogador

| Comando | Função |
|---|---|
| `/mechanium help` | Mostra a ajuda disponível. |
| `/mechanium calc <id-da-receita> [quantidade-final]` | Calcula lotes, produção e materiais intermediários de uma receita MECHANIUM. |
| `/mechanium environment` | Consulta o contador de impacto industrial do mundo atual. No console, a documentação registra consulta agregada dos mundos. |
| `/mechanium emc` | Abre o painel EMC. |
| `/mechanium emc balance` | Mostra o saldo EMC individual do jogador. |

O livro-guia entregue ao entrar também abre o menu principal por clique direito.

## Comandos administrativos

Todos os comandos administrativos exigem a permissão `mechanium.admin`, concedida por padrão a operadores, ou acesso configurado no MECHANIUM.

| Comando | Função |
|---|---|
| `/mechanium admin` | Abre o painel administrativo. |
| `/mechanium admin hud` | Abre diretamente o HUD/painel administrativo. |
| `/mechanium admin era <era>` | Altera a era global do servidor. |
| `/mechanium admin era <era> <jogador>` | Define uma era individual para teste. |
| `/mechanium admin recipes` | Abre o catálogo de receitas vanilla e seus requisitos históricos. |
| `/mechanium admin recipe <recipe_id> <era|off>` | Define a era requerida para uma receita vanilla ou libera-a com `off`. |
| `/mechanium admin custom-recipes` | Abre o editor de receitas próprias do MECHANIUM. |

Use IDs de era, não nomes traduzidos. Exemplos: `ancient`, `medieval`, `renaissance`, `industrial_revolution`, `electrification`, `modern`, `atomic_age`, `information_age`, `digital_age`, `space_age`, `mythic_age` e `speculative_future`.

## Permissões

| Permissão | Padrão | Descrição |
|---|---|---|
| `mechanium.admin` | Operadores | Acesso ao painel e funções administrativas, incluindo bypasses de propriedade quando previstos pelo sistema. |

O arquivo também contém `staff.admins` para uma lista adicional de administradores. Consulte a configuração gerada pela versão instalada antes de alterar o formato dessa lista.

## Arquivos e bancos de dados

Após o primeiro início, os arquivos ficam em `plugins/MECHANIUM/`:

| Arquivo | Uso |
|---|---|
| `config.yml` | Eras, pontos, recompensas, módulos, receitas vanilla, desgaste, addons e valores EMC. |
| `mechanium.db` | Banco SQLite principal: progressão, conteúdo e dados persistentes associados ao core. |
| `emc-exchange.db` | Ledger separado de saldo EMC e itens aprendidos, quando o módulo EMC é usado. |
| `addons/` | Diretório dos JARs externos de addon. |

Faça backup dos arquivos de banco antes de atualizar o plugin ou editar dados manualmente. Não apague bancos durante uma atualização.

## Configurações principais

Os exemplos abaixo representam os valores padrão documentados no repositório. Preserve as demais seções ao editar o arquivo.

### Progressão

```yaml
progression:
  default-era: ancient
  server-era: ancient
  starting-research-points: 40
  research:
    minimum-seconds: 120
    base-seconds: 120
    seconds-per-research-point: 3
    seconds-per-era: 15
    overrides: {}
```

- `default-era`: era inicial usada antes de existir uma era global persistida.
- `server-era`: valor inicial da era global.
- `starting-research-points`: pontos iniciais padrão.
- `minimum-seconds`: duração mínima de uma pesquisa.
- `base-seconds`, `seconds-per-research-point` e `seconds-per-era`: componentes do cálculo de duração.
- `overrides`: valores de duração específicos por tecnologia.

A duração padrão é calculada a partir do tempo-base, do custo científico e da ordem da era, respeitando o mínimo. Pesquisas ativas são persistidas com prazo absoluto e continuam avançando offline.

### Eras

`progression.eras` contém IDs, nomes visíveis e ordem. Não reutilize IDs já persistidos sem planejar uma migração. Módulos referenciam IDs estáveis, não nomes traduzidos.

### Recompensas científicas

`research-rewards.blocks` define pontos concedidos na primeira descoberta de cada tipo de minério por jogador. O padrão inclui carvão, cobre, ferro, ouro, redstone, lápis-lazúli, diamante, esmeralda, quartzo do Nether, ouro do Nether e detritos ancestrais. Um valor 0 ou a remoção da entrada desativa a recompensa daquele tipo.

`research-rewards.advancements.default` define a recompensa padrão de avanço/conquista indicada na configuração.

### Bloqueio de receitas vanilla

```yaml
vanilla-recipe-gates:
  enabled: true
  auto-register: true
  recipes:
    'minecraft:stone_pickaxe': ancient
    'minecraft:iron_pickaxe': medieval
    'minecraft:piston': industrial_revolution
    'minecraft:enchanting_table': mythic_age
```

- `enabled`: liga/desliga o sistema de bloqueio.
- `auto-register`: permite sugerir e registrar regras ausentes para receitas suportadas.
- `recipes`: mapa de ID de receita para ID de era; `off` libera uma receita.

A inicialização preserva regras existentes. A administração também pode alterar receitas pelo painel ou comando.

### Módulos

Cada chave em `modules` controla o módulo de mesmo ID. A configuração padrão habilita os módulos históricos/industriais listados no [catálogo](CATALOGO-DE-MODULOS.md); `optional-fantasy` e `optional-futuristic` começam desativados.

Desative módulos somente considerando as dependências. Um módulo dependente pode deixar de iniciar se a dependência necessária estiver desligada.

### Máquinas

```yaml
machines:
  wear:
    enabled: true
    max-cycles: 100
    cycles-per-operation: 1
  hardcore:
    enabled: false
    wear-multiplier: 2
    repair-iron-ingots: 2
```

O desgaste padrão é habilitado, com limite de 100 ciclos e um ciclo por operação. O modo hardcore é opcional e começa desabilitado. Quando ativado, aumenta o desgaste e o custo de reparação.

### Addons

```yaml
addons:
  directory: addons
  api-version: '1'
```

O diretório relativo fica dentro de `plugins/MECHANIUM/`. O carregador verifica descriptor, versão de API, dependências e ciclos entre addons.

### Lista adicional de staff

```yaml
staff:
  admins: []
```

A permissão `mechanium.admin` continua sendo o caminho padrão. Use a lista de admins adicionais somente no formato esperado pela versão instalada.

### EMC

```yaml
emc-exchange:
  values:
    'minecraft:stone': 1
    'minecraft:iron_ingot': 32
    'minecraft:diamond': 512
    'mechanium:mechanical_parts': 48
```

Cada valor é EMC por unidade. Apenas itens listados com valor positivo podem ser dissolvidos. Valores ausentes, zero ou negativos não são aceitos. Os valores de exemplo devem ser balanceados para a economia do servidor.

Consulte [EMC Exchange](../EMC_EXCHANGE.md) para todos os detalhes.

## Editor administrativo de receitas MECHANIUM

O comando `/mechanium admin custom-recipes` abre receitas já registradas por módulos/addons. É possível editar:

- ID do item de resultado e quantidade;
- tecnologia exigida;
- formato `SHAPED` ou `SHAPELESS`;
- ingredientes e padrão 3×3;
- salvar ou descartar a edição.

Alguns campos são informados no chat após clicar no botão correspondente. Exemplos de sintaxe documentada:

```text
Resultado: mechanical_parts
Ingredientes: I=IRON_INGOT,P=PISTON
Padrão: III/IPI/III
```

Use `.` para uma célula vazia do padrão; separe as três linhas com `/`. Ingredientes MECHANIUM usam ID como `mechanium:copper_coil`. Este editor não cria novos itens, tecnologias ou IDs de receita.

Para detalhes do painel e da lógica de receitas vanilla, consulte [Administração detalhada](../ADMINISTRACAO.md).
