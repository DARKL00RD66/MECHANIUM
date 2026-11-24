# Changelog

## 0.7.4.00-SNAPSHOT
- Adicionada a primeira implementação nativa de equivalência EMC: saldo por jogador, catálogo de itens aprendidos, dissolução e síntese.
- Criadas cinco tiers de Dissolvedor e cinco tiers de Materializador, com desbloqueios por pesquisa e limites crescentes de valor por item.
- Adicionados ledger SQLite separado, comando `/mechanium emc`, configuração de valores EMC e receitas para as dez estações.
- Itens sem valor explicitamente configurado são recusados; a síntese só oferece itens aprendidos pelo próprio jogador.
- Implementação funcional inicial e independente, sem copiar código ou recursos do upstream EMCTech. Integração com rede de energia, automação logística e PlaceholderAPI permanecem pendentes.
- Build Maven/CI e testes Paper/Purpur 1.21.11 necessários para validar a release.


## 0.7.3.01-SNAPSHOT
- Reorganizado o Guia do MECHANIUM: menu principal com Árvore de progressão, Eras históricas, Crafting e Wiki.
- Árvore de progressão abre diretamente na era efetiva do jogador; Eras históricas apresenta relógios selecionáveis e marca a era atual com uma Estrela do Nether acima do relógio correspondente. A seleção altera apenas a visualização da árvore, não a era/progressão do jogador.
- Wiki separada e somente de consulta, com seções paginadas para itens, receitas e máquinas e telas detalhadas de composição, requisitos, entradas, saídas e tempo de processo.
- Crafting permanece separado e conserva a fabricação com verificação e consumo de ingredientes.
- Atualização incremental de interface; build CI e verificação em servidor Paper/Purpur 1.21.11 devem confirmar esta revisão.


## 0.7.3.01-SNAPSHOT
- Adicionados cinco módulos nativos opcionais: FN Amplifications, Geyser Heads (catálogo cosmético local), Slime Customizer, Industrial Chemistry e Mining Automation.
- Cada módulo registra duas tecnologias, uma categoria, quatro itens e quatro receitas; FN Amplifications, Industrial Chemistry e Mining Automation também declaram máquinas no runtime genérico.
- Os módulos foram registrados no catálogo oficial, nos testes de presença e na configuração padrão.
- Geyser Heads ainda não implementa sincronização de skins nem integração específica com Geyser/Bedrock; Slime Customizer ainda não possui editor administrativo visual completo.
- Atualizado o reactor Maven para 0.7.3.01-SNAPSHOT. Build/CI e validação em Paper/Purpur 1.21.11 ainda precisam concluir.


## 0.7.2.01-SNAPSHOT
- Incremento de manutenção após 0.7.2.00: incorpora a auditoria de consistência, correções de dependências entre módulos, IDs exclusivos para Water Treatment e correção de inserção em pilhas dos Material Generators.
- Mantidos os cinco módulos adicionados em 0.7.2.00: Aquaculture, Glassworks, Rail Freight, Battery Industry e Cold Chain.
- Atualizados todos os oito POMs do reactor e o exemplo de addon para `0.7.2.01-SNAPSHOT`.
- Build Maven/CI desta versão em andamento; validação de gameplay em Paper/Purpur 1.21.11 permanece pendente.


