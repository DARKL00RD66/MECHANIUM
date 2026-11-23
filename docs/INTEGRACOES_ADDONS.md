# Plano de integração dos addons de referência

O MECHANIUM é independente do Slimefun e do Historical Ages. Os nomes abaixo são referências de funcionalidades a reimplementar em APIs próprias; não significam que os JARs originais sejam dependências ou que a compatibilidade integral esteja concluída. A portabilidade é feita com auditoria de licenças, sem copiar código ou assets sem autorização.

## Matriz de 31 referências

| Referência | Direção no MECHANIUM | Situação |
|---|---|---|
| ExoticGarden | Horticultura, cultivo e cadeias agrícolas | Primeira expansão funcional: sementes, ervas, extratos botânicos, conservas e caixote de colheita; cultivo customizado pendente |
| ElectricSpawners | Manejo de criação e produção animal | Planejado |
| PrivateStorage | Armazéns privados, proprietário e permissões | Base funcional implementada |
| ExtraGear | Ferramentas e equipamentos de utilidade | Picareta reforçada e bússola de levantamento com habilidades utilitárias |
| SlimefunOreChunks | Prospecção e beneficiamento mineral | Scanner local e primeira cadeia de beneficiamento; depósitos regionais pendentes |
| LiteXpansion | Cadeias industriais e processamento | Primeira cadeia e máquinas industriais adicionais implementadas |
| EcoPower | Energia renovável e eficiência | Geradores eólicos/solares, baterias e consumo de energia nas máquinas |
| HardcoreSlimefun | Regras configuráveis de progressão e balanceamento | Planejado |
| SFMobDrops | Subprodutos de criaturas | Base funcional implementada |
| SFCalc | Consulta e cálculo de receitas/custos | Comando com lotes e materiais recursivos; sem preços monetários |
| FluffyMachines | Máquinas industriais e automação | Prensa, britador, refinaria, montadora e filtro com runtime inicial |
| GlobalWarming | Emissões e impacto ambiental | Tecnologias ambientais iniciais; simulação pendente |
| SlimefunWarfare | Não serão portadas mecânicas de combate | Excluído do escopo operacional |
| Liquid | Redes de fluidos e armazenamento | Filtro, tanques e transporte de água purificada por tubulações |
| SlimeCustomizer | Ferramentas de configuração de conteúdo | API e addons externos formam a fundação; interface administrativa pendente |
| SMG | Identidade e comportamento da referência ainda precisam ser confirmados | Aguardando validação |
| Simple Storage | Armazenamento compartilhado e logística | Rede local paginada de até 64 nós adjacentes, com propriedade e acesso |
| Galactifun | Conteúdo de exploração espacial | Apenas módulo futurista opcional, desativado por padrão |
| FNAmplifications | Extensões de conteúdo e amplificação | Aguardando validação funcional e de licença |
| Element Manipulation | Conteúdo fantástico | Apenas módulo de fantasia opcional, desativado por padrão |
| DankTech2 | Armazenamento portátil | Primeira expansão: upgrade para armazém portátil persistente de 54 espaços; outras tiers e filtros pendentes |
| Networks | Redes logísticas entre máquinas e depósitos | Planejado; redes atuais são locais e específicas por domínio |
| MissileWarfare | Não serão portadas armas, mísseis ou mecânicas de ataque | Excluído do escopo operacional |
| EMCTech | Processamento e conversão de recursos | Aguardando confirmação da referência e da licença |
| Gastronomicon | Culinária e conservação de alimentos | Primeira cadeia de culinária adicionada |
| Better Nuclear Reactor | Modelagem abstrata de geração e eficiência energética | Planejado; sem instruções ou simulação técnica do mundo real |
| AdvancedTech | Processos industriais avançados | Britador, refinaria e montadora com energia |
| FastMachines | Filas, desempenho e limites de processamento | Planejado |
| MapJammers | Interferência/jamming não será implementada; cartografia civil pode ser avaliada separadamente | Excluído o mecanismo de interferência |
| SensibleToolbox | Ferramentas utilitárias e qualidade de vida | Kit de campo e kit de manutenção com habilidades próprias |
| GeyserHeads | Compatibilidade de apresentação com clientes Bedrock/Geyser | Planejado |

## Funcionalidades ampliadas nesta etapa (0.3.0–0.4.0)

- **ExoticGarden:** cadeia inicial de horticultura com mistura de sementes, ervas desidratadas, extrato botânico, conserva tropical e caixote de pomar; ainda sem crescimento de culturas customizadas.\n- **DankTech2:** upgrade de armazém portátil que amplia o inventário persistente de 27 para 54 espaços; tiers adicionais e filtros continuam pendentes.\n- **ExtraGear:** picareta reforçada aplica Haste breve após mineração; bússola apresenta coordenadas, biome, direção e bloco observado.
- **EcoPower:** gerador eólico (1 unidade/s), gerador solar (2 unidades/s de dia sem tempestade), baterias até 10.000 unidades e consumo por máquinas industriais.
- **SFCalc:** `/mechanium calc <receita> [quantidade]`, com cálculo de lotes e expansão de ingredientes MECHANIUM até oito níveis.
- **Liquid:** filtro de água, tanques e tubulações adjacentes que transportam água purificada entre tanques do mesmo proprietário.
- **AdvancedTech:** britador, refinaria e montadora, todos alimentados pela rede simplificada.
- **Simple Storage:** inventário virtual paginado para nós de barril face a face, com até 64 nós e controle de acesso.
- **SensibleToolbox:** kit de campo concede velocidade/visão noturna temporárias; kit de manutenção repara um equipamento em 25% da durabilidade máxima.

## Regras de portabilidade

1. Cada domínio é implementado como módulo oficial independente e pode ser desativado em `config.yml`.
2. Addons externos continuam usando a API pública do MECHANIUM.
3. Uma entrada nesta matriz não equivale à paridade integral com o projeto de referência.
4. Conteúdo fantástico/futurista permanece opcional e desativado por padrão.
5. Sistemas de combate, armas, ataques, mísseis e interferência não fazem parte da implementação operacional.
6. A versão 0.4.0-SNAPSHOT ainda depende de CI e testes funcionais em Paper/Purpur 1.21.11.
