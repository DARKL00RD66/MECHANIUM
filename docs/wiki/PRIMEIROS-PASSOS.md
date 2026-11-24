# Primeiros passos — guia do jogador

## 1. Abrir o MECHANIUM

Ao entrar no servidor, o plugin entrega um livro-guia. Segure-o e use clique direito para abrir o menu principal. O comando `/mechanium help` também apresenta a ajuda disponível no plugin.

O menu principal da linha 0.7.3.01+ separa quatro destinos:

| Entrada | Para que serve |
|---|---|
| **Árvore de progressão** | Abre a árvore na era efetiva do jogador e permite consultar e iniciar pesquisas. |
| **Eras históricas** | Mostra os relógios das eras. Selecionar uma era muda apenas a visualização da árvore, não a era real do jogador. |
| **Crafting** | Catálogo de fabricação de itens e máquinas. Exibe requisitos e ingredientes e fabrica quando o jogador atende às condições. |
| **Wiki** | Enciclopédia de consulta de itens, receitas e máquinas. Não fabrica e não consome materiais. |

A **Carteira EMC** também aparece no HUD quando o módulo `emc-exchange` está ativo. Ela abre o painel do saldo EMC do próprio jogador.

## 2. Entender eras históricas

A era efetiva determina o limite histórico de progressão. A configuração padrão contém:

1. Antiguidade
2. Idade Média
3. Renascimento
4. Revolução Industrial
5. Eletrificação
6. Era Moderna
7. Era Atômica e do Pós-Guerra
8. Era da Informação
9. Era Digital
10. Era Espacial
11. Era Mítica
12. Futuro Especulativo

As duas últimas são usadas por módulos opcionais de fantasia e futuro. A administração pode alterar a ordem e os nomes pelo arquivo de configuração.

Na tela **Eras históricas**, os relógios são controles de consulta. A Estrela do Nether marca a era efetiva atual. Clicar em outra era não avança o jogador, não concede pesquisas e não altera a era global do servidor: apenas abre a árvore daquela era para consulta.

## 3. Pontos científicos

Pontos científicos são usados para iniciar pesquisas. Não são dinheiro e não são EMC.

- O valor inicial padrão é **40 pontos científicos** para jogadores elegíveis sem pesquisa concluída e com saldo zero.
- Descobrir pela primeira vez determinados tipos de minério pode conceder pontos. A recompensa é única por jogador e por tipo de minério; minerar repetidamente o mesmo tipo não deve gerar farming.
- A configuração também possui recompensa padrão por avanços/conquistas. Os valores são definidos pelo administrador.
- Pontos são debitados quando a pesquisa começa. Se o jogador cancelar ou não concluir, confira as regras atuais do menu antes de iniciar outra pesquisa.

## 4. Iniciar uma pesquisa

1. Abra **Árvore de progressão**.
2. Selecione a tecnologia que deseja consultar.
3. Verifique o custo científico, os pré-requisitos e a era exigida.
4. Confirme que sua era efetiva alcança a era da tecnologia e que os pré-requisitos estão desbloqueados.
5. Inicie a pesquisa e acompanhe o tempo restante no menu.

As pesquisas do menu do jogador são temporizadas. A duração considera o custo em pontos e a posição da era, respeitando um mínimo configurável de 120 segundos por padrão. A pesquisa continua avançando enquanto o jogador está offline e através de reinicializações; o desbloqueio é aplicado quando o prazo termina e o jogador volta a ser processado pelo plugin.

**Importante:** ter uma pesquisa desbloqueada e estar autorizado a usar a tecnologia são verificações distintas. O serviço central também valida a era efetiva.

## 5. Fabricar itens e máquinas

Use **Crafting** para procurar o resultado desejado.

1. Abra o catálogo e selecione uma categoria.
2. Escolha o item ou a máquina.
3. Confira a descrição, a tecnologia necessária e a lista de ingredientes.
4. Reúna os materiais.
5. Confirme a fabricação.

O servidor valida a progressão e os ingredientes. Os materiais necessários são consumidos e o resultado é entregue no inventário. Itens personalizados são identificados por dados persistentes, não apenas pelo nome mostrado.

Para receitas Bukkit suportadas, as regras também podem bloquear o crafting normal, o livro de receitas, autocrafter, fornalhas, stonecutter e smithing. A cobertura depende dos tipos de receita que o Paper disponibiliza e que o MECHANIUM consegue interceptar com segurança.

