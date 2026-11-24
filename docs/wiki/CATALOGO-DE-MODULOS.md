# Catálogo de módulos do MECHANIUM

O arquivo `plugins/MECHANIUM/config.yml` controla os módulos internos. Nesta linha existem **49 IDs de módulo** declarados na configuração: **47 habilitados por padrão** e dois opcionais desabilitados (`optional-fantasy` e `optional-futuristic`). Dependências entre módulos também podem impedir a inicialização de um módulo se uma dependência necessária estiver desligada.

A tabela descreve o escopo atualmente documentado. “Parcial” significa que existe conteúdo inicial, mas não paridade completa com a ideia de origem ou com o sistema futuro pretendido.

## Fundação e progressão

| ID | Finalidade |
|---|---|
| `industry` | Núcleo de tecnologias, componentes e processos industriais; base do runtime de máquinas. |
| `geology` | Prospecção geológica local com kit de levantamento 9×9×9. |
| `agriculture` | Insumos agrícolas, sementes, substrato, embalagem de colheitas e tecnologias de cultivo. Crescimento customizado e colheita mecanizada completa ainda são pendentes. |
| `energy` | Progressão de tecnologias energéticas e dependência para sistemas de geração/armazenamento. |
| `storage` | Base de progressão de armazenamento e itens/tecnologias de estoque. |
| `environment` | Contador abstrato de impacto industrial por mundo e tecnologia de controle de emissões. |
| `culinary` | Cadeias alimentares e receitas, incluindo queijo, ensopado, carne conservada e rações. |
| `ore-processing` | Beneficiamento mineral e materiais intermediários de processamento. |
| `exotic-garden` | Misturas de sementes, ervas desidratadas, extrato botânico, conserva tropical e caixote de pomar. Parcial: sem plantas customizadas completas. |

## Ferramentas, cálculo, armazenamento e energia

| ID | Finalidade |
|---|---|
| `extra-gear` | Picareta reforçada com Haste breve e bússola de levantamento (coordenadas, bioma, direção e bloco observado). |
| `sensible-toolbox` | Kit de campo com efeitos temporários e kit consumível de reparo de equipamento. |
| `sfcalc` | Comando de cálculo recursivo de materiais/lotes de receitas MECHANIUM; não calcula preços monetários. |
| `eco-power` | Geradores eólico/solar, baterias de 10.000 unidades e conduítes energéticos; rede por proprietário de até 256 nós, somente em chunks carregados. A carga da bateria é preservada ao desmontar/recolocar. |
| `liquid` | Filtro, tanques de volume tipado (16.000 mB por padrão) e tubulações; água purificada e refrigerante, sem mistura de fluidos. |
| `advanced-tech` | Britador, refinaria e montadora industrial com processamento e consumo energético. |
| `simple-storage` | Rede de inventários virtuais paginados com até 64 nós adjacentes e controle de acesso. |
| `dank-tech2` | Upgrade do armazém portátil de 27 para 54 espaços. |
| `emc-exchange` | Saldo EMC individual, Dissolvedores, Materializadores e catálogo de itens aprendidos. Veja [guia EMC](../EMC_EXCHANGE.md). |

## Máquinas e indústria especializada

| ID | Finalidade |
|---|---|
| `electric-spawners` | Quatro incubadoras elétricas de animais passivos: galinha, vaca, porco e ovelha. |
| `fast-machines` | Prensa rápida e enrolador de bobinas, com ciclos de 20 e 40 ticks e consumo de energia. |
| `hardcore-slimefun` | Pesquisa e kit de manutenção; modo opcional de desgaste/custo de reparo mais rigoroso. |
| `air-filtration` | Purificador de ar e cartuchos; a receita da estação consome um balde de água. |
| `advanced-metallurgy` | Componentes metalúrgicos, aço e forno metalúrgico. |
| `pressure-engineering` | Placas compactadas e câmara de pressão. |
| `industrial-automation` | Circuitos de controle e montadora automática. |
| `industrial-logistics` | Caixas de transporte e roteador de carga. |
| `forestry` | Fibras de madeira, resina e estação florestal. |
| `textile-industry` | Fibras, tecidos industriais e tear mecanizado. |
| `biofuel` | Biomassa e células de biocombustível abstratas. |
| `construction-materials` | Cerâmica, painéis reforçados e estruturas pré-fabricadas com cadeia de mistura. |
| `industrial-chemistry` | Reagentes fictícios, resina, fibra sintética e misturador industrial; não simula química real. |
| `fn-amplifications` | Núcleos, bobinas, módulos de amplificação e estação baseada no runtime genérico. |

