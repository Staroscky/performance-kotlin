# TASK-1 — Dependências e Configurações

**Arquivo alvo:** `pom.xml`, `src/main/resources/application.yml`, `src/main/resources/application-*.yml`, `PerformanceApplication.kt` (existente)
**Referência SPEC:** Seções 3, 4.1, 8.2, 8.3, 10
**Depende de:** nenhuma
**Bloqueada por:** nenhuma

---

## Contexto

O projeto está no estado de bootstrap com apenas `spring-boot-starter-web` e um `application.yaml` mínimo. Esta task adiciona todas as dependências necessárias e cria os arquivos de configuração por ambiente, habilitando o contexto para as tasks seguintes compilarem e inicializarem.

## O que fazer

### 1. `pom.xml` — adicionar dependências e BOM

- Adicionar `<dependencyManagement>` com Spring Cloud BOM (verificar versão compatível com Boot 3.5.16 em spring.io/projects/spring-cloud)
- Adicionar dependências:
  - `org.springframework.cloud:spring-cloud-starter-openfeign`
  - `org.cache2k:cache2k-spring:2.6.1.Final`
  - `org.springframework.boot:spring-boot-starter-cache`
  - `org.springframework.boot:spring-boot-starter-data-redis`
  - `org.springframework.boot:spring-boot-configuration-processor` (optional=true)
  - `org.mockito.kotlin:mockito-kotlin:5.4.0` (scope=test)

### 2. `src/main/resources/application.yml` — renomear e expandir

Renomear `application.yaml` → `application.yml` e substituir o conteúdo por:

```yaml
spring:
  application:
    name: performance
  threads:
    virtual:
      enabled: true
  cache:
    type: cache2k
  data:
    redis:
      host: ${integration.valkey.host:localhost}
      port: ${integration.valkey.port:6379}

sts:
  url: ${integration.sts.url:http://localhost:9000/v1/oauth}
  client:
    id: ${integration.sts.client-id}
    secret: ${integration.sts.client-secret}
  app-id: d230d5c5-a270-4a9c-a93e-ac2da7ff176f

spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 1500
            readTimeout: 1500
          instituicoes-financeiras:
            url: ${integration.instituicoes-financeiras.url}
            defaultRequestHeaders:
              x-api-key: ${integration.instituicoes-financeiras.api-key}
              x-app-id: ${sts.app-id}
          inteligencia:
            url: ${integration.inteligencia.url}
          contatos-core:
            url: ${integration.contatos-core.url}
```

### 3. Criar arquivos de perfil

**`application-local.yml`:**
```yaml
sts:
  client:
    id: b7d1ab21-5f8c-42a0-b07c-5150fd59f1cb
    secret: 9773ac44-072b-41a7-b6a8-ccb051047ac0

integration:
  valkey:
    host: localhost
    port: 6379
  instituicoes-financeiras:
    url: http://localhost:9001/v1
    api-key: 1234567890
  inteligencia:
    url: http://localhost:9002/v1
  contatos-core:
    url: http://localhost:9003/v1
```

**`application-dev.yml`**, **`application-hom.yml`**, **`application-prod.yml`**: estrutura idêntica ao local com placeholders `changeme` para todos os valores sensíveis.

### 4. `PerformanceApplication.kt` — adicionar anotações

```kotlin
@SpringBootApplication
@EnableFeignClients
@EnableCaching
@ConfigurationPropertiesScan
class PerformanceApplication

fun main(args: Array<String>) {
    runApplication<PerformanceApplication>(*args)
}
```

### 5. `src/main/resources/config/instituicoes.yml`

Criar arquivo com estrutura mínima de exemplo:

```yaml
instituicoes-config:
  items:
    - ispb: "00000000"
      nome: "Exemplo Banco"
      icone: "ids_exemplo"
```

## Notas de implementação

- Confirmar a versão do Spring Cloud em https://spring.io/projects/spring-cloud#learn antes de fixar no pom.xml
- O `application.yaml` deve ser **deletado** após criar `application.yml` — Spring Boot carrega ambos se existirem, o que pode causar conflitos
- O `spring:` duplicado no YAML acima pode ser mesclado em um único bloco — ajuste conforme necessário para YAML válido
- `spring.cache.type=cache2k` requer `cache2k-spring` no classpath para auto-configuração funcionar

## Critério de aceite

- [ ] `mvn compile` passa sem erros após adicionar as dependências
- [ ] `mvn spring-boot:run -Dspring-boot.run.profiles=local` inicia sem `NoSuchBeanDefinitionException` ou falha de config
- [ ] `application.yaml` antigo removido; apenas `application.yml` existe
- [ ] Anotações `@EnableFeignClients`, `@EnableCaching` e `@ConfigurationPropertiesScan` presentes na `PerformanceApplication`
- [ ] Build e testes passam sem erros
