# Sistemas e mecânicas do MECHANIUM

Este documento explica os sistemas que compõem a linha 0.9.9.00-SNAPSHOT. A lista de módulos e o estado de cada domínio também estão no [catálogo de módulos](CATALOGO-DE-MODULOS.md).

## 1. Motor de progressão

O motor central mantém a era efetiva, os pontos científicos, as tecnologias pesquisadas e as permissões tecnológicas. Módulos oficiais e addons devem consultar esse serviço, em vez de criarem progressões paralelas.

A era global fica persistida no SQLite. Um jogador normalmente herda a era global; uma sobrescrita individual pode ser usada pela administração para testes. Pesquisas são individuais e associadas ao UUID do jogador.

### Pesquisa e autorização

- Uma tecnologia pode definir era exigida, custo científico e pré-requisitos.
- O jogador só pode iniciar uma pesquisa quando a era efetiva e os pré-requisitos permitem.
- O menu do jogador usa pesquisas temporizadas.
- A API distingue `hasUnlockedTechnology` (pesquisa concluída) de `canUseTechnology` (pesquisa e era permitem o uso).

### Tempo de pesquisa

A configuração permite alterar o mínimo, o tempo-base, o acréscimo por ponto científico, o acréscimo por era e sobrescritas por tecnologia. O padrão documentado é 120 segundos mínimos, 120 de base, 3 segundos por ponto e 15 segundos por posição da era. O prazo absoluto continua avançando offline e durante reinicializações.

## 2. Livro-guia, HUD e menus

O livro-guia é entregue automaticamente ao jogador e abre o menu principal com clique direito. A interface principal separa:

- Árvore de progressão;
- Eras históricas;
- Crafting;
- Wiki.

A tela de eras usa relógios selecionáveis e uma Estrela do Nether acima da era efetiva atual. A seleção troca apenas o contexto de consulta da árvore.

A Carteira EMC aparece no HUD quando `emc-exchange` está habilitado. O painel mostra o saldo individual e a quantidade de itens aprendidos.

## 3. Crafting e receitas

O catálogo de fabricação reúne itens e máquinas MECHANIUM por categoria. Cada entrada apresenta os dados disponíveis de resultado, ingredientes, tecnologia e requisitos. A fabricação valida as condições no servidor, consome os materiais e entrega o resultado.

Receitas MECHANIUM podem ser shaped ou shapeless e podem incluir ingredientes vanilla ou itens MECHANIUM identificados por ID persistente. O editor administrativo altera receitas já registradas; não cria novas tecnologias nem itens.

### Bloqueio de receitas vanilla

O catálogo de receitas vanilla é descoberto a partir das receitas que o Paper expõe. Regras de era podem ser sugeridas automaticamente e as regras existentes são preservadas. O bloqueio é aplicado a crafting, livro de receitas, autocrafter, fornalhas, stonecutter e smithing quando o evento/API oferece uma verificação confiável.

Limitações: receitas de fogueira são omitidas por não haver cancelamento confiável associado à chave da receita; poções e algumas operações especiais não utilizam a mesma registry; itens sem receita registrada não podem ser desbloqueados por este catálogo.

## 4. Máquinas e processos

O runtime genérico de máquina registra item, bloco-base, lista de ingredientes, lista de produtos, duração e tecnologia requerida. O dispenser é o bloco hospedeiro; os seis primeiros slots são buffers de entrada e os três últimos são buffers de saída.

Características documentadas:
- Progresso e UUID do proprietário ficam associados ao bloco em dados persistentes.
- Máquinas com propriedade validam o dono e administradores antes de operar ou desmontar.
- Processos podem exigir era, pesquisa e energia.
- O desgaste padrão máximo é 100 ciclos; ao atingir o limite, a máquina para.
- O runtime aceita até seis tipos de entrada e três produtos por lote; filas e processamento paralelo ainda não estão implementados.

Alguns sistemas têm blocos dedicados, como geradores, bateria, tanque, incubadora e estações EMC. Verifique a página de cada módulo para o comportamento específico.

## 5. Energia — EcoPower

O sistema energético é local e baseado em geradores, baterias e conduítes de conexão. Cada máquina declara seu próprio custo por operação.

| Componente | Comportamento documentado |
|---|---|
| Gerador eólico | Representado por fornalha; gera 1 unidade de energia por segundo. |
| Gerador solar | Representado por defumador; gera 2 unidades por segundo durante o dia e sem tempestade. |
| Bateria | Representada por barril; capacidade de 10.000 unidades. |
| Conduíte energético | Funil MECHANIUM identificado; conecta baterias, geradores e máquinas do mesmo proprietário. |

Geradores carregam baterias ligadas diretamente ou por conduítes. Máquinas debitam o valor energyCostPerOperation de sua definição; o sistema agrega carga entre baterias conectadas e calcula o débito completo antes de atualizar cada bateria. A rede suporta até 256 nós, respeita o proprietário e examina somente chunks carregados. A carga da bateria é preservada no item ao desmontar e restaurada quando recolocada. Os geradores são indexados por chunk carregado em vez de varrer todos os chunks a cada segundo.

