# Estado real do projeto

**Versão:** 0.1.0-SNAPSHOT  
**Plataforma-alvo:** Minecraft 1.21.11, Java 21, Paper/Purpur  
**Distribuição planejada:** um único MECHANIUM.jar

## Implementado na fundação
- API pública versionada para progressão, módulos e addons.
- Modelos de era e tecnologia com validação de IDs.
- Persistência SQLite com schema inicial, UUIDs, WAL e transação atômica de pesquisa.
- Motor inicial de eras, pontos científicos, pré-requisitos e autorização tecnológica.
- Módulos internos: indústria, agricultura, energia, armazenamento e ambiente.
- Módulos opcionais de fantasia e futuro, desativados por padrão.
- Carregador de addons externos com descriptor, dependências, ordem de carregamento e ciclo de vida.
- Bootstrap Bukkit, comandos iniciais e configuração.

## Ainda não implementado
- Registro completo de itens Minecraft e receitas.
- Máquinas, automação, redes de energia e fluidos.
- Livro/guia e interfaces gráficas.
- Migração real dos dados de Slimefun ou Historical Ages.
- Portabilidade funcional dos projetos de referência.
- Publicação da API em registry Maven remoto.
- Testes em servidor Paper/Purpur e gameplay real.

## Validação atual
- GitHub Actions: [run 43152407542](https://github.com/davitremea1-ops/MECHANIUM/actions/runs/43152407542) — **BUILD SUCCESS**.
- Testes unitários: **8 executados, 0 falhas, 0 erros** (3 API, 3 SQLite, 2 motor de progressão).
- O workflow tentou anexar o JAR, mas a cota de armazenamento de artefatos do GitHub Actions estava esgotada. O upload é não bloqueante; portanto, o build passou, mas este run não disponibilizou artefato para download.
- Ainda **não realizado**: inicialização em Paper/Purpur 1.21.11, validação de addons reais e testes de gameplay.

## Classificação
**Fundação compilável; não pronta para produção.** A aprovação Maven não substitui smoke test nem validação funcional em servidor.
