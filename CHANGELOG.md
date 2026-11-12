# Changelog

## 0.1.0-SNAPSHOT
- Fundação independente MECHANIUM.
- Maven multi-módulo para Java 21/Paper 1.21.11.
- API pública versão 1.
- Persistência SQLite versionada até o schema 3.
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
