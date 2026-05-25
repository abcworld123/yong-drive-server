package com.abcworld.yongdrive.controller

import com.abcworld.yongdrive.config.AuthProperties
import com.abcworld.yongdrive.dto.request.LoginRequest
import com.abcworld.yongdrive.dto.response.ApiResponse
import com.abcworld.yongdrive.filter.AuthWebFilter
import com.abcworld.yongdrive.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val authProperties: AuthProperties,
) {

    @GetMapping("/check")
    fun check(exchange: ServerWebExchange): Mono<ApiResponse> =
        exchange.session.flatMap { session ->
            if (session.getAttribute<Boolean>(AuthWebFilter.SESSION_ATTR_LOGIN) == true) {
                Mono.just(ApiResponse.success())
            } else {
                val cid = exchange.request.cookies.getFirst(AuthWebFilter.COOKIE_CID)?.value
                authService.isValidCid(cid).map { ApiResponse(success = it) }
            }
        }

    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<ApiResponse>> {
        if (!authService.verifyPassword(request.pw)) {
            return Mono.just(ResponseEntity.ok(ApiResponse(success = false)))
        }
        return exchange.session.flatMap { session ->
            session.attributes[AuthWebFilter.SESSION_ATTR_LOGIN] = true
            authService.issueCid().map { cid ->
                val cookie = ResponseCookie.from(AuthWebFilter.COOKIE_CID, cid)
                    .maxAge(Duration.ofSeconds(authProperties.cidTtlSeconds))
                    .path("/")
                    .httpOnly(true)
                    .build()
                ResponseEntity.status(HttpStatus.OK)
                    .header("Set-Cookie", cookie.toString())
                    .body(ApiResponse.success())
            }
        }
    }
}
