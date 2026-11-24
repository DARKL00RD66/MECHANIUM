# Distribuição histórica de itens e receitas

Esta página registra a auditoria de eras da linha **0.7.4.02-SNAPSHOT**. A classificação tem dois caminhos diferentes: conteúdo próprio do MECHANIUM e receitas vanilla.

## Diagnóstico

A concentração excessiva na Antiguidade vinha principalmente do classificador automático de receitas vanilla. Quando uma receita não correspondia a uma regra explícita, o método antigo retornava `ancient`. Isso fazia itens desconhecidos — inclusive objetos de tecnologia posterior — receberem a primeira era apenas por não terem sido reconhecidos.

A revisão dos módulos próprios mostrou uma distribuição diferente: as três tecnologias que estão explicitamente na Antiguidade no núcleo industrial são **Metalurgia do bronze**, **Sistemas de irrigação** e **Técnicas de alvenaria**. A tecnologia **Mecânica básica** está na Renascença; havia um comentário antigo e incorreto dizendo “Ancient”. A prensa mecânica também estava liberada por essa tecnologia, apesar de sua receita exigir um pistão, componente colocado na Revolução Industrial. Corrigi a prensa, sua receita e seu processo para exigir **Energia a vapor**, da Revolução Industrial.

Portanto, não era adequado simplesmente empurrar todas as tecnologias antigas para eras posteriores. O problema estava no fallback de receitas vanilla e em algumas associações pontuais.

## Princípios da nova classificação

A era deve refletir a técnica representada pelo resultado, não apenas a raridade do item ou o material visual do bloco. Um item vanilla não recebe uma era mais antiga só porque o classificador não o reconheceu.

| Era | Critério | Exemplos de referência |
|---|---|---|
| **Antiguidade** | Materiais básicos, ferramentas manuais primitivas, agricultura simples, cobre inicial e construção elementar. | Tábuas, pedra, ferramentas de madeira/pedra, cobre básico, vidro e tijolos simples. |
| **Idade Média** | Metalurgia ferrosa consolidada, ferraria e instrumentos de oficina. | Lingotes de ferro/ouro, bigorna, balde, caldeirão, lanternas e ferramentas de oficina. |
| **Renascimento** | Navegação, cartografia, óptica, precisão e conhecimento técnico sistematizado. | Bússola, mapa, luneta, quartzo trabalhado e ferramentas de diamante. |
| **Revolução Industrial** | Máquinas, produção mecanizada, mineração industrial e transporte ferroviário. | Pistão, funil, ejetor, carrinhos, trilhos, alto-forno e defumador. |
| **Eletrificação** | Circuitos elétricos, sinalização, iluminação e controle elétrico. | Lâmpada de redstone, repetidor, comparador, para-raios e lâmpada de cobre. |
| **Era Moderna** | Materiais processados e componentes de construção/produção mais avançados. | Bloco de ametista, calcita, cobre cortado, grades e estruturas de cobre trabalhado. |
| **Era Atômica e do Pós-Guerra** | Energia civil avançada, instrumentação e tecnologias especializadas de pós-guerra. | A maior parte do conteúdo está nos módulos tecnológicos próprios, não em receitas vanilla comuns. |
| **Era da Informação** | Sensores, coleta e tratamento de informação. | Sensor sculk e tecnologias de informação dos módulos MECHANIUM. |
| **Era Digital** | Lógica programável e automação baseada em processamento digital. | Observador, autocrafter e controle automatizado. |
| **Era Espacial** | Exploração e equipamentos de progressão do End usados como representação de tecnologia de fim de jogo. | Elytra e caixas de shulker. É uma convenção de progressão de Minecraft, não uma afirmação histórica literal. |
| **Era Mítica** | Magia, alquimia e artefatos de dimensões fantásticas. | Mesa de encantamentos, suporte de poções, beacon, baú do End e conduíte. |
| **Futuro Especulativo** | Materiais finais de progressão que não pertencem à história tecnológica real. | Netherite e seus componentes. |

