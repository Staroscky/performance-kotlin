package com.staroscky.performance.core.cache

import org.cache2k.extra.spring.SpringCache2kCacheManager
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class Cache2kConfig {

    @Bean
    fun cacheManager(): CacheManager = SpringCache2kCacheManager()
        .addCaches({ it.name("instituicoes").eternal(false).expireAfterWrite(24, TimeUnit.HOURS) })
}
