package com.staroscky.performance

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
@EnableCaching
@ConfigurationPropertiesScan
class PerformanceApplication

fun main(args: Array<String>) {
    runApplication<PerformanceApplication>(*args)
}
