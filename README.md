# MECHANIUM

**MECHANIUM** é um plugin independente para Minecraft, voltado a tecnologia, progressão histórica, indústria, automação e geopolítica.

- **Minecraft:** 1.21.11
- **Java:** 21
- **Servidores-alvo:** Paper e Purpur
- **Distribuição:** um único `MECHANIUM.jar`
- **Módulos oficiais:** incorporados ao JAR e ativados por configuração
- **Addons externos:** suportados por uma API pública versionada
- **Dependências:** nenhuma dependência obrigatória de Slimefun ou Historical Ages

## Decisões de arquitetura

1. MECHANIUM não carrega Slimefun nem Historical Ages como dependências.
2. Conceitos e funcionalidades desses projetos serão reimplementados em APIs próprias; código upstream só será portado após auditoria de licença e atribuição.
3. Módulos históricos/industriais formam o conjunto padrão.
4. Fantasia e ficção futurista ficam em módulos internos separados, desativados por padrão.
5. Addons comunitários são JARs externos carregados por `plugins/MECHANIUM/addons/`.
6. A progressão é consultada pelo serviço central. Módulos não devem criar listas duplicadas de eras nem contornar o motor.

## Estrutura Maven

| Módulo | Responsabilidade |
|---|---|
| `mechanium-api` | API pública para módulos e addons |
| `mechanium-storage` | Persistência SQLite e migrações |
| `mechanium-engine` | Eras, pesquisas e autorização tecnológica |
| `mechanium-modules` | Módulos oficiais, inclusive opcionais |
| `mechanium-addon-loader` | Descoberta, dependências e ciclo de vida de addons externos |
| `mechanium-plugin` | Bootstrap Bukkit e montagem do JAR único |

## Compilar

Requer JDK 21 e Maven 3.9+:

```bash
mvn clean verify
```

JAR principal:

```text
mechanium-plugin/target/MECHANIUM-0.1.0-SNAPSHOT.jar
```

## Instalar

1. Faça backup do servidor.
2. Copie o JAR MECHANIUM para `plugins/`.
3. Inicie Paper/Purpur 1.21.11.
4. Ajuste `plugins/MECHANIUM/config.yml`.
5. Coloque addons externos em `plugins/MECHANIUM/addons/` e reinicie.

A fundação ainda está em desenvolvimento e não substitui todas as funcionalidades do Slimefun ou dos addons neste estágio. Consulte [STATUS](docs/STATUS.md) e [ROADMAP](docs/ROADMAP.md).

## CI e estado

- [Build Java 21 / Maven verify](https://github.com/davitremea1-ops/MECHANIUM/actions/runs/43221237866) — **BUILD SUCCESS**, 17 testes aprovados na revisão do commit `ce908e5`.
- O JAR é empacotado pelo Maven Shade, mas o upload de artefatos do GitHub Actions não está disponível enquanto a quota de armazenamento permanecer esgotada.
- A base ainda não foi validada em servidor real e não é uma versão de produção.

## Documentação

- [Arquitetura](docs/ARQUITETURA.md)
- [Desenvolvimento de addons externos](docs/ADDONS_EXTERNOS.md)
- [Módulos oficiais e opcionais](docs/MODULOS.md)
- [Matriz de portabilidade](docs/PORTABILIDADE_ADDONS.md)
- [Licenças e atribuição](docs/LICENCAS_E_ORIGENS.md)
- [Roadmap](docs/ROADMAP.md)
- [Contribuição](CONTRIBUTING.md)
