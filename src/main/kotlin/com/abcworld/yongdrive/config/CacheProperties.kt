package com.abcworld.yongdrive.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.cache")
data class CacheProperties(
    val objectListTtlSeconds: Long = 86_400L,
)