## Redes, produção de recursos, água e transporte

| ID | Finalidade |
|---|---|
| `industrial-networks` | Conduítes e caixas protegidas para transporte local de itens MECHANIUM. Limite documentado de 128 conduítes por rede. |
| `better-reactor` | Gerador cristalino fictício que abastece a rede de baterias conectada por faces/conduítes; abstração de jogo, não simulação técnica real. |
| `material-generators` | Oito geradores passivos, com baú superior e intervalo configurável (200 ticks por padrão). |
| `water-treatment` | Cartuchos, células de água purificada e refrigerante; os tanques armazenam os dois fluidos por volume tipado, sem misturá-los. |
| `greenhouse` | Painéis de estufa, solução nutritiva e bandejas hidropônicas. Parcial: não equivale a crescimento de culturas customizadas automatizado. |
| `industrial-recycling` | Sucata mista, materiais separados e componentes de triagem/reciclagem. |
| `ore-chunks` | Fragmentos de ferro, cobre e ouro e lotes minerais compactados. |
| `aquaculture` | Ração aquícola, rede de criação, fertilizante reaproveitado, caixote de pescado e incubadora. |
| `glassworks` | Sílica, vidro industrial, painéis temperados, lentes de precisão e forno de vidro. |
| `rail-freight` | Dormentes, trilhos reforçados, paletes e manifestos de carga intermodal; não representa um sistema completo de trens autônomos. |
| `battery-industry` | Separadores, invólucros, células montadas e bancos de baterias industriais. |
| `cold-chain` | Fibras isolantes, células de refrigeração, caixotes térmicos e unidade refrigeradora. |
| `mining-automation` | Componentes de prospecção e módulos mineradores com montadora. Parcial: não extrai blocos automaticamente nesta etapa. |

## Catálogo, cosméticos e personalização

| ID | Finalidade e estado |
|---|---|
| `head-collection` | Fichas cosméticas, cabeças de coleção e expositores locais, sem dependências externas. |
| `slime-customizer` | Modelos, projetos em branco, ferramenta e catálogo de conteúdo. O editor administrativo visual completo ainda está pendente. |

## Módulos opcionais

| ID | Padrão | Finalidade |
|---|---|---|
| `optional-fantasy` | Desativado | Alquimia fictícia e tecnologias arcanas em progressão separada. |
| `optional-futuristic` | Desativado | Tecnologias de futuro especulativo em progressão separada. |

## Limitações comuns a vários módulos

- O runtime comum de dispenser aceita até seis tipos de entrada e três produtos por lote; o custo energético por operação fica na definição de cada máquina.
- A rede de fluidos suporta atualmente água purificada e refrigerante em volumes de mB. Válvulas, bombas, outros fluidos e API pública de registro para addons ainda não existem.
- Ter itens, categorias e receitas cadastrados não significa que o módulo possua automação completa ou todas as funcionalidades de uma referência externa.
- Greenhouse e Agriculture ainda não representam um simulador completo de culturas customizadas.
- Mining Automation possui componentes e receitas, mas não mineração autônoma de blocos.
- Head Collection é um catálogo cosmético local. MECHANIUM não inclui nem exige Geyser e não implementa uma camada Bedrock.
- Slime Customizer ainda não tem editor administrativo visual completo.
- Rail Freight registra itens e receitas de carga, não um sistema ferroviário autônomo completo.
- Validação real de cada cadeia precisa ocorrer em Paper/Purpur 1.21.11.

Para detalhes de implementação e limitações globais, consulte [Sistemas e mecânicas](SISTEMAS.md), [Estado real](../STATUS.md) e [Roadmap](../ROADMAP.md).
