# Testes e validação

## Build automatizado

Execução: [GitHub Actions 43323981115](https://github.com/davitremea1-ops/MECHANIUM/actions/runs/43323981115)

- Java: Temurin 21
- Build principal: Maven `clean verify`
- Resultado: **BUILD SUCCESS**
- Testes automatizados: **37 executados, 0 falhas, 0 erros**
- API pública instalada no repositório Maven local para compilar o exemplo de addon.
- Projeto-exemplo de addon compilado e empacotado com sucesso.

### Detalhamento

| Módulo | Testes | Escopo |
|---|---:|---|
| `mechanium-api` | 16 | Validação de itens, receitas, máquinas, drops e tecnologias |
| `mechanium-storage` | 6 | Persistência de jogadores, conteúdo, máquinas e drops após reabertura; débito atômico e limites de saldo |
| `mechanium-engine` | 6 | Progressão, desbloqueios, validação de conteúdo, máquinas e drops |
| `mechanium-addon-loader` | 2 | Classloader e visibilidade de dependências entre addons |
| `mechanium-plugin` | 7 | Política de acesso ao armazenamento privado e agregação de minérios |

## Artefato

O Maven Shade gerou o JAR em `mechanium-plugin/target/MECHANIUM-0.1.0-SNAPSHOT.jar`. O passo de upload do GitHub Actions falhou porque a quota de artefatos do repositório estava esgotada. Como esse passo usa `continue-on-error`, o problema de armazenamento não altera o resultado de `mvn clean verify`; significa apenas que o run não oferece download do JAR.

## Ainda não testado

- Inicialização em Paper 1.21.11.
- Inicialização em Purpur 1.21.11.
- Registro de itens e receitas no jogo.
- Operação de máquinas e redes em servidor real.
- Carregamento de addon externo real no servidor.
- Persistência após reinício de servidor real.
- Compatibilidade funcional dos projetos listados em `PORTABILIDADE_ADDONS.md`.

Não marcar esses itens como compatíveis até a execução dos testes correspondentes.
