# TASK-3 — CorrelationIdRequestInterceptor

**Arquivo alvo:** `src/main/kotlin/com/staroscky/performance/core/feign/CorrelationIdRequestInterceptor.kt` (novo)
**Arquivo de teste:** `src/test/kotlin/com/staroscky/performance/core/feign/CorrelationIdRequestInterceptorTest.kt` (novo)
**Referência SPEC:** Seções 5 (RF-04), 8.3
**Depende de:** TASK-1
**Bloqueada por:** nenhuma

---

## Contexto

Interceptor Feign responsável por propagar o `correlationId` do MDC como header `x-correlationId` em todas as chamadas de saída. Por ser `@Component`, aplica-se globalmente a todos os `FeignClient` (incluindo STS — o header `x-correlationId` é ignorado pelo STS sem impacto). O Logbook é tratado separadamente via `LogbookFeignConfig` (TASK-5).

## O que fazer

### Implementação

Criar `core/feign/CorrelationIdRequestInterceptor.kt`:

```kotlin
package com.staroscky.performance.core.feign

import feign.RequestInterceptor
import feign.RequestTemplate
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
class CorrelationIdRequestInterceptor : RequestInterceptor {

    override fun apply(template: RequestTemplate) {
        val correlationId = MDC.get(MDC_KEY)
        if (!correlationId.isNullOrBlank()) {
            template.header(HEADER_NAME, correlationId)
        }
    }

    companion object {
        private const val HEADER_NAME = "x-correlationId"
        private const val MDC_KEY = "correlationId"
    }
}
```

### Testes

Criar `CorrelationIdRequestInterceptorTest.kt` cobrindo um critério por teste:

**Teste 1 — MDC com correlationId:** após `apply`, o `RequestTemplate` deve ter o header `x-correlationId` com o valor do MDC.

**Teste 2 — MDC vazio (null):** `apply` não deve adicionar o header `x-correlationId`.

**Teste 3 — MDC com string blank:** `apply` não deve adicionar o header (blank é tratado como ausente).

Usar `RequestTemplate()` real (não mock) e `MDC.put`/`MDC.remove` no setup/teardown de cada teste.

## Notas de implementação

- Não adicionar header quando MDC está vazio ou blank — evita enviar `x-correlationId: ` (valor vazio) para serviços externos.
- O `MDC_KEY` deve ser idêntico ao `MDC_KEY` do `CorrelationIdFilter` — considerar extrair para constante compartilhada se houver risco de drift, mas constante local é suficiente por enquanto.
- O `RequestTemplate` pode ser instanciado diretamente nos testes: `RequestTemplate()`.

## Critério de aceite

- [ ] Com MDC populado: header `x-correlationId` presente no `RequestTemplate` com valor correto
- [ ] Com MDC null: header `x-correlationId` ausente no `RequestTemplate`
- [ ] Com MDC blank: header `x-correlationId` ausente no `RequestTemplate`
- [ ] Build e testes passam sem erros
