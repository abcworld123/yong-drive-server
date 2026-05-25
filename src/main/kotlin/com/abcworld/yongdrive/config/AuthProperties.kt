package com.abcworld.yongdrive.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.auth")
data class AuthProperties(
    val password: String = "",
    val cidTtlSeconds: Long = 2_592_000L,
    val cidLength: Int = 32,
)
