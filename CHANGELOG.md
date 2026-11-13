# Changelog

## 0.3.0-SNAPSHOT
- Ampliação funcional dos sete domínios integrados na versão anterior.
- EcoPower: geradores eólicos/solares, baterias persistentes, geração por tick e consumo de energia por máquinas industriais.
- Liquid: itens de tubulação e tanque; rede local de transporte de água purificada entre tanques do mesmo proprietário.
- SFCalc: cálculo de lotes e materiais com expansão recursiva de receitas intermediárias.
- AdvancedTech: britador e refinaria industrial, além da montadora já existente; máquinas avançadas condicionadas à energia.
- Simple Storage: inventário virtual paginado para até 64 nós de armazenamento adjacentes, com controle de propriedade e acesso.
- ExtraGear: habilidades de picareta reforçada e bússola de levantamento.
- SensibleToolbox: habilidades de kit de campo e kit consumível de manutenção.
- Atualização da documentação e versão de todos os módulos Maven para 0.3.0-SNAPSHOT.
- CI Java 21/Maven aprovado no run 43352161651: 52 testes automatizados sem falhas ou erros.
- JAR `MECHANIUM-0.3.0-SNAPSHOT.jar` publicado no GitHub Pre-release; smoke test em Paper/Purpur 1.21.11 ainda pendente.

## 0.2.0-SNAPSHOT
- Integração inicial independente de sete referências: ExtraGear, EcoPower, SFCalc, Liquid, AdvancedTech, Simple Storage e SensibleToolbox.
- Novas tecnologias, categorias, itens e receitas distribuídos por eras e protegidos pelo sistema central de pesquisas.
- Segundo processo funcional de máquina: montadora industrial para peças de precisão.
- Primeiro processamento de fluidos: filtro de água com entrada de balde e saída de item personalizado.
- Nó adicional de armazenamento protegido, com persistência do tipo de armazém para recuperação correta ao quebrar.
- Comando `/mechanium calc <id-da-receita>` para consultar receitas e requisitos.
- CI Java 21 com 49 testes automatizados sem falhas ou erros no run 43349904209.

## 0.1.0-SNAPSHOT
- Fundação independente MECHANIUM.
- Maven multi-módulo para Java 21/Paper 1.21.11.
- API pública versão 1.
- Persistência SQLite versionada até o schema 4, incluindo ownership de conteúdo.
- Registro persistente de categorias, itens, receitas, máquinas e regras de drops.
- Módulos industriais, agricultura, geologia, armazenamento, ambiente e energia.
- Primeira prensa mecânica funcional e drops agrícolas condicionados a pesquisa.
- Ferramenta de prospecção geológica com scanner local e cooldown.
- Armazenamento privado com permissões por UUID.
- Carregador de addons externos com dependências e ciclo de vida.
- Exemplo de addon Maven.
- Bootstrap Bukkit, menu de pesquisas, comandos básicos e workflow de build.
