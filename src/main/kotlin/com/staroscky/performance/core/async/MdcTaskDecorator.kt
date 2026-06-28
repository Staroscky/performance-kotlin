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
