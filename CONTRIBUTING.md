# Contribuindo para MECHANIUM

## Requisitos
- JDK 21
- Maven 3.9+
- Paper 1.21.11 para testes de integração

## Fluxo
1. Crie uma branch específica.
2. Mantenha módulos com responsabilidades claras.
3. Adicione testes para cada regra de negócio.
4. Execute `mvn clean verify`.
5. Documente mudanças na API e nas configurações.
6. Não importe código externo sem registrar licença e proveniência.

## Regras
- Dependências devem apontar para abstrações estáveis.
- Módulos opcionais permanecem isolados.
- Addons externos usam `mechanium-api` como provided.
- Nenhum módulo pode contornar o serviço central de progressão.
- UUID é a identidade persistente dos jogadores.
- Evite NMS; use APIs públicas sempre que possível.

Relatos de falha devem incluir versão do servidor, Java, MECHANIUM, módulos ativos e logs relevantes, removendo dados privados.
