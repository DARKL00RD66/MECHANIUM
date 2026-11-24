# Equivalência EMC do MECHANIUM

## Visão geral

O módulo opcional `emc-exchange` cria um saldo EMC individual e persistente para cada jogador. O sistema permite dissolver itens configurados em EMC, aprender esses itens e sintetizar uma unidade posteriormente pagando seu valor.

## Acesso

- `/mechanium emc`: abre o painel do ledger.
- `/mechanium emc balance`: consulta o saldo atual por chat.
- Estações: coloque o item de estação e clique com o botão direito no dispensador instalado.

## Fluxo de jogo

1. Pesquise os fundamentos da equivalência e as tecnologias de dissolução/síntese.
2. Fabrique um Dissolvedor e coloque-o.
3. Insira uma pilha no espaço central do menu e escolha **Dissolver pilha**.
4. A pilha é consumida, o EMC correspondente é creditado e o item fica aprendido na primeira dissolução.
5. Fabrique um Materializador e coloque-o.
6. Escolha um item aprendido no menu. A síntese cria uma unidade e debita o valor EMC dessa unidade.

O saldo e a lista aprendida são por UUID; não são compartilhados entre jogadores.

## Tiers

| Nível | Valor máximo de cada item aceito |
|---|---:|
| I | 16 EMC |
| II | 64 EMC |
| III | 256 EMC |
| IV | 1.024 EMC |
| V | 4.096 EMC |

A restrição é aplicada ao valor unitário. Uma pilha de vários itens pode gerar um total maior, desde que cada unidade esteja dentro do limite do nível.

## Valores configuráveis

Edite `plugins/MECHANIUM/config.yml`:

```yaml
emc-exchange:
  values:
    'minecraft:stone': 1
    'minecraft:iron_ingot': 32
    'minecraft:diamond': 512
    'mechanium:mechanical_parts': 48
```

Cada entrada é o valor de uma unidade. Use IDs `minecraft:<material>` para itens vanilla e `mechanium:<id>` para itens MECHANIUM. Valores ausentes, zero ou negativos não são aceitos para dissolução. Os valores padrão são exemplos iniciais de balanceamento e devem ser ajustados conforme a economia do servidor.

## Persistência e proteção

O ledger usa `plugins/MECHANIUM/emc-exchange.db`, independente do banco de progressão. O PDC do dispensador armazena o tipo da estação e o UUID do proprietário. Apenas o proprietário ou alguém com `mechanium.admin` pode abrir ou desmontar a estação. Estações protegidas não são destruídas por explosões ou pistões.

## Limitações desta primeira etapa

- Só itens listados em `emc-exchange.values` podem ser dissolvidos; não há cálculo automático por receita.
- A síntese só oferece itens aprendidos pelo próprio jogador.
- A estação usa interação manual; não há alimentação automática por funis ou conduítes.
- As estações ainda não consomem energia da rede EcoPower.
- PlaceholderAPI, placeholders externos e sincronização com outros plugins não estão incluídos.
- Os limites de tier são de valor EMC por unidade, não uma capacidade de energia armazenada pela máquina.

O módulo é uma implementação independente inspirada no comportamento geral descrito para sistemas de equivalência. Não inclui código ou assets do EMCTech original.
