# Política de versionamento do MECHANIUM

## Alvo atual

A linha de desenvolvimento foi consolidada diretamente em **`0.9.9.00-SNAPSHOT`**, partindo da base auditada `0.7.4.02-SNAPSHOT`.

As etapas internas equivalentes a 0.8.x e 0.9.x serão executadas dentro desta mesma linha de desenvolvimento. Não é necessário publicar ou criar tags para cada marco intermediário. Isso permite avançar para o pré-lançamento 0.9.9 sem declarar como concluídas funcionalidades que ainda estão em implementação.

## Regras

- Correções e melhorias durante a consolidação permanecem na linha `0.9.9.00-SNAPSHOT`.
- Não publicar releases ou tags intermediárias sem decisão explícita do mantenedor.
- Todos os POMs do reactor e o exemplo de addon devem usar a mesma versão do snapshot principal.
- O JAR, workflow, README, changelog, status e documentação precisam identificar o mesmo commit/versão.
- Não sobrescrever o histórico de versões anteriores no changelog.
- A versão estável planejada continua sendo `1.0.0`, somente após todos os critérios de aceite do 0.9.9.

## Sincronização obrigatória

Ao mudar a versão do snapshot, atualizar em conjunto:

1. `pom.xml` raiz e os seis módulos Maven.
2. A propriedade `mechanium.version` do addon de exemplo.
3. Caminho e instruções do JAR no README.
4. Changelog, status e Wiki.
5. Artefatos de CI e metadados do pacote.

## Critérios para concluir 0.9.9

- CI Java 21 / Maven `clean verify` aprovada.
- Testes de integração e persistência aprovados.
- Validação real em Paper e Purpur 1.21.11.
- Migrações antigas preservando dados.
- Documentação, licenças e compatibilidade dos addons revisadas.
- Nenhum problema crítico ou grave conhecido.
- O PR permanece não mesclado e não existe release final até autorização explícita.
