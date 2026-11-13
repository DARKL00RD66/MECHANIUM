# Módulos oficiais do MECHANIUM

## Módulos históricos e industriais padrão

| ID | Módulo | Padrão |
|---|---|---|
| `industry` | Máquinas e processos industriais | Ativado |
| `geology` | Prospecção e conhecimento mineral | Ativado |
| `agriculture` | Agricultura e produção alimentar | Ativado |
| `energy` | Geração e distribuição energética | Ativado |
| `storage` | Armazéns e logística de estoque | Ativado |
| `environment` | Gestão ambiental e resíduos | Ativado |

### Geologia

O módulo `geology` depende de `industry` e adiciona a pesquisa `geological_surveying`. Depois de desbloqueá-la, o jogador pode fabricar o `geological_survey_kit` e usá-lo em um bloco para contar minérios em um volume de 9×9×9 ao redor do ponto selecionado. Chunks descarregados são ignorados, evitando geração de terreno durante a varredura.

Esta é uma primeira ferramenta de prospecção local. Depósitos regionais, veios minerais, amostras e integração com mineração continuam pendentes.

## Módulos adicionais integrados

| ID | Referência | Primeiro conteúdo implementado |
|---|---|---|
| `extra-gear` | ExtraGear | Ferramentas e receitas reforçadas; habilidades especiais pendentes |
| `eco-power` | EcoPower | Componentes e pesquisas eólicas/solares; rede elétrica e geração pendentes |
| `sfcalc` | SFCalc | Consulta de receitas por `/mechanium calc <id>` |
| `liquid` | Liquid | Cartuchos e filtro de água funcional usando o runtime de máquinas |
| `advanced-tech` | AdvancedTech | Peças de precisão e montadora industrial |
| `simple-storage` | Simple Storage | Nó adicional de armazenamento protegido por UUID |
| `sensible-toolbox` | SensibleToolbox | Kits e manuais utilitários com receitas próprias |

Todos são módulos opcionais no código e estão ativados por padrão nesta configuração. São primeiras integrações independentes, não paridade completa com os addons de origem.

### Limites desta etapa

- `eco-power` registra componentes e progressão; não há rede de energia ou geração contínua.
- `liquid` implementa uma máquina de filtragem de água, mas ainda não possui tubulações nem redes gerais de fluidos.
- `simple-storage` cria outro armazém protegido; não conecta inventários em uma rede única.
- Os itens de `extra-gear` e `sensible-toolbox` usam atributos vanilla; habilidades especiais ainda exigem implementação adicional.

## Módulos opcionais

| ID | Conteúdo | Padrão |
|---|---|---|
| `optional-fantasy` | Alquimia fictícia e tecnologias arcanas | Desativado |
| `optional-futuristic` | Tecnologias especulativas de futuro | Desativado |

Cada módulo opcional possui classe, IDs, era e pesquisas próprios. Não altera a progressão histórica padrão. Administradores podem ativá-los explicitamente em `plugins/MECHANIUM/config.yml`.

## Próximos módulos candidatos

- Rede elétrica real com geração, armazenamento e consumo por máquina.
- Redes de fluidos e transporte de itens.
- Linhas de produção com filas, energia e manutenção.
- Clima, poluição e aquecimento global.
- Armazenamento conectado entre múltiplos depósitos.
- Integração visual Geyser/Bedrock.

O conteúdo relacionado a armas, ataques ou sistemas de combate operacional não faz parte do escopo. Sistemas abstratos de diplomacia, orçamento, defesa civil e logística podem ser tratados sem detalhes operacionais.
