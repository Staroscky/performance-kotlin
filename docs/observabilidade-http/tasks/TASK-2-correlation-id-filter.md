# TASK-2 — CorrelationIdFilter

**Arquivo alvo:** `src/main/kotlin/com/staroscky/performance/core/filter/CorrelationIdFilter.kt` (novo)
**Arquivo de teste:** `src/test/kotlin/com/staroscky/performance/core/filter/CorrelationIdFilterTest.kt` (novo)
**Referência SPEC:** Seções 5 (RF-01, RF-02, RF-08), 8.2
**Depende de:** TASK-1
**Bloqueada por:** nenhuma

---

## Contexto

Filtro Servlet de infraestrutura responsável por garantir que toda requisição tenha um `correlationId` no MDC antes de qualquer lógica de negócio. Gera UUID quando o header está ausente. Limpa o MDC ao final da requisição para não vazar contexto entre threads (especialmente relevante com virtual threads e Tomcat).

## O que fazer

### Implementação

Criar `core/filter/CorrelationIdFilter.kt`:

```kotlin
package com.staroscky.performance.core.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val correlationId = request.getHeader(HEADER_NAME) ?: UUID.randomUUID().toString()
        MDC.put(MDC_KEY, correlationId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
        }
    }

    companion object {
        const val HEADER_NAME = "x-correlationId"
        const val MDC_KEY = "correlationId"
    }
}
```

### Testes

Criar `CorrelationIdFilterTest.kt` cobrindo um critério por teste:

**Teste 1 — header presente:** MDC deve conter o valor exato do header após `doFilterInternal`.

**Teste 2 — header ausente:** MDC deve conter um UUID v4 válido (não nulo, não blank, parseable por `UUID.fromString`).

**Teste 3 — MDC limpo no finally:** após `doFilterInternal`, `MDC.get(MDC_KEY)` deve retornar null (mesmo que a `filterChain` lance exceção).

Usar `MockHttpServletRequest`, `MockHttpServletResponse` e uma `FilterChain` mockada via mockito-kotlin.

## Notas de implementação

- `@Order(Ordered.HIGHEST_PRECEDENCE)` garante execução antes do `LogbookFilter` do Logbook — o correlationId estará no MDC quando o Logbook gravar o log da request.
- O `finally` garante limpeza mesmo em erro 5xx — não usar `afterCompletion` de `HandlerInterceptor` que não é garantido em todas as exceções.
- O teste do `finally` com exceção: passar um mock de `FilterChain` que lance `RuntimeException` no `doFilter`, verificar que o MDC foi limpo mesmo assim.

## Critério de aceite

- [ ] Header presente: MDC populado com o valor exato do header
- [ ] Header ausente: MDC populado com UUID v4 válido (não blank, parseável)
- [ ] MDC removido no `finally` mesmo quando `filterChain.doFilter` lança exceção
- [ ] Build e testes passam sem erros