## Regras técnicas

A ordem de decisão do classificador agora é:

1. Conteúdo mítico e artefatos fantásticos.
2. Materiais de fim de jogo: Netherite no Futuro Especulativo; Elytra e Shulker na Era Espacial.
3. Automação digital e sensores informacionais.
4. Infraestrutura elétrica.
5. Maquinário e transporte industrial.
6. Materiais processados da Era Moderna.
7. Navegação, óptica e ferramentas de precisão renascentistas.
8. Metalurgia ferrosa medieval.
9. Materiais e ferramentas primitivas da Antiguidade.
10. **Fallback: Renascimento**, nunca Antiguidade.

O fallback renascentista é deliberadamente neutro: significa “classificação automática provisória”, não que qualquer item desconhecido tenha sido comprovadamente inventado no Renascimento. A administração deve revisar esses casos no catálogo de receitas.

## Conteúdo próprio do MECHANIUM

Itens e máquinas dos módulos internos não usam o fallback de receitas vanilla. Cada `ItemDefinition` referencia uma tecnologia; a tecnologia define a era e seus pré-requisitos. Assim, o item herda o nível de progressão da pesquisa associada.

Alguns exemplos de cadeias coerentes:
- **Bronze e alvenaria:** Antiguidade.
- **Rotação de culturas, moinhos e ferramentas de oficina:** Idade Média.
- **Mecânica básica, cartografia e prospecção:** Renascimento.
- **Vapor, produção mecanizada, trilhos, britagem e química industrial:** Revolução Industrial.
- **Motores, rede elétrica e incubação elétrica:** Eletrificação.
- **Eficiência, materiais sintéticos, estufas controladas e refrigeração:** Era Moderna.
- **Controle automatizado, hidroponia avançada e redes de computadores:** Era da Informação/Digital, conforme a tecnologia.

A prensa mecânica foi alinhada à Revolução Industrial: item, receita e processo agora exigem `steam_power`. Peças mecânicas simples continuam ligadas a `basic_mechanics`, na Renascença.

## Migração das configurações existentes

Na primeira inicialização desta versão, o MECHANIUM compara o valor salvo de cada receita com a sugestão automática da versão anterior. Quando o valor salvo ainda coincide com aquela sugestão antiga e a nova classificação é diferente, ele atualiza a regra.

- Regras definidas como `off` são preservadas.
- Valores que já diferem da sugestão anterior são preservados.
- O antigo padrão `minecraft:observer = modern` é corrigido para `digital_age`.
- A migração executa uma única vez e grava um marcador em `vanilla-recipe-gates.migrations`.

**Limitação importante:** versões antigas não registravam se um valor igual à sugestão automática havia sido escolhido manualmente. Por isso, uma escolha manual que coincidia exatamente com o antigo default também pode ser atualizada. O plugin registra essa ressalva no console; revise o catálogo e reaplique qualquer escolha deliberada com:

```text
/mechanium admin recipes
/mechanium admin recipe minecraft:<id> <era>
/mechanium admin recipe minecraft:<id> off
```

Faça backup de `plugins/MECHANIUM/config.yml` antes da atualização.

## Como continuar a auditoria

A descoberta de receitas acontece no servidor Paper, usando o registro real de receitas da versão instalada. A quantidade exata por era pode variar conforme as receitas efetivamente registradas. Para auditar uma instalação:

1. Inicie o Paper/Purpur com MECHANIUM atualizado.
2. Abra `/mechanium admin recipes`.
3. Confira as páginas e as eras sugeridas.
4. Ajuste os casos contextuais com o comando administrativo.
5. Registre novas exceções no `vanilla-recipe-gates.recipes` do `config.yml`.

Receitas de fogueira, poções e algumas operações especiais não entram no catálogo porque o Paper não oferece o mesmo caminho confiável de interceptação para elas. Consulte [Administração](../ADMINISTRACAO.md) e [Sistemas](SISTEMAS.md).
