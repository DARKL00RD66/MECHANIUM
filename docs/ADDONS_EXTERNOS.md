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
        // Registrar conteúdos e listeners usando a API pública.
    }

    @Override
    public void onDisable() {
        // Cancelar tarefas, remover listeners e fechar recursos.
    }
}
```

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
- Limpar tarefas e recursos em `onDisable`.
- Documentar licenças e bibliotecas incluídas.

Addons executam código Java no servidor. Instale apenas extensões de fontes confiáveis.


## Dependências entre addons

Quando um addon declara outro addon em `dependencies`, o carregador garante a ordem topológica e disponibiliza as classes e recursos dos addons declarados como dependências. Addons não declarados não são adicionados ao classpath do consumidor.

A compatibilidade de API é verificada pelo campo `api-version`. O campo `version` identifica a versão do addon e deve seguir versionamento semântico.

**Limite de segurança:** addons são código Java executado dentro do processo do servidor. O carregador não é uma sandbox e não consegue impedir que um addon malicioso acesse recursos do processo. Use somente JARs confiáveis e faça backup do servidor.
