package com.staroscky.performance.core.async

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.util.concurrent.CompletableFuture

class MdcTaskDecoratorTest {

    private val decorator = MdcTaskDecorator()

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `propaga MDC do thread submissor para o Runnable decorado`() {
        MDC.put("correlationId", "valor-propagado")
        val future = CompletableFuture<String?>()
        val decorated = decorator.decorate(Runnable { future.complete(MDC.get("correlationId")) })

        Thread(decorated).start()

        assertThat(future.get()).isEqualTo("valor-propagado")
    }

    @Test
    fun `MDC e limpo dentro do Runnable apos execucao normal`() {
        MDC.put("correlationId", "valor-que-deve-ser-limpo")
        val mdcAposExecucao = CompletableFuture<String?>()
        val decorated = decorator.decorate(Runnable {
            // executa e o finally limpa o MDC
        })
        val thread = Thread {
            decorated.run()
            mdcAposExecucao.complete(MDC.get("correlationId"))
        }

        thread.start()

        assertThat(mdcAposExecucao.get()).isNull()
    }

    @Test
    fun `nao lanca excecao quando MDC esta vazio no submissor`() {
        val future = CompletableFuture<Boolean>()
        val decorated = decorator.decorate(Runnable { future.complete(true) })

        Thread(decorated).start()

        assertThat(future.get()).isTrue()
    }

    @Test
    fun `MDC e limpo mesmo quando Runnable lanca excecao`() {
        MDC.put("correlationId", "valor")
        val mdcAposExcecao = CompletableFuture<String?>()
        val decorated = decorator.decorate(Runnable { throw RuntimeException("erro simulado") })
        val thread = Thread {
            runCatching { decorated.run() }
            mdcAposExcecao.complete(MDC.get("correlationId"))
        }

        thread.start()

        assertThat(mdcAposExcecao.get()).isNull()
    }
}
