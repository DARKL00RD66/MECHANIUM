# Changelog

## 0.2.0-SNAPSHOT
- Incremento de versão do projeto, módulos Maven e addon de exemplo para `0.2.0-SNAPSHOT`.
- Integração inicial independente de sete referências: ExtraGear, EcoPower, SFCalc, Liquid, AdvancedTech, Simple Storage e SensibleToolbox.
- Novas tecnologias, categorias, itens e receitas distribuídos por eras e protegidos pelo sistema central de pesquisas.
- Segundo processo funcional de máquina: montadora industrial para peças de precisão.
- Primeiro processamento de fluidos: filtro de água com entrada de balde e saída de item personalizado.
- Nó adicional de armazenamento protegido, com persistência do tipo de armazém para recuperação correta ao quebrar.
- Comando `/mechanium calc <id-da-receita>` para consultar receitas e requisitos.
- Documentação atualizada sobre as integrações e os limites ainda pendentes.

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
- CI Java 21 aprovado no run `43327115991`: 47 testes automatizados sem falhas ou erros.
- Migração SQLite v3→v4 limitada a uma única passagem e adoção de ownership apenas para definições legadas existentes.