## 0.7.2.00-SNAPSHOT
- Auditoria de consistência corrigiu dependências de tecnologias entre módulos (Culinary, Air Filtration, Metallurgy, Pressure Engineering, Automation e Logistics), e explicitou a dependência Energy no Battery Industry.
- Corrigida colisão de IDs com o módulo Liquid: o cartucho de tratamento de água agora usa `water_filter_cartridge` e `water_filter_cartridge_recipe`.
- Corrigida a produção dos geradores de materiais para permitir completar pilhas existentes em baús sem espaços vazios.
- Adicionados testes de regressão para pré-requisitos tecnológicos entre módulos, IDs duplicados de tecnologias e dependências opcionais.
- Adicionados cinco módulos oficiais: `aquaculture`, `glassworks`, `rail-freight`, `battery-industry` e `cold-chain`.
- Aquaculture: ração, redes de criação, reaproveitamento de subprodutos, caixotes e incubadora aquícola.
- Glassworks: carga de sílica, vidro industrial, painéis temperados, lentes de precisão e forno de vidro.
- Rail Freight: dormentes, trilhos reforçados, paletes e manifestos para operações de carga e transporte intermodal.
- Battery Industry: separadores, invólucros, células montadas e bancos de baterias industriais.
- Cold Chain: fibras isolantes, células de refrigeração, caixotes térmicos e unidade refrigeradora.
- Os cinco módulos adicionam 5 categorias, 10 tecnologias, 22 itens e 22 receitas; todos foram registrados no catálogo, nos testes e na configuração padrão.
- As definições de máquinas aquícola, forno de vidro e refrigeração usam o runtime genérico de uma entrada/uma saída; testes em servidor real continuam pendentes.
- Versão Maven avançada para `0.7.2.00-SNAPSHOT` no reactor e no exemplo de addon.

## 0.7.1.00-SNAPSHOT
- Adicionados quatro módulos oficiais opcionais: `water-treatment`, `greenhouse`, `industrial-recycling` e `ore-chunks`.
- Water Treatment: cartuchos filtrantes, células de água purificada, fluido refrigerante e recuperação hídrica, com progressão de filtragem e recirculação.
- Greenhouse: painéis de estufa, solução nutritiva, bandejas hidropônicas e controle climático; depende dos módulos Agriculture e Water Treatment.
- Industrial Recycling: sucata mista, separação de ferro/cobre, polímero abstrato e módulo de triagem para cadeia de economia circular.
- Ore Chunks: fragmentos de ferro, cobre e ouro e lotes compactados, integrados à Geologia e ao Beneficiamento Mineral.
- Os quatro módulos possuem receitas e tecnologias próprias, são registrados no catálogo e podem ser desativados individualmente no `config.yml`.
- Versão Maven avançada para `0.7.1.00-SNAPSHOT` em todos os módulos e no exemplo de addon.

## 0.7.0.01-SNAPSHOT
- Adicionado o módulo opcional `material-generators`, reimplementação independente do domínio SimpleMaterialGenerators (SMG).
- Oito geradores passivos produzem pedregulho, pedra, cascalho, areia, pedra lisa, vidro, netherrack e areia das almas em um baú colocado diretamente acima.
- Produção padrão de um item a cada 200 ticks, configurável em `modules.material-generators.interval-ticks`; progresso fica persistido no bloco e pausa quando não há baú ou espaço.
- Geradores possuem proprietário, bloqueio contra desmontagem por terceiros, proteção contra explosões/pistões e recuperação do item ao quebrar.
- Geração básica exige `basic_material_generation`; materiais avançados exigem `advanced_material_generation`. Módulo pode ser desativado em `config.yml`.

## 0.7.0.01-SNAPSHOT
- Adicionado o módulo opcional better-reactor com gerador cristalino abstrato, sem simulação de engenharia nuclear real.
- O gerador usa cristais fictícios como combustível: uma unidade mantém a produção de 8 unidades de energia por segundo durante cinco minutos, carregando baterias MECHANIUM adjacentes.
- Adicionado o módulo opcional industrial-networks: conduítes e caixas protegidas para transporte automático de itens MECHANIUM.
- Redes conectam até 128 conduítes adjacentes do mesmo proprietário; transferem um item personalizado por ciclo entre caixas conectadas, apenas em chunks carregados.
- Caixas preservam o conteúdo ao serem desmontadas; a rede não transporta itens vanilla nem conecta depósitos à distância.
- Adicionados quatro módulos oficiais opcionais: `forestry`, `textile-industry`, `biofuel` e `construction-materials`.
- Forestry: fibras de madeira, resina vegetal, tábuas tratadas e estação florestal.
- Textile Industry: fibras, tecidos industriais, tecido reforçado e tear mecanizado.
- Biofuel: biomassa e células de biocombustível abstratas para progressão dentro do jogo.
- Construction Materials: cerâmica, painéis reforçados, estruturas pré-fabricadas e misturador industrial.
- Cada módulo possui categoria, tecnologias, receitas e máquinas quando aplicável; ativação individual por `config.yml`.


