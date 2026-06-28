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
