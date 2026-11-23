# Matriz de portabilidade dos projetos de referência

A lista consolidada contém **31 projetos únicos**. URLs duplicadas foram contadas uma vez. Esta matriz define a direção de análise; não afirma que as funcionalidades já foram implementadas.

| Projeto | Domínio candidato | Direção MECHANIUM |
|---|---|---|
| ExoticGarden | Agricultura, cultivo e alimentos | Safras, cultivo e processamento coerentes com o mundo |
| ElectricSpawners | Fauna e produção controlada | Quatro incubadoras elétricas para animais passivos, com spawn eggs e consumo de energia; sem criaturas hostis |
| PrivateStorage | Armazenamento privado | Cofres, propriedade UUID, permissões e auditoria |
| ExtraGear | Ferramentas e equipamentos | Ferramentas utilitárias por era |
| SlimefunOreChunks | Mineração e geologia | Prospecção e recursos regionais |
| LiteXpansion | Máquinas e indústria | Processamento industrial e automação |
| EcoPower | Energia renovável | Geração, armazenamento e redes |
| HardcoreSlimefun | Balanceamento | Modo hardcore opcional, desgaste configurável e kit de manutenção por pesquisa |
| SFMobDrops | Recursos de criaturas | Drops equilibrados e configuráveis |
| SFCalc | Calculadora e utilidades | Utilitário de informação e cálculo |
| FluffyMachines | Máquinas | Filas, consumo e produção por era |
| GlobalWarming | Clima e ambiente | Indicador abstrato de impacto industrial e célula consumível de recuperação; não simula clima real |
| SlimefunWarfare | Sistemas geopolíticos | Somente diplomacia, orçamento e logística civil abstrata |
| Liquid | Fluidos | Tubulações, tanques e transferência |
| SlimeCustomizer | Configuração de conteúdo | Projetos de receita/máquina e editor administrativo para alterar receitas registradas; adicionar novos IDs ainda pendente |
| SMG (SimpleMaterialGenerators) | Processamento de materiais | Peneira e separador com receitas e máquinas próprias; expansão de geradores autônomos pendente |
| Simple Storage | Armazéns | Capacidade, acesso e logística |
| Galactifun | Exploração espacial | Módulo de ficção opcional, se mantido |
| FNAmplifications | Amplificação/produção | Auditar processos concretos antes de incluir |
| Element Manipulation | Manipulação fantástica | Módulo de fantasia opcional, separado |
| DankTech2 | Armazenamento portátil | Inventário e armazenamento portáteis |
| Networks | Redes de itens | Nó de armazenamento incorporado à rede local de até 64 nós; redes remotas e roteamento automático pendentes |
| MissileWarfare | Conteúdo bélico | Não incorporar mecânicas de armas, ataques ou operação de combate |
| EMCTech | Transmutação/EMC | Não incorporar geração/transmutação fantástica gratuita de recursos no núcleo |
| Gastronomicon | Culinária | Receitas, alimentos e cadeia produtiva |
| Better Nuclear Reactor | Energia nuclear | Avaliar somente conceitos energéticos abstratos e não operacionais |
| AdvancedTech | Tecnologia avançada | Processos industriais plausíveis e escalonados |
| FastMachines | Eficiência de máquinas | Prensa e enrolador acelerados ligados à rede energética; filas e paralelismo pendentes |
| MapJammers | Interferência eletrônica | Não portar jamming; cartografia e informação geográfica são candidatas |
| SensibleToolbox | Ferramentas | Ferramentas utilitárias não redundantes |
| GeyserHeads | Cosméticos Bedrock/Geyser | Compatibilidade visual opcional |

## Implementações parciais

- **SFMobDrops (parcial / clean-room):** API pública `MobDropDefinition` aceita entidade Bukkit, item MECHANIUM, quantidade mínima/máxima, chance e tecnologia necessária. `MechaniumMobDropRegisterEvent` informa addons quando uma regra é registrada.
- O listener preserva drops vanilla e acrescenta apenas os subprodutos personalizados cujo jogador responsável pelo abate atende à tecnologia exigida. Definições com chance e quantidades são persistidas no SQLite (schema 3) e restauradas ao inicializar a API; o módulo/addon ainda deve registrar seu listener de gameplay a cada inicialização.
- O módulo de agricultura registra gordura animal como subproduto de vacas, porcos e ovelhas após `crop_rotation`, além de receitas iniciais de composto, fertilizante e tochas de sebo.
- Ainda não há configuração por bioma, Looting, dificuldade, multiplicadores administrativos ou painel de edição de tabelas.
- **PrivateStorage (parcial):** o módulo `storage` registra `private_storage_crate` e uma receita condicionada a `warehouse_accounting`. O plugin protege o barril por UUID do proprietário, permite até 32 UUIDs confiáveis e bloqueia abertura/retirada por funil, quebra por terceiros, pistões e explosões. O acesso é gerenciado com `/mechanium storage trust|untrust|list`.
- Ainda não foram portados recursos de rede, armazenamento em massa, filtros, pesquisa global, transferência automática ou permissões por grupos.
- **SlimefunOreChunks (parcial / reimplementação independente):** o módulo `geology` adiciona a pesquisa `geological_surveying`, o item `geological_survey_kit` e uma receita. Ao usar o kit em um bloco, o scanner conta os tipos de minério em um cubo local de 9×9×9, ignora chunks descarregados e apresenta os cinco tipos mais frequentes. Há cooldown e testes unitários da agregação.
- Esta primeira entrega não gera depósitos ou veios minerais, não altera a distribuição de minério do mundo e não implementa amostras, mapas geológicos ou integração com mineração. É uma ferramenta de prospecção inicial, não uma paridade completa com o addon de referência.

