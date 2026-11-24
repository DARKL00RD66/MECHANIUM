# Guia de administração do MECHANIUM

## 1. Requisitos do servidor

- Minecraft 1.21.11;
- Paper ou Purpur compatível com a API-alvo;
- Java 21;
- JAR do MECHANIUM correspondente ao snapshot que será instalado.

O projeto é experimental. Faça primeiro a instalação em uma cópia de teste do servidor e valide menus, máquinas, receitas, bancos e addons antes de atualizar o servidor principal.

## 2. Instalação inicial

1. Pare o servidor e faça backup.
2. Copie o JAR para a pasta `plugins/`.
3. Inicie o Paper/Purpur uma vez para gerar `plugins/MECHANIUM/`.
4. Pare novamente o servidor antes de editar configurações.
5. Ajuste `config.yml` e confira módulos/dependências.
6. Inicie o servidor e examine o console para erros de carregamento.
7. Entre com uma conta de teste, abra o livro-guia e valide a árvore, Crafting, Wiki e máquinas habilitadas.

Não instale addons externos de origem desconhecida. Addons executam código Java no processo do servidor; o carregador não é uma sandbox.

## 3. Atualizações e backups

Antes de trocar o JAR:
- Pare o servidor.
- Faça backup de `plugins/MECHANIUM/config.yml`, `mechanium.db`, `emc-exchange.db` e da pasta `addons/`.
- Guarde o JAR anterior para rollback.
- Instale o novo JAR sem apagar os bancos.
- Inicie e confira migrações/erros no console.
- Teste a interface e uma cadeia de produção em ambiente separado.

O banco principal usa SQLite com migrações versionadas. O ledger EMC fica em banco separado. Não edite SQLite manualmente sem backup e sem compreender as relações de dados.

## 4. Definir a era do servidor

A era global persistida vale para jogadores que não tenham uma sobrescrita individual de teste.

- Painel: `/mechanium admin`.
- Comando global: `/mechanium admin era <era>`.
- Sobrescrita individual de teste: `/mechanium admin era <era> <jogador>`.
- A tela de eras individuais permite atribuir era de teste e retornar para **Herdar era global**.

A mudança global não apaga as sobrescritas individuais. Jogadores offline recebem o aviso da nova era ao entrar, conforme a revisão de anúncio salva.

A era inicial configurada é fallback para servidores sem era global persistida. Alterar `default-era` depois que a era global já foi persistida não substitui automaticamente a era global atual.

## 5. Pontos científicos

O crédito inicial padrão é 40 pontos para jogadores elegíveis sem pesquisa concluída e saldo zero. Recompensas de minério são únicas por jogador e por tipo de bloco/minério. Ajuste os valores em `research-rewards.blocks`; use 0 ou remova a entrada para desabilitar uma recompensa.

Pesquisas do menu são temporizadas. A duração usa mínimo, base, custo científico e posição da era; jobs persistem em SQLite e seguem o relógio mesmo com o jogador offline.

## 6. Regras para receitas vanilla

Abra `/mechanium admin recipes` para consultar o catálogo suportado. A lista é paginada e mostra resultado, ID, tipo de processo e era exigida.

Clique em uma entrada para avançar entre as eras; depois da última, o requisito vira `off`. Também é possível usar:

```text
/mechanium admin recipe minecraft:enchanting_table mythic_age
/mechanium admin recipe minecraft:enchanting_table off
```

O bloqueio é aplicado aos contextos suportados: crafting do jogador, livro de receitas, autocrafter, fornalhas, stonecutter e smithing. Processos automáticos sem jogador usam era global; ações de jogadores usam a era efetiva do jogador.

Limites: receitas de fogueira são omitidas; poções e algumas operações especiais não usam a mesma registry de receitas; itens sem receita registrada não entram neste catálogo.

## 7. Editor de receitas MECHANIUM

Use `/mechanium admin custom-recipes`. O editor permite alterar receitas registradas por módulos e addons, incluindo resultado, quantidade, tecnologia, formato, ingredientes e padrão shaped. Alterações são gravadas como overrides em `admin-recipe-overrides`, não modificam as definições canônicas/ownership em SQLite.

O editor não cria IDs de itens ou receitas novos. Para criar conteúdo novo, desenvolva um módulo/addon e registre-o pela API.

## 8. Segurança de máquinas e armazenamento

Máquinas, caixas e nós protegidos registram o UUID do proprietário. Respeite as permissões/ACLs do sistema e teste o comportamento com:
- proprietário;
- jogador não confiável;
- jogador confiável, quando aplicável;
- administrador;
- chunk descarregado e após reinício.

Não presuma que toda rede é global: energia, água, conduítes e Simple Storage têm limites locais e capacidades distintas.

## 9. Towny

Towny é uma dependência opcional. A integração descrita sincroniza UUID/nome da nação do jogador e publica o serviço `NationIdentityProvider` para módulos/addons. O plugin não exige Towny para o core.

## 10. Diagnóstico básico

- Confira o console durante o startup e procure o primeiro erro, não apenas a última mensagem.
- Confirme Java 21 e versão do Paper/Purpur.
- Confirme que o JAR principal está em `plugins/` e addons em `plugins/MECHANIUM/addons/`.
- Verifique o ID do módulo e suas dependências no `config.yml`.
- Em receitas, confirme se a receita aparece no catálogo suportado e se o requisito está definido como esperado.
- Em máquinas, valide tecnologia, propriedade, item de entrada, espaço de saída, desgaste e energia.
- Em redes, valide adjacência, proprietário comum, chunks carregados e os limites descritos na Wiki.
- Não conclua que uma funcionalidade está quebrada apenas pelo teste Maven; reproduza também em um servidor limpo Paper/Purpur 1.21.11.

## 11. Referências de administração

- [Comandos e configuração](COMANDOS-E-CONFIGURACAO.md)
- [Painel administrativo detalhado](../ADMINISTRACAO.md)
- [Estado real do projeto](../STATUS.md)
- [Roadmap e critérios de aceite](../ROADMAP.md)
