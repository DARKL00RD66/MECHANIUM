# Desenvolvimento de addons externos

Addons são JARs independentes que usam `mechanium-api`. Eles não são plugins Bukkit separados: MECHANIUM controla carregamento e ciclo de vida.

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

Addons também podem registrar máquinas com `api.registerMachine(new MachineDefinition(...))`. O modelo atual define item de máquina, bloco-base, item e quantidade de entrada, item e quantidade de saída, tempo em ticks e tecnologia exigida. O runtime inicial implementa máquinas hospedadas em `DISPENSER`, com primeiro espaço como entrada e último como saída; o registro de outras classes de máquinas aguarda evolução do runtime. O progresso e UUID do proprietário ficam em PDC do bloco e são preservados por reinício/chunk unload. Não há sistema de energia nem rede de máquinas nesta etapa.

Addons podem registrar regras de drops com `api.registerMobDrop(new MobDropDefinition(...))`. A definição informa o tipo de entidade Bukkit, ID do item MECHANIUM, quantidade mínima/máxima, chance entre 0 e 1 e tecnologia opcional. A regra e o próprio item são verificados contra a progressão do jogador antes do drop. Regras são declaradas pelos módulos/addons durante o bootstrap e não são persistidas como configuração editável no SQLite.

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

## Dependências entre addons

Quando um addon declara outro addon em `dependencies`, o carregador garante a ordem topológica e disponibiliza classes e recursos dos addons declarados como dependências. Addons não declarados não são adicionados ao classpath do consumidor.

A compatibilidade de API é verificada pelo campo `api-version`. O campo `version` identifica a versão do addon e deve seguir versionamento semântico.
