package com.staroscky.performance.core.caronte

import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestMapping

@Component
class CaronteRegistry(ctx: ApplicationContext) {

    val items: List<CaronteItem> = ctx
        .getBeansWithAnnotation(CaronteMapping::class.java)
        .values
        .mapNotNull { bean ->
            val clazz = AopUtils.getTargetClass(bean)
            val rel = clazz.getAnnotation(CaronteMapping::class.java)?.rel
                ?: return@mapNotNull null
            val href = clazz.getAnnotation(RequestMapping::class.java)?.value?.firstOrNull()
                ?: return@mapNotNull null
            CaronteItem(rel = rel, href = href)
        }
        .sortedBy { it.rel }
        .also { log.info("CaronteRegistry: {} itens registrados → {}", it.size, it.map { i -> i.rel }) }

    companion object {
        private val log = LoggerFactory.getLogger(CaronteRegistry::class.java)
    }
}
