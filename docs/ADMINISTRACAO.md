function content(value) {
    const iterate =
      typeof value === "string" ? undefined : value?.[iteratorKey];
    if (typeof iterate !== "function") {
      return emitBlock(value);
    }

    // Reuse the method from a potentially stateful getter, with its original
    // receiver. The for-of loop closes the iterator if the host rejects a block.
    const blocks = { [iteratorKey]: () => apply(iterate, value, []) };
    for (const block of blocks) {
      emitBlock(block);
    }
  }

## Rebalanceamento de eras das receitas vanilla

Na primeira inicialização do snapshot 0.7.4.02, o plugin executa a migração `historical-distribution-0-7-4-02`. Ela reclassifica regras cujo valor salvo ainda coincide com a sugestão automática da versão anterior, quando a nova classificação recomenda outra era. Valores `off` e valores diferentes da sugestão antiga são preservados. O observador, que vinha no config padrão como `modern`, é atualizado para `digital_age`.

Versões anteriores não guardavam a origem de cada valor. Por isso, a migração atualiza regras que coincidem com sugestões automáticas antigas e também corrige cinco defaults empacotados que estavam fora da nova classificação (bigorna, alto-forno, defumador, detector de luz e observador). Uma escolha manual que coincidia exatamente com esses valores também pode ser atualizada. O console registra essa ressalva. Faça backup do `config.yml`, examine `/mechanium admin recipes` após a migração e reaplique exceções manuais com `/mechanium admin recipe minecraft:<id> <era|off>`.

Os critérios e exemplos por era estão na [auditoria histórica da Wiki](wiki/DISTRIBUICAO-HISTORICA.md).
