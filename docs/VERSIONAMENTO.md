# Política de versionamento do MECHANIUM

O desenvolvimento segue snapshots incrementais até a primeira versão estável.

## Regras

- Correções pequenas usam patch adicional.
- Novos conjuntos de funcionalidades ou integrações começam um novo marco.
- O formato de versões do projeto usa quatro blocos numéricos, como `0.7.0.00-SNAPSHOT`.
- Os domínios integrados permanecem no único JAR oficial; o sufixo de snapshot identifica o estado de desenvolvimento e não significa que cada módulo interno seja um JAR independente.
- A sequência de snapshots avança até `0.9.9.00-SNAPSHOT`; a versão estável planejada permanece `1.0.0`, após cumprir os critérios de aceite.

## Marco atual

A linha atual é `0.7.0.00-SNAPSHOT`. Este marco acrescenta cinco módulos inspirados em addons de referência, melhorias de fabricação, filtros de catálogo e uma economia mais visível de pontos de pesquisa.

## Sincronização obrigatória

Ao avançar a versão, atualizar em conjunto:

1. `pom.xml` raiz e todos os módulos Maven.
2. A dependência `mechanium.version` do addon de exemplo.
3. Caminhos de artefatos e instruções no README.
4. Changelog, estado do projeto, matriz de módulos e roadmap.
5. Tag/release de snapshot somente depois do CI construir e publicar o JAR.

Nunca substituir o conteúdo histórico do changelog. Cada snapshot mantém sua seção e o JAR publicado deve corresponder exatamente ao commit indicado na release.