## 6. Consultar a Wiki interna

A Wiki do menu é somente para consulta. Abra **Wiki** e escolha:

- **Itens:** descrições e informações dos materiais.
- **Receitas:** resultado, ingredientes e requisitos.
- **Máquinas:** bloco-base, entrada, saída, duração e tecnologia requerida, quando esses dados estão disponíveis.

Use a Wiki para planejar a cadeia de produção; depois retorne a **Crafting** para fabricar. A Wiki não cria itens, não inicia processos e não consome materiais.

## 7. Usar uma máquina

O runtime atual utiliza dispenser como bloco-base para as definições genéricas de máquina. Em linhas gerais:

1. Pesquise a tecnologia da máquina.
2. Fabrique o item da estação pelo Crafting.
3. Coloque a estação no mundo.
4. Interaja com ela para inserir o material de entrada e acompanhar o processo.
5. Retire o produto no espaço de saída quando o processo concluir.

O runtime base suporta **uma entrada e uma saída por ciclo**, sem fila de produção nem múltiplos ingredientes. Alguns módulos usam outros blocos específicos para geradores, baterias, tanques e armazenamentos; consulte o catálogo de módulos antes de construir.

Máquinas com propriedade registrada pertencem ao jogador que as colocou. O dono e administradores podem operá-las ou desmontá-las conforme as regras do sistema. Máquinas que exigem energia precisam estar conectadas a uma bateria/rede válida.

## 8. Energia, manutenção e redes

- Geradores eólicos e solares carregam baterias adjacentes do mesmo proprietário.
- Baterias conectadas pelas faces formam uma rede local para fornecer energia a máquinas próximas.
- O limite padrão de capacidade de uma bateria é 10.000 unidades.
- O consumo por ciclo das máquinas compatíveis é de 1 unidade.
- Máquinas acumulam desgaste por operação. Ao atingir o limite configurado, param até receber manutenção.
- O proprietário pode agachar e clicar na máquina com uma barra de ferro para repará-la. O kit de manutenção é uma alternativa pesquisável.
- Redes de água, armazenamento e conduítes têm regras próprias de adjacência e propriedade; elas não são redes globais.

## 9. Sistema EMC

EMC é uma economia tecnológica pessoal, diferente da economia monetária do servidor.

1. Abra a Carteira EMC ou use `/mechanium emc`.
2. Dissolva itens que tenham valor positivo cadastrado. O sistema credita EMC e aprende aquele tipo de item na primeira dissolução.
3. Use um Materializador compatível para sintetizar uma unidade de um item que você já aprendeu.
4. O saldo é debitado pelo valor unitário configurado.

O saldo e o catálogo aprendido pertencem ao UUID do jogador e não são compartilhados. Os tiers das estações limitam o valor EMC de cada unidade aceita. Leia o [tutorial completo de EMC](../EMC_EXCHANGE.md) para valores, tiers, proteção e configuração.

## 10. Comandos úteis

| Comando | Uso |
|---|---|
| `/mechanium help` | Exibe a ajuda do plugin. |
| `/mechanium calc <id-da-receita> [quantidade]` | Calcula materiais e lotes de uma receita MECHANIUM. |
| `/mechanium emc` | Abre o painel EMC. |
| `/mechanium emc balance` | Mostra seu saldo EMC. |
| `/mechanium environment` | Consulta o contador de impacto industrial do mundo atual. |

## Dúvidas comuns

**Selecionar uma era no relógio muda minha era?** Não. A tela serve para consultar a árvore de outra era; somente a administração pode alterar a era global ou uma sobrescrita individual de teste.

**EMC substitui o dinheiro do servidor?** Não. É um saldo pessoal de equivalência de itens; não possui mercado ou transferência entre jogadores nesta etapa.

**A Wiki fabrica o item?** Não. A Wiki é consulta. A fabricação acontece no menu Crafting ou em mecanismos de crafting suportados.

**Toda máquina aceita qualquer ingrediente?** Não. Cada máquina tem um processo definido. O runtime genérico atual usa uma entrada e uma saída.

**Todas as receitas vanilla estão cobertas?** Não. Existem limitações para receitas/processos especiais que não são expostos ou interceptáveis pela API usada.
