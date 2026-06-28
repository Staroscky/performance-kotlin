package com.staroscky.performance.core.filter

import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.UUID

class CorrelationIdFilterTest {

    private val filter = CorrelationIdFilter()
    private val response = MockHttpServletResponse()

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `popula MDC com valor exato do header quando presente`() {
        val request = MockHttpServletRequest().apply {
            addHeader(CorrelationIdFilter.HEADER_NAME, "test-correlation-id")
        }
        var capturedMdcValue: String? = null
        val chain = FilterChain { _, _ -> capturedMdcValue = MDC.get(CorrelationIdFilter.MDC_KEY) }

        filter.doFilter(request, response, chain)

        assertThat(capturedMdcValue).isEqualTo("test-correlation-id")
    }

    @Test
    fun `gera UUID valido no MDC quando header esta ausente`() {
        val request = MockHttpServletRequest()
        var capturedMdcValue: String? = null
        val chain = FilterChain { _, _ -> capturedMdcValue = MDC.get(CorrelationIdFilter.MDC_KEY) }

        filter.doFilter(request, response, chain)

        assertThat(capturedMdcValue).isNotBlank()
        assertThat(UUID.fromString(capturedMdcValue)).isNotNull()
    }

    @Test
    fun `limpa MDC no finally mesmo quando filterChain lanca excecao`() {
        val request = MockHttpServletRequest()
        val chain = mock<FilterChain> {
            on { doFilter(request, response) } doThrow RuntimeException("erro simulado")
        }

        runCatching { filter.doFilter(request, response, chain) }

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull()
    }
}