## Implementações adicionais anteriores
- **ExoticGarden/Gastronomicon (parcial):** expansão clean-room de agricultura e culinária com sementes, substrato, embalagens de colheita, queijo, ensopado, carne conservada e rações. Ainda não há plantas customizadas, estufas funcionais ou árvore completa de alimentos.
- **FluffyMachines/LiteXpansion (parcial):** moinho industrial e conservadora de frutas usam o runtime de máquinas MECHANIUM e consomem energia; o runtime permanece limitado a um insumo e um produto por ciclo.
- **Networks (parcial):** rede local de depósitos face a face existente, com limite de 64 nós e paginação.
- **DankTech2 (parcial):** armazém portátil único com inventário de 27 espaços salvo no próprio item. Não há upgrades, filtros, compressão ou busca global.
- **GlobalWarming (parcial):** contador persistente abstrato por mundo; ciclos industriais somam impacto, e a tecnologia emissions_control interrompe novas contagens para máquinas daquele proprietário. Não simula temperatura, clima ou eventos ambientais.
- **HardcoreSlimefun/FastMachines (base anterior):** o runtime já aplicava desgaste e bônus de eficiência; os módulos 0.5.0 acrescentam manutenção desbloqueável, regras opcionais de dificuldade e duas máquinas rápidas dedicadas.
## Implementações 0.5.0-SNAPSHOT

- **ElectricSpawners (parcial / clean-room):** quatro incubadoras construídas sobre o runtime comum de máquinas. Elas consomem sementes, feno, cenouras ou trigo e geram ovos de invocação vanilla de galinha, vaca, porco ou ovelha. Todas exigem a pesquisa `livestock_incubation` e energia de bateria adjacente. O primeiro estágio não cria criaturas hostis, não automatiza abate e não reproduz o comportamento integral do addon original.
- **FastMachines (parcial / clean-room):** a prensa rápida transforma uma barra de ferro em duas peças mecânicas em um ciclo de 20 ticks; o enrolador transforma cobre em duas bobinas em 40 ticks. Ambos exigem energia e a pesquisa `fast_machinery`. Ainda compartilham o limite do runtime atual: dispenser, uma entrada e uma saída, sem filas nem processamento paralelo.
- **HardcoreSlimefun (parcial / clean-room):** inclui pesquisas de confiabilidade/manutenção e o item `machine_maintenance_kit`, que restaura o desgaste de uma máquina quando usado agachado sobre ela. O modo opcional `machines.hardcore.enabled` pode multiplicar o desgaste por operação e elevar o custo de reparo com ferro; vem desativado por padrão.


## Implementações 0.7.0.00-SNAPSHOT

- **GlobalWarming (primeira etapa):** `carbon_capture_cell` reduz em 10 unidades o contador abstrato de impacto industrial do mundo. O módulo reutiliza o indicador persistente existente; não representa temperatura ou clima.
- **Networks (primeira etapa):** `network_storage_node` é um barril especial que participa da rede de inventário local, conectada por faces e com controle de proprietário/permissões.
- **SimpleMaterialGenerators (primeira etapa):** duas máquinas no runtime comum: cascalho → fragmento mineral e areia → pelota de argila. Não há geração de recursos sem insumos.
- **SlimeCustomizer (primeira etapa):** receitas de projetos e atalho `/mechanium customizer` para o editor administrativo de receitas que já existem. Interface de criação completa de itens, tecnologias e máquinas novas ainda não foi implementada.
- **SlimyRepair (primeira etapa):** kit consumível repara um item danificado em 50% da durabilidade máxima.
- **Pontos de pesquisa:** mineração de novos tipos de minério, primeira visita a cada bioma e conquistas elegíveis concedem recompensas individuais, persistidas para impedir farming repetido.
- **Catálogo:** seleção por tipo e categoria, requisitos de tecnologia, lista de insumos e fabricação por clique com verificação de espaço e consumo seguro.

## Regras de portabilidade

1. Auditar repositório, commit, licença, dependências e funcionalidades antes de copiar código.
2. Reimplementar por domínio na API MECHANIUM; evitar dependência de APIs internas do Slimefun.
3. Registrar cada item, máquina e receita com IDs persistentes e progressão central.
4. Não atribuir eras sem analisar a função e a coerência histórica.
5. Documentar funcionalidades não portadas, adaptações e limitações.
6. Não considerar um projeto compatível apenas porque compilou.
7. Recursos fantasiosos/futuristas permanecem em módulos opcionais separados.
8. Sistemas relacionados a armas, ataques e operação de combate não serão implementados; diplomacia, economia, defesa civil e logística abstrata podem ser simuladas sem detalhes operacionais.

## Situação atual

**Não portado:** funcionalidades completas dos 31 projetos. Há implementações parciais para PrivateStorage, SFMobDrops e o domínio inicial de prospecção geológica. Os demais domínios ainda exigem auditoria e implementação com testes próprios.
