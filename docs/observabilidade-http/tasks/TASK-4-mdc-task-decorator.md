# TASK-4 — MdcTaskDecorator

**Arquivo alvo:** `src/main/kotlin/com/staroscky/performance/core/async/MdcTaskDecorator.kt` (novo)
**Arquivo de teste:** `src/test/kotlin/com/staroscky/performance/core/async/MdcTaskDecoratorTest.kt` (novo)
**Referência SPEC:** Seções 8.7
**Depende de:** TASK-1
**Bloqueada por:** nenhuma

---

## Contexto

Infraestrutura defensiva para propagação de MDC em `ThreadPoolTaskExecutor`. Não é wired a nenhum executor nesta feature — fica disponível como `@Component` para uso explícito em futuros executores baseados em pool de threads. Virtual threads (configuração atual do projeto) não precisam deste decorator, pois Logback 1.5.x usa `InheritableThreadLocal` e propaga MDC automaticamente.

## O que fazer

### Implementação

Criar `core/async/MdcTaskDecorator.kt`:

```kotlin
package com.staroscky.performance.core.async

import org.slf4j.MDC
import org.springframework.core.task.TaskDecorator
import org.springframework.stereotype.Component

@Component
class MdcTaskDecorator : TaskDecorator {

    override fun decorate(runnable: Runnable): Runnable {
        val contextMap = MDC.getCopyOfContextMap() ?: emptyMap()
        return Runnable {
            MDC.setContextMap(contextMap)
            try {
                runnable.run()
            } finally {
                MDC.clear()
            }
        }
    }
}
```

### Testes

Criar `MdcTaskDecoratorTest.kt` cobrindo um critério por teste:

**Teste 1 — MDC propagado:** popular MDC no thread atual, decorar um `Runnable`, executá-lo em thread separada (`Thread(decorated).start()`), verificar que o MDC estava populado dentro do `Runnable`.

**Teste 2 — MDC limpo após execução:** após o `Runnable` decorado ser executado, o MDC interno ao `Runnable` deve ser limpo no `finally`.

**Teste 3 — MDC vazio no submissor:** quando não há MDC no thread que submete, o `Runnable` decorado deve rodar com MDC vazio (sem NullPointerException).

**Teste 4 — MDC limpo após exceção:** se o `Runnable` lançar exceção, o MDC ainda deve ser limpo no `finally`.

## Notas de implementação

- `MDC.getCopyOfContextMap()` retorna `null` quando o MDC está vazio — o `?: emptyMap()` trata esse caso.
- `MDC.setContextMap(emptyMap())` é equivalente a `MDC.clear()` — comportamento seguro.
- O teste de propagação entre threads requer sincronização (ex.: `CountDownLatch` ou `CompletableFuture`) para aguardar a thread filha antes de verificar o resultado.
- Este bean NÃO deve ser usado com `SimpleAsyncTaskExecutor` (virtual threads) — não é suportado. Documentar no KDoc se conveniente.

## Critério de aceite

- [ ] MDC do thread submissor propagado para o `Runnable` decorado
- [ ] MDC limpo dentro do `Runnable` após execução normal
- [ ] Sem NullPointerException quando MDC está vazio no submissor
- [ ] MDC limpo mesmo quando `Runnable` lança exceção
- [ ] Build e testes passam sem erros
