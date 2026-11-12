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

## Módulos opcionais

| ID | Conteúdo | Padrão |
|---|---|---|
| `optional-fantasy` | Alquimia fictícia e tecnologias arcanas | Desativado |
| `optional-futuristic` | Tecnologias especulativas de futuro | Desativado |

Cada módulo opcional possui classe, IDs, era e pesquisas próprios. Não altera a progressão histórica padrão. Administradores podem ativá-los explicitamente em `plugins/MECHANIUM/config.yml`.

## Próximos módulos candidatos

- Redes de fluidos e transporte de itens.
- Máquinas avançadas e linhas de produção.
- Clima, poluição e aquecimento global.
- Armazenamento e redes de depósitos.
- Culinária e processamento de alimentos.
- Integração visual Geyser/Bedrock.

O conteúdo relacionado a armas, ataques ou sistemas de combate operacional não faz parte do escopo. Sistemas abstratos de diplomacia, orçamento, defesa civil e logística podem ser tratados sem detalhes operacionais.
