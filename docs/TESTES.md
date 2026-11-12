# Testes e validação

## Build automatizado

Execução: [GitHub Actions 43152407542](https://github.com/davitremea1-ops/MECHANIUM/actions/runs/43152407542)

- Java: Temurin 21
- Build: Maven `clean verify`
- Resultado: `BUILD SUCCESS`
- Testes: 8 executados, 0 falhas, 0 erros.

### Detalhamento

| Módulo | Testes | Escopo |
|---|---:|---|
| `mechanium-api` | 3 | Normalização de IDs, custo inválido e dependência circular |
| `mechanium-storage` | 3 | Persistência após reabertura, débito atômico, saldo insuficiente e grants negativos |
| `mechanium-engine` | 2 | Bloqueio por era, pré-requisitos, pontos e rejeição de era desconhecida |

## Artefato

O build gerou o JAR em `mechanium-plugin/target/MECHANIUM-0.1.0-SNAPSHOT.jar`. O passo de upload do workflow não conseguiu armazená-lo porque a cota de artefatos do GitHub Actions estava esgotada. Isso não alterou o resultado de Maven; significa apenas que o run não oferece download do JAR.

## Ainda não testado

- Inicialização em Paper 1.21.11.
- Inicialização em Purpur 1.21.11.
- Registro de itens e receitas no jogo.
- Operação de máquinas e redes.
- Carregamento de addon externo real.
- Persistência após reinício de servidor real.
- Compatibilidade funcional dos projetos listados em PORTABILIDADE_ADDONS.md.

Não marcar esses itens como compatíveis até a execução dos testes correspondentes.
