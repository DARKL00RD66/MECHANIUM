# Changelog

## 0.1.0-SNAPSHOT
- Fundação independente MECHANIUM.
- Maven multi-módulo para Java 21/Paper 1.21.11.
- API pública versão 1.
- Persistência SQLite versionada até o schema 4, incluindo ownership de conteúdo.
- Registro persistente de categorias, itens, receitas, máquinas e regras de drops.
- Validação de itens sem exigir inicialização de RegistryAccess do Bukkit; materiais de ar são rejeitados.
- Registro de máquinas rejeita tipos de bloco ainda não suportados pelo runtime.
- Eventos públicos de conteúdo e progressão.
- Módulos industriais, agricultura, geologia, armazenamento, ambiente e energia.
- Primeira prensa mecânica funcional e drops agrícolas condicionados a pesquisa.
- Ferramenta de prospecção geológica com scanner local e cooldown.
- Armazenamento privado com permissões por UUID.
- Carregador de addons externos com dependências e ciclo de vida.
- Exemplo de addon Maven.
- Bootstrap Bukkit, menu de pesquisas, comandos básicos e workflow de build.
- CI Java 21 aprovado no commit `7028e79`: 37 testes automatizados sem falhas.
- Registro transacional e reconciliação de conteúdo por módulo/addon, com rollback em inicialização malsucedida e remoção de definições obsoletas.
- Módulos ordenados automaticamente por dependências; ciclos, referências desconhecidas e IDs duplicados são rejeitados.
- Falhas de leitura/linkage de JARs de addons são isoladas.
- Drops de inventários de armazéns e máquinas passam a ocorrer após confirmação da quebra, evitando duplicação quando outro plugin cancela o evento.
- CI Java 21 aprovado no run `43326632777`: 46 testes automatizados sem falhas ou erros.
