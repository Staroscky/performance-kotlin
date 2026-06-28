package com.staroscky.performance.core.cache

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class ValKeyConfig {

    @Bean
    fun redisTemplate(factory: RedisConnectionFactory, objectMapper: ObjectMapper): RedisTemplate<String, Any> {
        val redisMapper = objectMapper.copy().apply {
            activateDefaultTyping(
                polymorphicTypeValidator,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
            )
        }
        return RedisTemplate<String, Any>().apply {
            setConnectionFactory(factory)
            keySerializer = StringRedisSerializer()
            valueSerializer = GenericJackson2JsonRedisSerializer(redisMapper)
        }
    }
}
