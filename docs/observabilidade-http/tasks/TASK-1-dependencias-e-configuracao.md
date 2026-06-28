# TASK-1 — Dependências e configuração

**Arquivo alvo:** `pom.xml`, `application.yml`, `application-local.yml`, `application-dev.yml`, `application-hom.yml`, `application-prod.yml`
**Referência SPEC:** Seções 4.1, 8.6
**Depende de:** nenhuma
**Bloqueada por:** nenhuma

---

## Contexto

Base de todas as demais tasks. Adiciona as dependências do Logbook e configura os parâmetros de logging e Feign por ambiente. Sem isso, as tasks 2 a 5 não compilam ou não têm efeito.

## O que fazer

### 1. `pom.xml`

Adicionar property e dependências:

```xml
<properties>
    <logbook.version>3.10.0</logbook.version>  <!-- verificar última 3.x no Maven Central -->
</properties>

<dependencies>
    <dependency>
        <groupId>org.zalando</groupId>
        <artifactId>logbook-spring-boot-starter</artifactId>
        <version>${logbook.version}</version>
    </dependency>
    <dependency>
        <groupId>org.zalando</groupId>
        <artifactId>logbook-feign</artifactId>
        <version>${logbook.version}</version>
    </dependency>
</dependencies>
```

> Verificar no Maven Central a versão 3.x mais recente compatível com Spring Boot 3.5 / Spring Cloud 2023.0.x antes de fixar o valor.

### 2. `application.yml`

Adicionar ao final do arquivo:

```yaml
logbook:
  filter:
    enabled: true
  format:
    style: http
  include:
    - /**

spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            loggerLevel: FULL
          sts:
            loggerLevel: NONE
```

> Mesclar com o bloco `spring.cloud.openfeign` existente — não duplicar a chave.

### 3. `application-local.yml`

Adicionar padrão de log amigável com correlationId:

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ} [%thread] %-5level [%X{correlationId:-}] %logger{36} - %msg%n"
```

### 4. `application-dev.yml`, `application-hom.yml`, `application-prod.yml`

Adicionar em cada um:

```yaml
logging:
  structured:
    format:
      console: ecs
```

## Notas de implementação

- O bloco `spring.cloud.openfeign.client.config` já existe em `application.yml` com configs de `connectTimeout`, `readTimeout` e URLs. Adicionar `loggerLevel` dentro dos blocos existentes sem sobrescrever.
- `loggerLevel: FULL` no `default` aplica-se a todos os clients incluindo futuros; STS é explicitamente sobrescrito para `NONE`.
- O formato `ecs` (Spring Boot 3.4+) inclui MDC automaticamente no JSON — nenhuma config extra necessária para que `correlationId` apareça no log estruturado.
- Não criar `logback-spring.xml` — tudo via `application.yml`.

## Critério de aceite

- [ ] `mvn compile` passa sem erros após adição das dependências
- [ ] `logbook-spring-boot-starter` e `logbook-feign` visíveis em `mvn dependency:tree`
- [ ] `application.yml` contém o bloco `logbook:` e `loggerLevel` default/sts
- [ ] `application-local.yml` contém `logging.pattern.console` com `%X{correlationId:-}`
- [ ] `application-dev.yml`, `application-hom.yml`, `application-prod.yml` contêm `logging.structured.format.console: ecs`
