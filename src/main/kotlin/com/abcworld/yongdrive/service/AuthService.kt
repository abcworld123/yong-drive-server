package com.abcworld.yongdrive.service

import com.abcworld.yongdrive.config.AuthProperties
import com.abcworld.yongdrive.dto.request.LoginRequest
import com.abcworld.yongdrive.dto.response.ApiResponse
import com.abcworld.yongdrive.security.SecurityConstants
import com.abcworld.yongdrive.util.CidGenerator
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Duration

@Service
class AuthService(
    private val redis: ReactiveStringRedisTemplate,
    private val authProperties: AuthProperties,
    private val passwordEncoder: PasswordEncoder,
    private val cidGenerator: CidGenerator,
    private val securityContextRepository: ServerSecurityContextRepository,
) {

    suspend fun login(request: LoginRequest, exchange: ServerWebExchange): ResponseEntity<ApiResponse> {
        if (!verifyPassword(request.pw)) {
            return ResponseEntity.ok(ApiResponse(success = false))
        }
        val authentication = UsernamePasswordAuthenticationToken(
            SecurityConstants.SESSION_PRINCIPAL,
            null,
            listOf(SimpleGrantedAuthority(SecurityConstants.ROLE_USER)),
        )
        securityContextRepository.save(exchange, SecurityContextImpl(authentication)).awaitSingleOrNull()
        val cookie = buildCidCookie(issueCid())
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(ApiResponse.success())
    }

    suspend fun isValidCid(cid: String?): Boolean {
        if (cid.isNullOrBlank()) return false
        return redis.hasKey(cid).awaitSingle()
    }

    private fun verifyPassword(raw: String): Boolean {
        val stored = authProperties.password
        if (stored.isBlank()) return false
        return passwordEncoder.matches(raw, stored)
    }

    private suspend fun issueCid(): String {
        val cid = cidGenerator.generate(authProperties.cidLength)
        redis.opsForValue()
            .set(cid, "", Duration.ofSeconds(authProperties.cidTtlSeconds))
            .awaitSingleOrNull()
        return cid
    }

    private fun buildCidCookie(cid: String): ResponseCookie =
        ResponseCookie.from(SecurityConstants.COOKIE_CID, cid)
            .maxAge(Duration.ofSeconds(authProperties.cidTtlSeconds))
            .path("/")
            .httpOnly(true)
            .build()
}
