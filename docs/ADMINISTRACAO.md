# Painel administrativo e progressão histórica

## Abrir o painel

Jogadores com a permissão `mechanium.admin` (por padrão, operadores) ou listados em `staff.admins` podem usar:

```
/mechanium admin
/mechanium admin hud
```

O painel é uma interface de inventário com animação de revelação (moldura primeiro e botões surgindo do centro para fora) e contém:

- **Trocar era GLOBAL:** altera a era histórica do servidor. Jogadores online recebem o título **Nova era** com o nome da era como subtítulo e o som de conclusão de desafio. Jogadores offline recebem o mesmo aviso uma vez ao voltar, desde que ainda não tenham recebido a revisão mais recente.
- **Eras individuais (testes):** mantém a seleção de era por jogador para testes isolados. Também permite testar todos os conectados de uma vez sem alterar a era global. O botão **Herdar era global** remove a sobrescrita individual e faz o jogador voltar a acompanhar a era do servidor.
- **Era inicial de configuração:** fallback utilizado apenas quando ainda não existe era global persistida.
- **Pontos científicos:** concede 50 pontos ao jogador conectado selecionado.
- **Receitas vanilla por era:** configura requisitos históricos para um conjunto inicial de receitas do Minecraft e permite ativar/desativar o sistema globalmente.

A era global é persistida no SQLite e vale para jogadores online e offline que não possuam uma sobrescrita individual de teste. Alterar a era global não apaga as sobrescritas de teste. O comando /mechanium admin era <era> também altera a era global; para testes individuais use /mechanium admin era <era> <jogador> ou a tela de **Eras individuais (testes)**.

## Receitas vanilla

A tela de receitas permite clicar em cada receita para avançar a era exigida na ordem cadastrada. Após a última era, a regra passa para `off` (sem bloqueio). O estado é salvo no `config.yml`.

Também é possível configurar qualquer ID de receita do namespace Minecraft pelo comando:

```
/mechanium admin recipe minecraft:iron_pickaxe medieval
/mechanium admin recipe minecraft:iron_pickaxe off
```

Para abrir diretamente a tela de receitas:

```
/mechanium admin recipes
```

O bloqueio atual intercepta a fabricação em bancada (`CraftItemEvent`). Ele não altera as receitas originais e não bloqueia automaticamente processos de fornalha, bigorna, ferraria, corte de pedra ou outros tipos de estação. Cada tipo adicional precisa de tratamento próprio.

O sistema pode ser desativado por completo no painel ou em:

```yaml
vanilla-recipe-gates:
  enabled: true
  recipes:
    'minecraft:iron_pickaxe': medieval
```

Ausência de regra ou valor `off` deixa a receita liberada.

## Editor de receitas próprias do MECHANIUM

Abra pelo painel administrativo em **Receitas MECHANIUM** ou diretamente:

```
/mechanium admin custom-recipes
```

O editor lista as receitas registradas pelos módulos e addons. Ao selecionar uma receita, é possível:

- Alterar o item de resultado escolhendo outro ID de item registrado no MECHANIUM.
- Aumentar ou diminuir a quantidade produzida, respeitando o limite de pilha do item.
- Escolher a tecnologia exigida; a era necessária vem da tecnologia selecionada. Também é possível remover o requisito específico da receita.
- Alternar entre `SHAPED` e `SHAPELESS`.
- Editar o mapa de ingredientes e, para receitas `SHAPED`, o padrão 3×3.
- Salvar ou descartar as alterações antes de sair.

Alguns campos são digitados no chat após clicar no botão correspondente:

```
Resultado: mechanical_parts
Ingredientes: I=IRON_INGOT,P=PISTON
Padrão: III/IPI/III
```

Para ingredientes MECHANIUM, use `mechanium:item_id`, por exemplo `I=mechanium:copper_coil`. Para deixar uma célula vazia no padrão, use `.`. Separe as linhas do padrão com `/`. Os materiais vanilla precisam ser itens válidos; o resultado precisa ser um item registrado pelo MECHANIUM.

As alterações ficam em `admin-recipe-overrides` no `config.yml`, separadas das definições canônicas dos módulos no SQLite. O plugin aplica esses overrides depois de carregar os módulos e addons, e atualiza a receita Bukkit durante a sessão. Assim, uma atualização do módulo não sobrescreve a edição administrativa.

Este editor modifica receitas já registradas; não cria itens ou tecnologias novos, e não adiciona IDs de receita que não pertençam a um módulo ou addon carregado.

## Eras e tecnologias

A progressão padrão inclui Antiguidade, Idade Média, Renascimento, Revolução Industrial, Eletrificação, Era Moderna, Era Atômica e do Pós-Guerra, Era da Informação, Era Digital e Era Espacial. As eras Mítica e Futuro Especulativo permanecem disponíveis para os módulos opcionais de fantasia e futuro.

Cada tecnologia tem custo e pré-requisitos. A era do jogador precisa alcançar a era exigida pela tecnologia antes de iniciar a pesquisa.

## Pontos científicos iniciais

`progression.starting-research-points` define o crédito inicial (padrão: 40). O plugin concede esse valor a jogadores sem pesquisas concluídas e com saldo zero. A verificação também alcança contas antigas que nunca conseguiram iniciar a árvore; após uma pesquisa bem-sucedida, a concessão não se repete.

## Como ganhar pontos científicos

Além dos 40 pontos iniciais, jogadores recebem pontos ao descobrir minérios pela primeira vez. Cada material configurado rende pontos uma única vez por jogador; o marcador fica salvo nos dados persistentes do jogador, para impedir farming quebrando repetidamente o mesmo tipo de minério.

Os valores podem ser alterados em `research-rewards.blocks` no `config.yml`. Exemplo:

```yaml
research-rewards:
  blocks:
    IRON_ORE: 2
    DIAMOND_ORE: 5
    ANCIENT_DEBRIS: 8
```

A lista padrão também inclui as variantes de ardósia profunda, ouro, cobre, carvão, lápis-lazúli, redstone, esmeralda e minérios do Nether. Use valor `0` ou remova uma entrada para não recompensar aquele material.

## Configurações antigas

Na inicialização, o MECHANIUM mescla as eras padrão que estiverem ausentes, preservando as eras personalizadas e os demais valores já configurados. Faça backup do arquivo antes de atualizar.