## 0.6.0-SNAPSHOT
- Adicionados cinco módulos oficiais: `air-filtration`, `advanced-metallurgy`, `pressure-engineering`, `industrial-automation` e `industrial-logistics`.
- Novas categorias, tecnologias, itens e receitas para purificador de ar (inclui balde de água), componentes metalúrgicos, câmara de pressão, circuitos de controle, montadora e roteador de carga.
- Os cinco módulos são ativados por padrão e podem ser desativados individualmente em `config.yml`.
- Novo catálogo de fabricação por clique: mostra itens e máquinas por categoria, requisitos, tecnologia e ingredientes; valida o inventário, consome os materiais e entrega o resultado sem abrir a bancada. Máquinas também exibem seus processos e continuam instaláveis como blocos dispenser.


## 0.5.0-SNAPSHOT
- Adicionados os módulos opcionais `electric-spawners`, `fast-machines` e `hardcore-slimefun`.
- ElectricSpawners: incubadoras elétricas de galinha, vaca, porco e ovelha; usam sementes, feno, cenouras ou trigo e geram spawn eggs vanilla após processamento e consumo de energia.
- FastMachines: prensa de alto rendimento e enrolador rápido de bobinas, com ciclos de 20 e 40 ticks, respectivamente, e alimentação pela rede elétrica.
- HardcoreSlimefun: pesquisas de confiabilidade e manutenção, kit reparador desbloqueável e configuração opcional de desgaste/custo de reparo mais rigorosa.
- A configuração mantém o modo hardcore desativado por padrão; os três módulos ficam habilitados por padrão e podem ser desligados individualmente.
- A linha Maven avança para 0.5.0-SNAPSHOT por incluir um novo conjunto de integrações.

## 0.4.1-SNAPSHOT
- Animação de abertura dos menus mais cadenciada: moldura revelada em grupos de 3 itens e conteúdo em grupos de 2, com intervalo de 2 ticks por etapa.
- Pesquisas agora possuem tempo individual; duração padrão calculada pelo custo científico e pela era, com mínimo obrigatório de 120 segundos.
- Sobrescritas por tecnologia em `progression.research.overrides`, mantendo o mínimo de dois minutos.
- Pesquisas em andamento persistidas no SQLite (schema 6); o prazo continua correndo enquanto o jogador está offline e após reinicializações.
- Pontos científicos são debitados ao iniciar a pesquisa; a tecnologia só é desbloqueada quando o prazo termina.
- Menu mostra estado, porcentagem, barra de progresso, duração estimada e tempo restante; contador atualiza sem reabrir a tela.
- Comando `/mechanium unlock` passa a iniciar a pesquisa temporizada; método de desbloqueio instantâneo da API é preservado para compatibilidade.
- Testes para persistência após reinicialização, conclusão após tempo offline, débito atômico e cálculo de duração.
- Versão patch avançada para 0.4.1-SNAPSHOT.

## 0.4.0-SNAPSHOT
- Catálogo vanilla ampliado por descoberta automática do registro de receitas do Paper 1.21.11, com paginação no painel administrativo.
- Novas receitas suportadas recebem uma era histórica sugerida automaticamente; regras existentes e alterações administrativas são preservadas.
- Bloqueios aplicados a crafting, livro de receitas, autocrafter, processamento em fornalhas, corte de pedra e ferraria.
- Correção da receita da mesa de encantamentos para a Era Mítica, com migração automática do valor padrão antigo em instalações existentes.
- Primeiro módulo ExoticGarden: mistura de sementes, ervas desidratadas, extrato botânico, conserva tropical e caixote de pomar.\n- Primeira expansão DankTech2: upgrade de armazém portátil de 27 para 54 espaços, com conteúdo persistido no próprio item.
- Novos testes unitários para a classificação histórica de receitas e inclusão do ExoticGarden no catálogo de módulos.
- Versionamento Maven avançado para 0.4.0-SNAPSHOT. CI e smoke test em Paper/Purpur 1.21.11 ainda pendentes.

