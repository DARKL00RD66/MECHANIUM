# Painel administrativo e progressão histórica

## Abrir o painel

Jogadores com a permissão `mechanium.admin` (por padrão, operadores) ou listados em `staff.admins` podem usar:

```
/mechanium admin
/mechanium admin hud
```

O painel é uma interface de inventário e contém:

- **Alterar era de jogadores:** seleciona um jogador conectado ou todos os jogadores conectados.
- **Era inicial do servidor:** define a era aplicada a novos jogadores; não altera jogadores existentes.
- **Pontos científicos:** concede 50 pontos ao jogador conectado selecionado.
- **Receitas vanilla por era:** configura requisitos históricos para um conjunto inicial de receitas do Minecraft e permite ativar/desativar o sistema globalmente.

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

## Eras e tecnologias

A progressão padrão inclui Antiguidade, Idade Média, Renascimento, Revolução Industrial, Eletrificação, Era Moderna, Era Atômica e do Pós-Guerra, Era da Informação, Era Digital e Era Espacial. As eras Mítica e Futuro Especulativo permanecem disponíveis para os módulos opcionais de fantasia e futuro.

Cada tecnologia tem custo e pré-requisitos. A era do jogador precisa alcançar a era exigida pela tecnologia antes de iniciar a pesquisa.

## Pontos científicos iniciais

`progression.starting-research-points` define o crédito inicial (padrão: 40). O plugin concede esse valor a jogadores sem pesquisas concluídas e com saldo zero. A verificação também alcança contas antigas que nunca conseguiram iniciar a árvore; após uma pesquisa bem-sucedida, a concessão não se repete.

## Configurações antigas

Na inicialização, o MECHANIUM mescla as eras padrão que estiverem ausentes, preservando as eras personalizadas e os demais valores já configurados. Faça backup do arquivo antes de atualizar.