### Better Reactor

O módulo opcional `better-reactor` usa um gerador cristalino fictício. Um cristal energético MECHANIUM abastece o bloco por cinco minutos; enquanto ativo, o gerador fornece 8 unidades por segundo à rede de baterias conectada por faces/conduítes do mesmo proprietário. É uma abstração de jogo, não uma simulação de engenharia nuclear.

## 6. Fluidos — Liquid e Water Treatment

Os tanques guardam volume tipado no PDC do bloco, medido em milibuckets (mB). A capacidade padrão é 16.000 mB; a taxa de transferência padrão é 250 mB por ciclo (dois segundos).

- `purified_water` representa 250 mB de água purificada.
- `purified_water_cell` representa 1.000 mB de água purificada.
- `coolant_cell` representa 250 mB de `industrial_coolant`.
- Um tanque só aceita um fluido por vez; água purificada e refrigerante não podem ser misturados.
- Clique no tanque com um item de fluido desbloqueado para abastecer. Agache e clique com a mão vazia para retirar o maior recipiente de saída desbloqueado que caiba no volume disponível; o volume retirado depende do recipiente registrado (por exemplo, 250, 500 ou 1.000 mB).
- Tubulações são dispensers identificados e conectam tanques do mesmo proprietário por faces. A rede transporta volume, não apenas um item por ciclo.
- A rede percorre até 128 tubulações indexadas e somente chunks carregados. Não carrega chunks automaticamente.
- Itens fluidos legados no inventário são convertidos em volume quando o tanque é acessado. Tipo e quantidade são preservados no PDC do item de tanque ao desmontar e recolocar.
- Módulos e addons podem registrar fluidos customizados e recipientes pela API pública (`FluidDefinition` e `FluidContainerDefinition`). Cada recipiente declara volume em múltiplos de 250 mB e se pode abastecer e/ou esvaziar; os itens e a tecnologia necessária devem estar registrados antes. Ainda não há válvulas, bombas nem transporte de lava/água vanilla por tubulações.

## 7. Logística industrial

### Industrial Networks

Conduítes e caixas de rede formam uma rede local. A cada dois segundos, o sistema tenta mover um lote de itens MECHANIUM entre caixas com espaço. O padrão é 16 itens por rede/ciclo, configurável entre 1 e 64 em `industrial-networks.items-per-cycle`. Se o destino aceitar apenas parte do lote, somente os itens efetivamente inseridos são removidos da origem. A rede aceita até 128 conduítes adjacentes, com propriedade compatível. Apenas itens identificados como MECHANIUM são transportados; itens vanilla permanecem nos depósitos. Chunks descarregados e depósitos distantes não são conectados.

### Filtro de aceitação por caixa

O proprietário pode configurar cada caixa sem comandos:

1. Agache e clique com o botão direito na caixa segurando um item MECHANIUM para adicioná-lo à lista de aceitação.
2. Repita com o mesmo item para removê-lo.
3. Agache e clique com a mão vazia para limpar toda a lista.
4. Uma lista vazia significa que a caixa aceita qualquer item MECHANIUM; uma lista preenchida aceita somente os IDs listados.

O filtro fica salvo no bloco e é copiado para o item recuperado quando a caixa é desmontada. Só o proprietário ou um administrador pode alterá-lo. Prioridades entre caixas ainda não existem.

### Simple Storage

Nós de armazenamento conectados face a face e pertencentes ao mesmo dono podem abrir um inventário virtual paginado. Uma rede comporta até 64 nós. O acesso respeita proprietário, jogadores confiáveis e administradores. A rede não atravessa blocos intermediários nem liga depósitos distantes.

### Armazém portátil

O item de armazenamento portátil possui inventário persistido nos dados do próprio item. O nível normal tem 27 espaços; o upgrade `portable_storage_advanced` tem 54. A primeira implementação ainda não oferece filtros, compressão, múltiplos tiers ou pesquisa global.

## 8. Geologia e prospecção

Depois de desbloquear `geological_surveying`, o jogador pode fabricar o kit de prospecção. Ao usá-lo em um bloco, a ferramenta conta minérios em um volume local de 9×9×9. Chunks descarregados são ignorados para evitar geração de terreno durante a varredura.

Depósitos regionais, veios minerais, amostras e integração profunda com mineração continuam pendentes.

## 9. Agricultura, horticultura e culinária

Os módulos agrícolas adicionam sementes, substrato, caixotes de colheita e itens de cadeia alimentar. A culinária inclui queijo, ensopado, carne conservada, rações e conservas. Exotic Garden inclui ervas, extrato botânico, mistura de sementes e conserva tropical.

O moinho industrial transforma trigo em farinha MECHANIUM e a conservadora transforma frutas doces em conserva. São máquinas de entrada/saída e exigem energia.

O módulo Greenhouse acrescenta o `greenhouse_controller`: coloque-o como dispenser e conecte uma bateria à rede energética do mesmo proprietário. Depois de desbloquear `controlled_environment_farming`, o controlador pode dar **um estágio adicional** a uma planta compatível (`Ageable`) dentro do raio configurado quando ela já tiver crescido normalmente. Cada bônus consome a quantidade configurada de energia. Se a rede estiver sem energia, o crescimento normal permanece, mas o bônus não ocorre. O sistema só examina chunks carregados e não quebra, coloca, colhe ou replanta blocos.