## 0.3.0-SNAPSHOT
- Nova era global persistente no SQLite, aplicada a jogadores online e offline que não tenham sobrescrita individual de teste.
- Painel administrativo com botão separado para trocar a era de todo o servidor; seleção individual permanece disponível para testes e agora inclui retorno à era global.
- Anúncio de mudança global com título “Nova era”, nome da era como subtítulo e som de conclusão de desafio; jogadores offline recebem o aviso no próximo login.
- Animação de revelação nos menus de pesquisa, administração e edição de receitas: moldura e controles aparecem em sequência com sons de interface.
- Migração SQLite para schema 5 com suporte a era global e sobrescritas individuais.
- Teste automatizado para persistência da era global, herança por jogadores e sobrescritas de teste.
- Ampliação funcional dos sete domínios integrados na versão anterior.
- EcoPower: geradores eólicos/solares, baterias persistentes, geração por tick e consumo de energia por máquinas industriais.
- Liquid: itens de tubulação e tanque; rede local de transporte de água purificada entre tanques do mesmo proprietário.
- SFCalc: cálculo de lotes e materiais com expansão recursiva de receitas intermediárias.
- AdvancedTech: britador e refinaria industrial, além da montadora já existente; máquinas avançadas condicionadas à energia.
- Simple Storage: inventário virtual paginado para até 64 nós de armazenamento adjacentes, com controle de propriedade e acesso.
- ExtraGear: habilidades de picareta reforçada e bússola de levantamento.
- SensibleToolbox: habilidades de kit de campo e kit consumível de manutenção.
- Atualização da documentação e versão de todos os módulos Maven para 0.3.0-SNAPSHOT.
- CI Java 21/Maven aprovado no run 43352161651: 52 testes automatizados sem falhas ou erros.
- JAR `MECHANIUM-0.3.0-SNAPSHOT.jar` publicado no GitHub Pre-release; smoke test em Paper/Purpur 1.21.11 ainda pendente.

## 0.2.0-SNAPSHOT
- Integração inicial independente de sete referências: ExtraGear, EcoPower, SFCalc, Liquid, AdvancedTech, Simple Storage e SensibleToolbox.
- Novas tecnologias, categorias, itens e receitas distribuídos por eras e protegidos pelo sistema central de pesquisas.
- Segundo processo funcional de máquina: montadora industrial para peças de precisão.
- Primeiro processamento de fluidos: filtro de água com entrada de balde e saída de item personalizado.
- Nó adicional de armazenamento protegido, com persistência do tipo de armazém para recuperação correta ao quebrar.
- Comando `/mechanium calc <id-da-receita>` para consultar receitas e requisitos.
- CI Java 21 com 49 testes automatizados sem falhas ou erros no run 43349904209.

## 0.1.0-SNAPSHOT
- Fundação independente MECHANIUM.
- Maven multi-módulo para Java 21/Paper 1.21.11.
- API pública versão 1.
- Persistência SQLite versionada até o schema 4, incluindo ownership de conteúdo.
- Registro persistente de categorias, itens, receitas, máquinas e regras de drops.
- Módulos industriais, agricultura, geologia, armazenamento, ambiente e energia.
- Primeira prensa mecânica funcional e drops agrícolas condicionados a pesquisa.
- Ferramenta de prospecção geológica com scanner local e cooldown.
- Armazenamento privado com permissões por UUID.
- Carregador de addons externos com dependências e ciclo de vida.
- Exemplo de addon Maven.
- Bootstrap Bukkit, menu de pesquisas, comandos básicos e workflow de build.
