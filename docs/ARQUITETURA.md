# Arquitetura do MECHANIUM

## Objetivo

Um plugin Bukkit independente, distribuído em um único JAR, com fronteiras internas de módulos. Não existe dependência de runtime do Slimefun ou do Historical Ages.

## Camadas

```text
Paper / Purpur 1.21.11
        |
MECHANIUM Plugin Bootstrap
        |
Public API + Service Registry
        |
Progression Engine ---- Persistence (SQLite)
        |
Official Module Manager
  |-- Industry
  |-- Geology
  |-- Agriculture
  |-- Energy
  |-- Storage
  |-- Environment
  |-- Optional Fantasy (off by default)
  |-- Optional Futuristic (off by default)
        |
External Addon Loader
        |
plugins/MECHANIUM/addons/*.jar
```

## Módulos Maven

Cada subprojeto tem build e responsabilidade próprios. O módulo final depende deles e usa Maven Shade para produzir um único JAR. A API é incluída no JAR principal e também gera um artefato de desenvolvimento para addons externos.

## Regras de integração

- Itens, receitas, máquinas e processos devem usar IDs estáveis.
- Conteúdo tecnológico deve apontar para uma era e, quando necessário, pré-requisitos de pesquisa.
- Conteúdo sem mapeamento não recebe liberação automática.
- A autorização deve ocorrer no serviço central, não apenas na GUI.
- Dados de jogador são identificados por UUID, não por nome.
- Débito de pontos e registro de pesquisa são atômicos no SQLite.
- Módulos opcionais não podem alterar nem desbloquear o caminho histórico padrão.
- Addons externos declaram versão mínima da API e dependências; ciclos e dependências ausentes impedem seu carregamento.
- Módulos internos declaram dependências e são ativados conforme `config.yml`; módulos que exigem outro módulo não iniciam se a dependência estiver desativada.

## Persistência e migrações

SQLite usa WAL, foreign keys e tabela de versão de schema. Migrações futuras devem ser numeradas e transacionais. Antes de importar dados de outro plugin, MECHANIUM deverá criar backup, verificar UUIDs e apresentar relatório de conflitos; nunca apagar bancos de origem automaticamente.

## Compatibilidade

O alvo inicial é Minecraft 1.21.11, Java 21, Paper e Purpur. A API interna evita NMS sempre que Bukkit/Paper fornece contrato público.