O raio padrão é 4 blocos (configurável entre 1 e 8) e o custo padrão é 1 unidade por crescimento extra. O item antigo `climate_controller` permanece como legado; a máquina funcional nova é `greenhouse_controller`.

Limitações: não há hidroponia automatizada, colheita automática, replantio nem culturas customizadas. A estufa ainda não substitui um sistema agrícola completo.

## 10. Incubadoras e máquinas rápidas

Electric Spawners adiciona quatro incubadoras de animais passivos:
- Sementes → ovo de galinha;
- Feno → ovo de vaca;
- Cenouras → ovo de porco;
- Trigo → ovo de ovelha.

Exigem a pesquisa `livestock_incubation` e uma rede energética do mesmo proprietário com o custo declarado na definição da máquina. Não criam criaturas hostis.

Fast Machines adiciona:
- Prensa rápida: uma barra de ferro gera duas peças mecânicas em 20 ticks.
- Enrolador: cobre gera duas bobinas em 40 ticks.

Ambas exigem energia e a pesquisa `fast_machinery`. Continuam sujeitas ao limite do runtime genérico.

## 11. Manutenção e desgaste

Máquinas acumulam desgaste por ciclo, até 100 por padrão. Quando atingem o limite, param. O proprietário pode agachar e clicar na máquina com barras de ferro para reparar. O módulo Hardcore Slimefun adiciona o kit `machine_maintenance_kit`, desbloqueado por pesquisa, e regras opcionais mais rígidas.

`machines.hardcore.enabled` vem desativado. Quando ativado, aplica o multiplicador de desgaste e eleva o custo em barras de ferro para reparo.

## 12. Geradores passivos de materiais

O módulo Material Generators possui oito geradores passivos. Cada gerador precisa de um baú diretamente acima; o intervalo padrão é 200 ticks por unidade. O progresso fica associado ao bloco. A produção pausa quando não há baú ou espaço. Geradores têm proprietário e proteção contra desmontagem indevida, explosões e pistões.

A primeira etapa não possui rede de distribuição própria, filtros ou upgrades de velocidade.

## 13. Ambiente e impacto industrial

Cada ciclo de produção de máquina pode acrescentar uma unidade abstrata ao contador persistente de impacto industrial do mundo. A tecnologia `emissions_control` evita novas contagens das máquinas daquele proprietário; ela não reduz o contador histórico.

O comando `/mechanium environment` consulta o contador do mundo atual; pelo console, a documentação registra consulta de todos os mundos. Filtro de carbono e kit de recuperação possuem receitas, mas ainda não têm efeito de instalação/uso no mundo.

## 14. Ferramentas e utilidades

- **ExtraGear:** picareta reforçada concede Haste breve após mineração; bússola de levantamento informa coordenadas, bioma, direção e bloco observado.
- **SensibleToolbox:** kit de campo concede Speed e Night Vision temporárias, com cooldown de 30 segundos; kit de manutenção consumível repara um item do inventário em 25% da durabilidade máxima, com cooldown de 10 segundos.
- **Towny:** integração opcional lê UUID/nome da nação e publica o serviço `NationIdentityProvider`. Não é requisito de instalação.
- **Drops de criaturas:** a API permite registrar subprodutos por tipo de criatura, quantidade, chance e tecnologia sem remover os drops vanilla.

## 15. Equivalência EMC

O módulo `emc-exchange` mantém um saldo privado por UUID e catálogo de itens aprendidos. O Dissolvedor converte itens com valor configurado em EMC; o Materializador sintetiza uma unidade de item previamente aprendido e debita o valor correspondente. Existem cinco níveis de cada estação.

O valor unitário máximo aceito por tier é 16, 64, 256, 1.024 e 4.096 EMC. O sistema é manual nesta etapa e não substitui uma economia monetária. A integração energética, logística automática, PlaceholderAPI e cálculo automático de valor por receita ainda não estão implementados. Consulte [EMC Exchange](../EMC_EXCHANGE.md).

## 16. Módulos opcionais de fantasia e futuro

`optional-fantasy` e `optional-futuristic` estão desativados por padrão. São domínios opcionais separados e não devem alterar a progressão histórica padrão quando desligados.

## Limitações transversais

- O alvo é Paper/Purpur 1.21.11 e Java 21; outros ambientes não estão declarados como suportados.
- O runtime genérico de máquinas usa dispenser, até seis tipos de insumo e três produtos por lote; cada máquina informa ingredientes, produtos, tempo e custo de energia ao clicar com o botão direito. Filas e processamento paralelo permanecem pendentes.
- Não existem filas de produção ou processamento paralelo no runtime genérico.
- Energia, fluidos, armazenamento e conduítes são redes locais; não atravessam distâncias sem conexões.
- Algumas referências/addons têm apenas uma implementação inicial, não paridade completa.
- CI e testes unitários não substituem smoke tests em servidor real.
