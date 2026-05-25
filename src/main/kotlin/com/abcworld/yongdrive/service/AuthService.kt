package com.abcworld.yongdrive.service

import com.abcworld.yongdrive.config.AuthProperties
import com.abcworld.yongdrive.util.CidGenerator
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class AuthService(
    private val redis: ReactiveStringRedisTemplate,
    private val authProperties: AuthProperties,
    private val passwordEncoder: PasswordEncoder,
    private val cidGenerator: CidGenerator,
) {

    fun verifyPassword(raw: String): Boolean {
        val stored = authProperties.password
        if (stored.isBlank()) return false
        return passwordEncoder.matches(raw, stored)
    }

    fun issueCid(): Mono<String> {
        val cid = cidGenerator.generate(authProperties.cidLength)
        return redis.opsForValue()
            .set(cid, "", Duration.ofSeconds(authProperties.cidTtlSeconds))
            .thenReturn(cid)
    }

    fun isValidCid(cid: String?): Mono<Boolean> {
        if (cid.isNullOrBlank()) return Mono.just(false)
        return redis.hasKey(cid)
    }
}
