# Política de versionamento do MECHANIUM

O projeto seguirá a sequência de snapshots definida para chegar à primeira versão estável.

## Regras

- **Correções pequenas e ajustes localizados:** incrementam o patch da linha atual. Exemplo: `0.3.0-SNAPSHOT`, `0.3.1-SNAPSHOT`, `0.3.2-SNAPSHOT`.
- **Novos conjuntos de funcionalidades ou integrações de addons:** iniciam o próximo marco de funcionalidades. Exemplo: `0.4.0-SNAPSHOT`, depois `0.5.0-SNAPSHOT`.
- **Alterações dentro de um marco:** incrementam seu patch sem substituir snapshots anteriores. Exemplo: `0.4.1-SNAPSHOT`, `0.4.2-SNAPSHOT`. Para snapshots numerados dentro do mesmo patch, usar dois dígitos finais: `0.7.1.00-SNAPSHOT`, `0.7.1.01-SNAPSHOT`, `0.7.1.02-SNAPSHOT`.
- **Pré-lançamento final:** a sequência de desenvolvimento avança até `0.9.9-SNAPSHOT`.
- **Versão oficial:** somente depois da validação dos critérios de aceite e do encerramento da linha de snapshots; a versão estável planejada é `1.0.0`.

## Marco atual

A linha atual documentada é `0.7.4.01-SNAPSHOT`. Correções pequenas devem avançar o sufixo incremental seguindo a convenção adotada no marco vigente; novos conjuntos de funcionalidades podem abrir um novo marco. Preserve snapshots e artefatos anteriores. Antes de publicar uma nova versão, confira todos os POMs, o addon de exemplo, README, changelog e workflow.

## Sincronização obrigatória

Ao avançar a versão, atualizar em conjunto:

1. `pom.xml` raiz e todos os módulos Maven, mantendo a mesma versão `X.Y.Z.NN-SNAPSHOT` em todo o reactor.
2. A dependência `mechanium.version` do addon de exemplo.
3. Caminhos de artefatos e instruções no README.
4. Changelog e estado do projeto.
5. Tag/release de snapshot somente depois do CI construir e publicar o JAR.

Nunca substituir o conteúdo histórico do changelog de versões anteriores. Cada snapshot mantém sua própria seção e o JAR publicado deve corresponder ao commit identificado na release.
