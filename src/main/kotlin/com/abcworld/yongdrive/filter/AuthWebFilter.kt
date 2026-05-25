package com.abcworld.yongdrive.filter

import com.abcworld.yongdrive.service.AuthService
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class AuthWebFilter(
    private val authService: AuthService,
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.value()
        if (WHITELIST.any { path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        return exchange.session.flatMap { session ->
            val sessionLoggedIn = session.getAttribute<Boolean>(SESSION_ATTR_LOGIN) == true
            if (sessionLoggedIn) {
                chain.filter(exchange)
            } else {
                val cid = exchange.request.cookies.getFirst(COOKIE_CID)?.value
                authService.isValidCid(cid).flatMap { valid ->
                    if (valid) chain.filter(exchange) else unauthorized(exchange)
                }
            }
        }
    }

    private fun unauthorized(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
    }

    companion object {
        const val SESSION_ATTR_LOGIN = "isLogin"
        const val COOKIE_CID = "cid"
        private val WHITELIST = listOf("/auth/login", "/auth/check")
    }
}
