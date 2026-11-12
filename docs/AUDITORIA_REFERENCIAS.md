# Auditoria preliminar de origem e licenças

**Data da verificação:** 12/11/2026  
**Escopo:** localizar repositórios candidatos e inspecionar arquivos de licença na raiz.  
**Importante:** esta é uma pré-auditoria. Os repositórios abaixo são candidatos encontrados por nome; antes de portar qualquer implementação, deve-se confirmar que o repositório e o commit correspondem exatamente ao projeto indicado pela referência original do addon.

## Resultado por projeto

| Projeto de referência | Repositório candidato | Arquivo/licença observado | Situação para MECHANIUM |
|---|---|---|---|
| ExoticGarden | [Slimefun-Addon-Community/ExoticGarden](https://github.com/Slimefun-Addon-Community/ExoticGarden) | `LICENSE` — GPL-3.0 | Reimplementar independentemente; não copiar para o JAR sem resolver compatibilidade copyleft |
| ElectricSpawners | [Slimefun-Addon-Community/ElectricSpawners](https://github.com/Slimefun-Addon-Community/ElectricSpawners) | `LICENSE` — GPL-3.0 | Reavaliar o domínio como manejo/produção animal; reimplementar |
| PrivateStorage | [Slimefun-Addon-Community/PrivateStorage](https://github.com/Slimefun-Addon-Community/PrivateStorage) | `LICENSE` — MIT | Reimplementação preferida; reutilização pontual exige preservar copyright e aviso MIT |
| ExtraGear | [TheBusyBiscuit/ExtraGear](https://github.com/TheBusyBiscuit/ExtraGear) | `LICENSE` — GPL-3.0 | Reimplementar independentemente |
| SlimefunOreChunks | [Slimefun-Addon-Community/SlimefunOreChunks](https://github.com/Slimefun-Addon-Community/SlimefunOreChunks) | `LICENSE` — MIT | Reimplementação preferida; preservar aviso se houver reutilização autorizada |
| LiteXpansion | [Slimefun-Addon-Community/LiteXpansion](https://github.com/Slimefun-Addon-Community/LiteXpansion) | `LICENSE` — GPL-3.0 | Reimplementar independentemente |
| EcoPower | [Slimefun-Addon-Community/EcoPower](https://github.com/Slimefun-Addon-Community/EcoPower) | `LICENSE` — MIT | Reimplementação preferida; preservar aviso se houver reutilização autorizada |
| HardcoreSlimefun | [Slimefun-Addon-Community/HardcoreSlimefun](https://github.com/Slimefun-Addon-Community/HardcoreSlimefun) | `LICENSE` — MIT | Reimplementar regras de balanceamento; preservar aviso se reutilizar código |
| SFMobDrops | [WalshyDev/SFMobDrops](https://github.com/WalshyDev/SFMobDrops) | `LICENSE` — MIT | Reimplementação preferida; preservar aviso se houver reutilização autorizada |
| SFCalc | [SlimefunNewHorizons/SFCalc-drake](https://github.com/SlimefunNewHorizons/SFCalc-drake) (fork candidato) | `LICENSE.txt` — GPL-3.0 | Confirmar upstream original; não copiar automaticamente |
| FluffyMachines | [NCBPFluffyBear/FluffyMachines](https://github.com/NCBPFluffyBear/FluffyMachines) | `LICENSE.txt` — GPL-3.0 | Reimplementar independentemente |
| GlobalWarming | [nsporillo/GlobalWarming](https://github.com/nsporillo/GlobalWarming) (candidato) | `LICENSE` — LGPL-3.0 | Confirmar que é a referência original; avaliar separação e obrigações LGPL |
| SlimefunWarfare | [Seggan/SlimefunWarfare](https://github.com/Seggan/SlimefunWarfare) | `LICENSE.txt` — GPL-3.0 | Não portar mecânicas operacionais de combate; somente sistemas civis abstratos reimplementados |
| Liquid | [Seggan/Liquid](https://github.com/Seggan/Liquid) | `LICENSE.txt` — LGPL-3.0 | Reimplementar rede de fluidos; revisão jurídica necessária antes de qualquer reutilização |
| SlimeCustomizer | [NCBPFluffyBear/SlimeCustomizer](https://github.com/NCBPFluffyBear/SlimeCustomizer) | `LICENSE.txt` — GPL-3.0 | Reimplementar ferramentas administrativas, sem incorporar código automaticamente |
| SMG | [wickidcow/SF_SMG](https://github.com/wickidcow/SF_SMG) | `LICENSE` — GPL-3.0 | Confirmar identidade com referência original; reimplementar |
| Simple Storage | [Sefiraat/Simple-Storage](https://github.com/Sefiraat/Simple-Storage) | `LICENSE` — GPL-3.0 | Reimplementar independentemente |
| Galactifun | [Slimefun-Addon-Community/Galactifun](https://github.com/Slimefun-Addon-Community/Galactifun) | `LICENSE` — GPL-3.0 | Somente módulo futurista opcional; reimplementar |
| FNAmplifications | [wickidcow/SF_FNAmplifications](https://github.com/wickidcow/SF_FNAmplifications) | `LICENSE` — GPL-3.0 | Confirmar identidade; reimplementar |
| Element Manipulation | [lucasGithuber/Element-Manipulation](https://github.com/lucasGithuber/Element-Manipulation) | `LICENSE` — GPL-3.0 | Somente módulo de fantasia opcional; reimplementar |
| DankTech2 | [Sefiraat/DankTech2](https://github.com/Sefiraat/DankTech2) | `LICENSE` — GPL-3.0 | Reimplementar armazenamento portátil |
| Networks | [servernotdie/networksexpansion](https://github.com/servernotdie/networksexpansion) (candidato) | `LICENSE` — GPL-3.0 | Confirmar se corresponde ao addon listado; reimplementar |
| MissileWarfare | [koiboi-dev/MissileWarfare](https://github.com/koiboi-dev/MissileWarfare) (candidato) | `LICENSE` — GPL-3.0 | Excluído: não implementar armas, mísseis, ataques ou operação de combate |
| EMCTech | [Sefiraat/EMCTech](https://github.com/Sefiraat/EMCTech) (candidato) | Não foi localizado `LICENSE`, `LICENSE.md`, `LICENSE.txt` ou `COPYING` | Não copiar código; confirmar origem e permissão. Reimplementar somente após delimitar comportamento |
| Gastronomicon | [SchnTgaiSpock/Gastronomicon](https://github.com/SchnTgaiSpock/Gastronomicon) (candidato) | `LICENSE` — GPL-3.0 | Reimplementar culinária e cadeia produtiva |
| Better Nuclear Reactor | [pingguomc/BetterNuclearReactor](https://github.com/pingguomc/BetterNuclearReactor) (candidato) | `LICENSE` — GPL-3.0 | Confirmar identidade; somente sistema energético abstrato e seguro |
| AdvancedTech | [Slimefun-Addon-Community/AdvancedTech](https://github.com/Slimefun-Addon-Community/AdvancedTech) | `LICENSE` — MIT | Reimplementar processos industriais; preservar aviso se reutilizar código |
| FastMachines | [GuizhanCraft/FastMachines](https://github.com/GuizhanCraft/FastMachines) | `LICENSE` — GPL-3.0 | Reimplementar otimização com limites e custos |
| MapJammers | [Intybyte/MapJammers](https://github.com/Intybyte/MapJammers) | `LICENSE.md` — GPL-3.0 | Não portar interferência/jamming; cartografia pode ser reimplementada sem esse mecanismo |
| SensibleToolbox | [Slimefun/SensibleToolbox](https://github.com/Slimefun/SensibleToolbox) | `LICENSE` — GPL-3.0 | Reimplementar apenas ferramentas pertinentes |
| GeyserHeads | [Hahaa13/GeyserHeads](https://github.com/Hahaa13/GeyserHeads) | `LICENSE` — GPL-3.0 | Confirmar identidade e dependências Geyser; reimplementar integração visual |

## Regras de decisão

1. A existência de uma licença MIT não elimina a obrigação de preservar avisos de copyright e o texto da licença.
2. GPL-3.0 e LGPL-3.0 exigem análise de compatibilidade e obrigações de distribuição. O JAR único do MECHANIUM não deve incorporar código derivado desses projetos sem uma decisão explícita sobre a licença e o fornecimento do código correspondente.
3. Repositórios marcados como candidatos ainda precisam ser comparados com as URLs originais de builds fornecidas para o projeto.
4. Ausência de um arquivo de licença nos nomes pesquisados não significa autorização de uso; o código não será copiado sem permissão expressa.
5. Descrições de comportamento podem orientar uma implementação independente, mas não se deve copiar código, estrutura expressiva ou assets sem autorização.
6. Antes de qualquer portabilidade real, registrar commit exato, dependências, assets, avisos e comportamento a ser reimplementado.

## Conclusão

A auditoria preliminar não autoriza a cópia indiscriminada de nenhum addon. Há referências MIT que permitem reutilização sob condições, diversas referências GPL/LGPL que exigem compatibilidade e obrigações específicas, e alguns repositórios candidatos cuja correspondência ainda deve ser confirmada. A implementação do MECHANIUM seguirá preferencialmente por reimplementação independente e modular.
