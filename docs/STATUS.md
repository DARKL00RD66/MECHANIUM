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

## Classificação
**Em desenvolvimento / não pronto para produção** até que CI e smoke tests de servidor sejam aprovados.
