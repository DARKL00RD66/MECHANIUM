# Changelog

## 0.6.0-SNAPSHOT
- Adicionados cinco módulos oficiais: `air-filtration`, `advanced-metallurgy`, `pressure-engineering`, `industrial-automation` e `industrial-logistics`.
- Novas categorias, tecnologias, itens e receitas para purificador de ar (inclui balde de água), componentes metalúrgicos, câmara de pressão, circuitos de controle, montadora e roteador de carga.
- Os cinco módulos são ativados por padrão e podem ser desativados individualmente em `config.yml`.
- Catálogo de fabricação por clique, pontos de pesquisa ampliados e revisão da classificação de itens/máquinas ainda estão em desenvolvimento nesta linha.


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
