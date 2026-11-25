# MECHANIUM

![Logo oficial do MECHANIUM](assets/mechanium-logo.png)

**MECHANIUM** é um plugin independente para Minecraft focado em progressão histórica, tecnologia, indústria, automação e geopolítica.

- **Versão em consolidação:** `0.9.9.00-SNAPSHOT`
- **Minecraft:** 1.21.11
- **Java:** 21
- **Plataformas-alvo:** Paper e Purpur
- **Distribuição:** um único `MECHANIUM.jar`
- **Módulos oficiais:** incorporados ao JAR e configuráveis
- **Addons externos:** API pública versionada e carregador próprio
- **Geyser/Floodgate/Bedrock:** fora do escopo; não são dependências e não há integração Bedrock.

## Arquitetura

1. O MECHANIUM não depende de Slimefun, Historical Ages, Geyser ou Floodgate.
2. A progressão é centralizada na API e no motor de eras, tecnologias e pesquisas.
3. Os módulos históricos e industriais formam o conjunto principal. Conteúdo de fantasia/futurista é opcional.
4. Addons comunitários são JARs externos carregados de `plugins/MECHANIUM/addons/`.
5. Código de terceiros só pode ser incorporado após auditoria de licença, origem e atribuição.
6. Head Collection é exclusivamente cosmético/local; não usa API ou serviço de plataforma Bedrock.

## Estrutura Maven

| Módulo | Responsabilidade |
|---|---|
| `mechanium-api` | API pública para módulos e addons |
| `mechanium-storage` | Persistência SQLite e migrações |
| `mechanium-engine` | Eras, pesquisas e autorização tecnológica |
| `mechanium-modules` | Módulos oficiais |
| `mechanium-addon-loader` | Descoberta, dependências e ciclo de vida de addons |
| `mechanium-plugin` | Bootstrap Bukkit/Paper e montagem do JAR |

## Compilar

Requer JDK 21 e Maven 3.9+:

```bash
mvn clean verify
```

JAR principal:

```text
mechanium-plugin/target/MECHANIUM-0.9.9.00-SNAPSHOT.jar
```

## Instalar

1. Faça backup do servidor e do banco SQLite.
2. Copie o JAR MECHANIUM para `plugins/`.
3. Inicie Paper ou Purpur 1.21.11.
4. Revise `plugins/MECHANIUM/config.yml`.
5. Coloque addons compatíveis em `plugins/MECHANIUM/addons/` e reinicie.

## Estado da consolidação 0.9.9

A implementação já inclui máquinas multi-entrada/multi-saída, agendamento round-robin, redes de energia por conduítes, fluidos tipados, filtros e transferência em lote na logística, EMC individual, editor inicial de conteúdo e módulos industriais expandidos.

O CI Java 21/Maven do commit atual passou e gerou o artefato `MECHANIUM-0.9.9.00-SNAPSHOT`. Isso confirma compilação e testes automatizados, mas não substitui smoke tests em servidores Paper e Purpur reais. A validação de reinícios, chunks, propriedade, permissões e migrações em ambiente de jogo continua pendente. Não considerar a versão pronta para produção até concluir essa etapa.

## Documentação

- [Wiki em português](docs/wiki/README.md)
- [Auditoria da distribuição histórica](docs/wiki/DISTRIBUICAO-HISTORICA.md)
- [Estado real do projeto](docs/STATUS.md)
- [Plano de consolidação 0.9.9](docs/PLANO-0.9.9.md)
- [Roadmap](docs/ROADMAP.md)
- [Arquitetura](docs/ARQUITETURA.md)
- [Comandos e administração](docs/ADMINISTRACAO.md)
- [Desenvolvimento de addons](docs/ADDONS_EXTERNOS.md)
- [Módulos oficiais](docs/MODULOS.md)
- [Equivalência EMC](docs/EMC_EXCHANGE.md)
- [Licenças e atribuição](docs/LICENCAS_E_ORIGENS.md)
- [Política de versionamento](docs/VERSIONAMENTO.md)
- [Projeto-exemplo de addon](examples/mechanium-addon-example/README.md)

## Licença

O MECHANIUM pode ser instalado, executado e configurado em servidores. Modificações, obras derivadas e redistribuição de versões alteradas exigem autorização prévia e por escrito do titular. A redistribuição gratuita de uma versão oficial sem modificações é permitida, desde que os avisos de autoria e a licença sejam preservados.

Leia [LICENSE.md](LICENSE.md). Esta é uma licença proprietária personalizada, não uma licença open source.
