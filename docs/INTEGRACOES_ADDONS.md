# Plano de integração dos addons de referência

O MECHANIUM é independente do Slimefun. Os nomes abaixo são referências de funcionalidades a reimplementar na API própria; não significam que os JARs originais sejam dependências ou que a compatibilidade integral já esteja concluída. O trabalho é feito por módulos e com auditoria de licenças, sem copiar código ou assets sem autorização.

## Matriz de 31 referências

| Referência | Direção no MECHANIUM | Situação |
|---|---|---|
| ExoticGarden | Horticultura, cultivo e cadeias agrícolas | Primeira base agrícola existente; expansão pendente |
| ElectricSpawners | Manejo de criação e produção animal | Planejado |
| PrivateStorage | Armazéns privados, proprietário e permissões | Base funcional implementada |
| ExtraGear | Ferramentas e equipamentos de utilidade | Primeira camada de ferramentas e receitas adicionada; habilidades especiais pendentes |
| SlimefunOreChunks | Prospecção e beneficiamento mineral | Prospecção e primeira cadeia de minério implementadas; depósitos regionais pendentes |
| LiteXpansion | Cadeias industriais e processamento | Primeira cadeia de beneficiamento adicionada |
| EcoPower | Energia renovável e eficiência | Componentes e pesquisas eólicas/solares adicionados; geração/rede elétrica pendentes |
| HardcoreSlimefun | Regras configuráveis de progressão e balanceamento | Planejado |
| SFMobDrops | Subprodutos de criaturas | Base funcional implementada |
| SFCalc | Consulta e cálculo de receitas/custos | Consulta de receitas via `/mechanium calc <id>` adicionada |
| FluffyMachines | Máquinas industriais e automação | Prensa inicial funcional; expansão pendente |
| GlobalWarming | Emissões e impacto ambiental | Tecnologias ambientais iniciais; simulação pendente |
| SlimefunWarfare | Não serão portadas mecânicas de combate | Excluído do escopo operacional |
| Liquid | Redes de fluidos e armazenamento | Filtro de água funcional; redes gerais de fluidos pendentes |
| SlimeCustomizer | Ferramentas de configuração de conteúdo | API e addons externos já oferecem a fundação; interface administrativa pendente |
| SMG | Identidade e comportamento da referência ainda precisam ser confirmados | Aguardando validação |
| Simple Storage | Armazenamento compartilhado e logística | Nó adicional protegido; rede multi-inventário pendente |
| Galactifun | Conteúdo de exploração espacial | Apenas módulo futurista opcional, desativado por padrão |
| FNAmplifications | Extensões de conteúdo e amplificação | Aguardando validação funcional e de licença |
| Element Manipulation | Conteúdo fantástico | Apenas módulo de fantasia opcional, desativado por padrão |
| DankTech2 | Armazenamento portátil | Planejado |
| Networks | Redes logísticas entre máquinas e depósitos | Planejado |
| MissileWarfare | Não serão portadas armas, mísseis ou mecânicas de ataque | Excluído do escopo operacional |
| EMCTech | Processamento e conversão de recursos | Aguardando confirmação da referência e da licença |
| Gastronomicon | Culinária e conservação de alimentos | Primeira cadeia de culinária adicionada |
| Better Nuclear Reactor | Modelagem abstrata de geração e eficiência energética | Planejado; sem instruções ou simulação técnica do mundo real |
| AdvancedTech | Processos industriais avançados | Peças de precisão e montadora industrial adicionadas |
| FastMachines | Filas, desempenho e limites de processamento | Planejado |
| MapJammers | Interferência/jamming não será implementada; cartografia civil pode ser avaliada separadamente | Excluído o mecanismo de interferência |
| SensibleToolbox | Ferramentas utilitárias e qualidade de vida | Kits utilitários e manuais com receitas adicionados; habilidades especiais pendentes |
| GeyserHeads | Compatibilidade de apresentação com clientes Bedrock/Geyser | Planejado |

## Primeiras integrações desta etapa

- **Sete novas referências:** ExtraGear, EcoPower, SFCalc, Liquid, AdvancedTech, Simple Storage e SensibleToolbox receberam módulos nativos com pesquisas e conteúdo próprio. Os limites específicos estão descritos em `docs/MODULOS.md`.

- **Culinária/horticultura:** módulo `culinary`, com tecnologias por era, farinha, massa, conservas, rações e receitas próprias.
- **Beneficiamento mineral:** módulo `ore-processing`, com peneiramento, britagem e refino por progressão. Isso ainda não gera depósitos regionais nem substitui a mineração vanilla.
- **Agricultura, armazenamento e drops:** aproveitam os módulos já existentes, sem dependência do Slimefun.
- **Towny:** integração opcional via API pública; UUID e nome da nação são sincronizados no PDC do jogador e expostos pela interface `NationIdentityProvider`.

## Regras de portabilidade

1. Cada domínio será implementado como módulo oficial separado e poderá ser desativado em `config.yml`.
2. Addons externos continuarão usando a API pública do MECHANIUM.
3. A compatibilidade será validada por testes, licença, dependências e comportamento; uma entrada nesta matriz não equivale a uma implementação completa.
4. Conteúdo fantástico/futurista permanece opcional e desativado por padrão.
5. Sistemas de combate, armas, ataques, mísseis e interferência não fazem parte da implementação operacional.
