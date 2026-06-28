# TASK-5 — LogbookFeignConfig e anotação dos FeignClients

**Arquivos alvo:**
- `src/main/kotlin/com/staroscky/performance/core/feign/LogbookFeignConfig.kt` (novo)
- `src/main/kotlin/com/staroscky/performance/instituicoes/integration/InstituicoesFinanceirasClient.kt` (modificar)
- `src/main/kotlin/com/staroscky/performance/sugestoes/integration/InteligenciaClient.kt` (modificar)
- `src/main/kotlin/com/staroscky/performance/contatos/integration/ContatosCoreClient.kt` (modificar)

**Referência SPEC:** Seções 5 (RF-05, RF-06, RF-07), 8.4, 8.5
**Depende de:** TASK-1
**Bloqueada por:** nenhuma

---

## Contexto

Configura o Logbook como logger HTTP dos Feign clients de negócio (instituições, inteligência, contatos). O `StsClient` é intencionalmente excluído para evitar log de `client_secret`. A abordagem é explícita: cada `@FeignClient` de negócio recebe `configuration = [LogbookFeignConfig::class]`; o STS não recebe nada.

## O que fazer

### 1. Criar `core/feign/LogbookFeignConfig.kt`

```kotlin
package com.staroscky.performance.core.feign

import feign.Logger
import org.zalando.logbook.Logbook
import org.zalando.logbook.openfeign.FeignLogbookLogger

class LogbookFeignConfig(private val logbook: Logbook) {
    fun feignLogger(): Logger = FeignLogbookLogger(logbook)
}
```

> **Atenção:** esta classe NÃO deve ter `@Configuration`, `@Component` nem qualquer anotação Spring. Se anotada, o Spring a registrará como bean global e o `StsClient` passará a ser logado, expondo credentials.

### 2. Modificar `InstituicoesFinanceirasClient.kt`

Adicionar `configuration = [LogbookFeignConfig::class]` na anotação:

```kotlin
@FeignClient(name = "instituicoes-financeiras", configuration = [LogbookFeignConfig::class])
```

### 3. Modificar `InteligenciaClient.kt`

```kotlin
@FeignClient(name = "inteligencia", configuration = [LogbookFeignConfig::class])
```

### 4. Modificar `ContatosCoreClient.kt`

```kotlin
@FeignClient(name = "contatos-core", configuration = [LogbookFeignConfig::class])
```

### 5. `StsClient.kt` — sem alteração

```kotlin
@FeignClient(name = "sts", url = "${sts.url}")  // permanece como está
```

## Notas de implementação

- `LogbookFeignConfig` é instanciada pelo Spring Cloud Feign em um child context por client — ela pode receber o bean `Logbook` do parent context via construtor sem necessidade de anotação.
- O `loggerLevel: FULL` configurado em TASK-1 é necessário para o `FeignLogbookLogger` receber os dados completos de request/response; sem ele, o Feign não repassa o body ao logger.
- Não há testes unitários para esta task — a configuração é validada na TASK-6 (verificação e2e manual).

## Critério de aceite

- [ ] `LogbookFeignConfig` criada sem nenhuma anotação Spring (`@Configuration`, `@Component`, `@Bean`)
- [ ] `InstituicoesFinanceirasClient`, `InteligenciaClient` e `ContatosCoreClient` têm `configuration = [LogbookFeignConfig::class]`
- [ ] `StsClient` não foi alterado
- [ ] Build compila sem erros
