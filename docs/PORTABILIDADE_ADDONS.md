# Matriz de portabilidade dos projetos de referência

A lista consolidada contém **31 projetos únicos**. URLs duplicadas foram contadas uma vez. Esta matriz define a direção de análise; não afirma que as funcionalidades já foram implementadas.

| Projeto | Domínio candidato | Direção MECHANIUM |
|---|---|---|
| ExoticGarden | Agricultura, cultivo e alimentos | Safras, cultivo e processamento coerentes com o mundo |
| ElectricSpawners | Fauna e produção controlada | Auditar; priorizar manejo e pecuária com limites ecológicos |
| PrivateStorage | Armazenamento privado | Cofres, propriedade UUID, permissões e auditoria |
| ExtraGear | Ferramentas e equipamentos | Ferramentas utilitárias por era |
| SlimefunOreChunks | Mineração e geologia | Prospecção e recursos regionais |
| LiteXpansion | Máquinas e indústria | Processamento industrial e automação |
| EcoPower | Energia renovável | Geração, armazenamento e redes |
| HardcoreSlimefun | Balanceamento | Custos, desgaste e dificuldade configuráveis |
| SFMobDrops | Recursos de criaturas | Drops equilibrados e configuráveis |
| SFCalc | Calculadora e utilidades | Utilitário de informação e cálculo |
| FluffyMachines | Máquinas | Filas, consumo e produção por era |
| GlobalWarming | Clima e ambiente | Emissões, clima e impacto ambiental |
| SlimefunWarfare | Sistemas geopolíticos | Somente diplomacia, orçamento e logística civil abstrata |
| Liquid | Fluidos | Tubulações, tanques e transferência |
| SlimeCustomizer | Configuração de conteúdo | Ferramentas administrativas internas |
| SMG | A auditar | Identificar funções, licença e dependências antes de decidir |
| Simple Storage | Armazéns | Capacidade, acesso e logística |
| Galactifun | Exploração espacial | Módulo de ficção opcional, se mantido |
| FNAmplifications | Amplificação/produção | Auditar processos concretos antes de incluir |
| Element Manipulation | Manipulação fantástica | Módulo de fantasia opcional, separado |
| DankTech2 | Armazenamento portátil | Inventário e armazenamento portáteis |
| Networks | Redes de itens | Roteamento, capacidade, energia e propriedade |
| MissileWarfare | Conteúdo bélico | Não incorporar mecânicas de armas, ataques ou operação de combate |
| EMCTech | Transmutação/EMC | Não incorporar geração/transmutação fantástica gratuita de recursos no núcleo |
| Gastronomicon | Culinária | Receitas, alimentos e cadeia produtiva |
| Better Nuclear Reactor | Energia nuclear | Avaliar somente conceitos energéticos abstratos e não operacionais |
| AdvancedTech | Tecnologia avançada | Processos industriais plausíveis e escalonados |
| FastMachines | Eficiência de máquinas | Otimização com custos e limites |
| MapJammers | Interferência eletrônica | Não portar jamming; cartografia e informação geográfica são candidatas |
| SensibleToolbox | Ferramentas | Ferramentas utilitárias não redundantes |
| GeyserHeads | Cosméticos Bedrock/Geyser | Compatibilidade visual opcional |

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

**Não portado:** funcionalidades completas dos 31 projetos. A fundação inicial implementa API, persistência, progressão básica, módulos de registro e carregador de addons. Cada domínio ainda exige auditoria e implementação com testes próprios.
