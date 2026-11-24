# Desenvolvimento de addons externos

Addons são JARs independentes que usam `mechanium-api`. Eles não são plugins Bukkit separados: MECHANIUM controla carregamento e ciclo de vida.

## Projeto de exemplo

Há um addon Maven independente em [`examples/mechanium-addon-example`](../examples/mechanium-addon-example/README.md). Ele demonstra descriptor, dependência de módulo, registro de conteúdo, listener Bukkit e limpeza no ciclo de vida.

Na raiz do repositório MECHANIUM, execute:

```bash
mvn -pl mechanium-api -am install
cd examples/mechanium-addon-example
mvn clean package
```

O JAR resultante deve ser copiado para `plugins/MECHANIUM/addons/`.

## Descriptor obrigatório

Adicionar `mechanium-addon.properties` na raiz do JAR:

```properties
id=exemplo-industria
version=1.0.0
main=org.exemplo.industria.IndustriaAddon
api-version=1
dependencies=industry,energy
```

IDs devem ser únicos e usar letras minúsculas, números, hífen ou underscore. Dependências podem apontar para módulos oficiais ou outros addons.

## Classe principal

```java
package org.exemplo.industria;

import dev.mechanium.api.MechaniumAddon;
import dev.mechanium.api.MechaniumAddonContext;

public final class IndustriaAddon implements MechaniumAddon {
    @Override
    public void onEnable(MechaniumAddonContext context) {
        context.logger().info("Addon conectado ao MECHANIUM");
        // Registrar conteúdos, tecnologias e listeners usando a API pública.
    }

    @Override
    public void onDisable() {
        // Cancelar tarefas e fechar recursos criados pelo addon.
    }
}
```

## Eventos públicos de progressão

Addons podem registrar listeners Bukkit para acompanhar mudanças do motor:

- `MechaniumPlayerEraChangeEvent`: jogador, era anterior e nova era.
- `MechaniumResearchPointsChangeEvent`: saldo científico anterior e novo.
- `MechaniumTechnologyUnlockedEvent`: jogador, definição da tecnologia e saldo após o desbloqueio.
- `MechaniumCategoryRegisterEvent`, `MechaniumItemRegisterEvent` e `MechaniumRecipeRegisterEvent`: novos conteúdos registrados.
- `MechaniumMobDropRegisterEvent`: nova regra de drop de criatura registrada.
- `MechaniumMachineRegisterEvent`: nova definição de máquina industrial registrada.

Addons podem registrar máquinas com `api.registerMachine(new MachineDefinition(...))`. O modelo atual define lista de até seis `MachineIngredient`, até três `MachineProduct`, duração em ticks, custo de energia por operação e tecnologia exigida. O runtime hospeda máquinas em `DISPENSER`: slots 0–5 são entradas e slots 6–8 são saídas. O custo energético é declarado na própria definição; valor zero significa que a máquina não consome energia. Máquinas energizadas devem conectar-se a uma bateria do mesmo proprietário, diretamente ou por conduítes `energy_conduit`. O progresso, desgaste e UUID do proprietário ficam no PDC do bloco; as definições e seus ingredientes/produtos são persistidos no SQLite (schema atual v9). O bloco físico continua limitado a `DISPENSER`; outros tipos de máquina aguardam uma extensão do runtime.

### Registrar fluidos e recipientes

A API pública permite registrar fluidos personalizados com `FluidDefinition` e `FluidContainerDefinition`. Registre primeiro a tecnologia requerida e cada item de recipiente com `api.registerTechnology(...)` e `api.registerItem(...)`; depois registre o fluido:

```java
api.registerFluid(new FluidDefinition(
    "industrial_coolant",
    "Refrigerante industrial",
    "industrial_water_recycling",
    List.of(
        new FluidContainerDefinition("coolant_cell", 250, true, true),
        new FluidContainerDefinition("coolant_canister", 1000, true, false)
    )
));
```

Cada recipiente declara seu volume em milibuckets (mB), se pode abastecer o tanque e se pode retirar fluido. O volume deve ser múltiplo de 250 mB; o registro precisa conter ao menos um recipiente de entrada e um de saída. Os IDs dos itens devem corresponder a itens MECHANIUM já registrados e a tecnologia indicada precisa existir.

