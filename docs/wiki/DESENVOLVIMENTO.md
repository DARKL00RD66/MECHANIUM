# Desenvolvimento e criação de addons

MECHANIUM é um projeto Maven multi-módulo. A API pública permite criar módulos externos sem empacotar uma segunda cópia do core.

## 1. Ambiente de desenvolvimento

- JDK 21;
- Maven 3.9 ou superior;
- dependência Paper API 1.21.11;
- IDE Java com suporte Maven (opcional).

Na raiz do repositório:

```bash
mvn clean verify
```

O JAR final é produzido por `mechanium-plugin` em:

```text
mechanium-plugin/target/MECHANIUM-<versão>.jar
```

O reactor Maven inclui:
- `mechanium-api`: API pública e contratos;
- `mechanium-storage`: persistência SQLite e migrações;
- `mechanium-engine`: eras, pesquisa e autorização;
- `mechanium-modules`: módulos oficiais;
- `mechanium-addon-loader`: descoberta e ciclo de vida de addons;
- `mechanium-plugin`: bootstrap Bukkit/Paper e JAR final.

## 2. Compilar o addon de exemplo

A API ainda não está publicada em um registry Maven remoto. Instale-a no repositório Maven local a partir da raiz:

```bash
mvn -pl mechanium-api -am install
cd examples/mechanium-addon-example
mvn clean package
```

Copie o JAR resultante para:

```text
plugins/MECHANIUM/addons/
```

Reinicie o servidor para o carregador descobrir o addon. Veja também o [README do projeto-exemplo](../../examples/mechanium-addon-example/README.md).

## 3. Descriptor obrigatório

O JAR do addon deve conter `mechanium-addon.properties` na raiz:

```properties
id=exemplo-industria
version=1.0.0
main=org.exemplo.industria.IndustriaAddon
api-version=1
dependencies=industry,energy
```

- `id`: identificador único; use minúsculas, números, hífen ou underscore.
- `version`: versão do addon, preferencialmente SemVer.
- `main`: classe Java principal.
- `api-version`: versão da API pública compatível.
- `dependencies`: módulos oficiais ou addons que precisam carregar antes.

## 4. Classe principal

```java
package org.exemplo.industria;

import dev.mechanium.api.MechaniumAddon;
import dev.mechanium.api.MechaniumAddonContext;

public final class IndustriaAddon implements MechaniumAddon {
    @Override
    public void onEnable(MechaniumAddonContext context) {
        context.logger().info("Addon conectado ao MECHANIUM");
        // Registre conteúdo e listeners usando a API pública.
    }

    @Override
    public void onDisable() {
        // Cancele tarefas e feche recursos criados pelo addon.
    }
}
```

## 5. Conteúdo e progressão

Use IDs estáveis e únicos para categorias, itens, receitas, máquinas, tecnologias e drops. Todo conteúdo tecnológico deve declarar era e, quando necessário, pré-requisitos. Não contorne o serviço central de progressão ao fabricar, usar itens ou operar máquinas.

Para itens e receitas personalizados, use os tipos de definição da API e registre-os pelo contexto do addon. A documentação técnica completa está em [Addons externos](../ADDONS_EXTERNOS.md).

## 6. Propriedade transacional do conteúdo

Durante `onEnable`, o MECHANIUM abre um escopo transacional para o addon. Registros feitos nesse callback são associados ao proprietário `addon:<id>`. O escopo só é confirmado se a inicialização terminar com sucesso; em caso de falha, alterações em SQLite e memória são revertidas.

A cada inicialização, registre novamente todo o conjunto de conteúdo que o addon deseja manter. Definições próprias ausentes na nova inicialização são removidas durante reconciliação. Se o addon deixar de carregar, suas definições com ownership são removidas.

Para registros dinâmicos fora de `onEnable`, use explicitamente `openContentScope` e faça `commit()` somente depois de registrar o conjunto completo daquele proprietário. Fechar sem commit executa rollback.

## 7. Eventos disponíveis

A documentação da API descreve eventos públicos para:
- mudança de era do jogador;
- alteração do saldo de pontos científicos;
- desbloqueio de tecnologia;
- registro de categoria, item e receita;
- registro de drop de criatura;
- registro de máquina.

Os eventos são síncronos; listeners devem respeitar a thread principal do servidor. Remova listeners e tarefas em `onDisable`.

## 8. Máquinas e drops

A API permite registrar máquinas com item, bloco-base, entrada, quantidade, saída, quantidade, tempo em ticks e tecnologia. O runtime genérico atual trabalha com dispenser, primeiro espaço como entrada e último como saída. Não há múltiplos insumos, múltiplas saídas, fila ou distribuição universal de energia no contrato atual.

A API também permite registrar drops adicionais por tipo de entidade, item MECHANIUM, intervalo de quantidade, chance de 0 a 1 e tecnologia opcional. Os drops vanilla não são removidos.

## 9. Segurança e responsabilidade

Addons são código Java executado dentro do processo do servidor. O carregador confere descriptor, versão de API, dependências e ciclos, mas **não é uma sandbox**. Um addon malicioso pode acessar recursos disponíveis ao processo. Instale apenas JARs confiáveis e mantenha backups.

Não reutilize IDs de conteúdo pertencentes a módulos oficiais ou outros addons. Não empacote cópias de `dev.mechanium.api`. Documente bibliotecas e licenças. Consulte [Licenças e origens](../LICENCAS_E_ORIGENS.md).

## Referências

- [Arquitetura](../ARQUITETURA.md)
- [Contrato completo de addons](../ADDONS_EXTERNOS.md)
- [Exemplo Maven](../../examples/mechanium-addon-example/README.md)
- [Roadmap](../ROADMAP.md)
