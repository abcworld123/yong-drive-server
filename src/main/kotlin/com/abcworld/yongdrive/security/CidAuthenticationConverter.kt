package com.abcworld.yongdrive.security

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class CidAuthenticationConverter : ServerAuthenticationConverter {

    override fun convert(exchange: ServerWebExchange): Mono<Authentication> = mono {
        val alreadyAuthenticated = ReactiveSecurityContextHolder.getContext()
            .awaitSingleOrNull()?.authentication?.isAuthenticated == true
        if (alreadyAuthenticated) return@mono null
        val cid = exchange.request.cookies.getFirst(SecurityConstants.COOKIE_CID)?.value
        if (cid.isNullOrBlank()) return@mono null
        PreAuthenticatedAuthenticationToken(SecurityConstants.CID_PRINCIPAL, cid)
    }
}