Os tanques guardam o ID e o volume no PDC, bloqueiam mistura de fluidos diferentes e transferem volume entre tanques conectados do mesmo proprietário. Ao retirar fluido, o runtime escolhe o maior recipiente de saída que esteja desbloqueado e caiba no volume disponível. As definições e seus recipientes são persistidos no SQLite v9 e associados ao módulo/addon que os registrou; se o registro do fluido for removido, o volume que já está em um tanque permanece preservado. Ainda não há adaptadores para fluidos vanilla, válvulas ou bombas.

Addons podem registrar regras de drops com `api.registerMobDrop(new MobDropDefinition(...))`. A definição informa o tipo de entidade Bukkit, ID do item MECHANIUM, quantidade mínima/máxima, chance entre 0 e 1 e tecnologia opcional. A regra e o próprio item são verificados contra a progressão do jogador antes do drop. Definições de máquinas e regras de drops são persistidas no SQLite e restauradas no próximo carregamento da API. O registro persistente guarda a definição; tarefas, listeners e integrações de runtime continuam sendo responsabilidade do addon em cada inicialização.

O método `api.hasUnlockedTechnology(playerId, technologyId)` consulta se a pesquisa foi concluída. `api.canUseTechnology(playerId, technologyId)` também verifica se a era atual autoriza seu uso; os dois estados não são equivalentes.

Os eventos são síncronos e devem ser consumidos no contexto da thread principal do servidor. Registre listeners com o plugin hospedeiro e remova-os no desligamento do addon para evitar referências residuais.

## Dependência Maven

Declare a API como `provided`:

```xml
<dependency>
  <groupId>dev.mechanium</groupId>
  <artifactId>mechanium-api</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
```

A API ainda não está publicada em registry Maven remoto. Use `mvn -pl mechanium-api install` para disponibilizá-la no repositório Maven local.

## Instalação

Coloque o JAR em `plugins/MECHANIUM/addons/` e reinicie. O carregador valida descriptor, versão de API, dependências e ciclos. Addons são desligados em ordem inversa.

## Regras

- Não sombrear nem empacotar cópias de `dev.mechanium.api`.
- Registrar toda tecnologia com era e pré-requisitos.
- Revalidar autorização ao fabricar, usar, operar máquinas ou transferir tecnologia.
- Persistir proprietários por UUID.
- Limpar tarefas, listeners e recursos em `onDisable`.
- Documentar licenças e bibliotecas incluídas.

Addons executam código Java no servidor. O carregador não é uma sandbox e não consegue impedir que um addon malicioso acesse recursos do processo. Instale somente JARs de fontes confiáveis e faça backup do servidor.


## Propriedade e ciclo de vida do conteúdo

Durante `onEnable`, MECHANIUM abre automaticamente um escopo transacional para o addon. Os registros de categorias, itens, receitas, máquinas, fluidos e drops feitos nesse callback ficam associados a `addon:<id>`. O escopo é confirmado somente quando `onEnable` termina com sucesso; em caso de falha, as gravações SQLite e as definições em memória são revertidas.

A cada inicialização, o addon deve registrar novamente todo o conteúdo que deseja manter. Se um addon carregar sem registrar uma definição que era sua, essa definição antiga é removida durante a reconciliação. Se o addon deixar de carregar, todas as definições com ownership dele são removidas. Um ID de conteúdo tem um único proprietário: não reutilize IDs pertencentes a módulos oficiais ou a outros addons. Declare sempre as dependências no descriptor.

Para registrar conteúdo dinamicamente depois do `onEnable`, abra explicitamente um escopo:

```java
try (var scope = context.api().openContentScope("addon:" + "exemplo-industria")) {
    // Registrar ou atualizar o conjunto de conteúdo pertencente ao addon.
    scope.commit();
}
```

Cada escopo representa o conjunto completo de conteúdo daquele proprietário; portanto, registre nele todos os IDs que devem continuar ativos, não apenas um item isolado. O fechamento sem `commit()` faz rollback. A migração SQLite v4 adota os IDs conhecidos dos módulos oficiais já existentes. Conteúdo legado de addons que ainda não possui ownership não é apagado automaticamente; ele precisa ser registrado novamente pelo addon para passar a ser gerenciado.

## Dependências entre addons

Quando um addon declara outro addon em `dependencies`, o carregador garante a ordem topológica e disponibiliza classes e recursos dos addons declarados como dependências. Addons não declarados não são adicionados ao classpath do consumidor.

A compatibilidade de API é verificada pelo campo `api-version`. O campo `version` identifica a versão do addon e deve seguir versionamento semântico.
