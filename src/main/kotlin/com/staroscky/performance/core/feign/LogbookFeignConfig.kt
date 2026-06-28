package com.staroscky.performance.core.feign

import feign.Logger
import org.zalando.logbook.Logbook
import org.zalando.logbook.openfeign.FeignLogbookLogger

class LogbookFeignConfig(private val logbook: Logbook) {
    fun feignLogger(): Logger = FeignLogbookLogger(logbook)
}
