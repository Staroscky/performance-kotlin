package com.staroscky.performance.core.feign

import feign.RequestTemplate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class CorrelationIdRequestInterceptorTest {

    private val interceptor = CorrelationIdRequestInterceptor()

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `injeta header x-correlationId quando MDC esta populado`() {
        MDC.put("correlationId", "meu-correlation-id")
        val template = RequestTemplate()

        interceptor.apply(template)

        assertThat(template.headers()["x-correlationId"]).containsExactly("meu-correlation-id")
    }

    @Test
    fun `nao injeta header quando MDC esta nulo`() {
        val template = RequestTemplate()

        interceptor.apply(template)

        assertThat(template.headers()).doesNotContainKey("x-correlationId")
    }

    @Test
    fun `nao injeta header quando MDC contem string blank`() {
        MDC.put("correlationId", "   ")
        val template = RequestTemplate()

        interceptor.apply(template)

        assertThat(template.headers()).doesNotContainKey("x-correlationId")
    }
}
