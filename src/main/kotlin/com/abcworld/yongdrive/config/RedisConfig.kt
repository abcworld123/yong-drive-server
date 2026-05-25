package com.abcworld.yongdrive.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate

@Configuration
class RedisConfig {

    @Bean
    fun reactiveStringRedisTemplate(
        factory: ReactiveRedisConnectionFactory,
    ): ReactiveStringRedisTemplate = ReactiveStringRedisTemplate(factory)
}
