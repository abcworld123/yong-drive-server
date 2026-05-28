package com.abcworld.yongdrive.security

import com.abcworld.yongdrive.service.AuthService
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class CidAuthenticationManager(
    private val authService: AuthService,
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> = mono {
        val cid = authentication.credentials as? String ?: return@mono null
        if (cid.isBlank()) return@mono null
        val valid = authService.isValidCid(cid)
        if (valid) {
            val authorities = listOf(SimpleGrantedAuthority(SecurityConstants.ROLE_USER))
            PreAuthenticatedAuthenticationToken(
                SecurityConstants.CID_PRINCIPAL,
                cid,
                authorities,
            )
        } else {
            throw BadCredentialsException("Invalid cid")
        }
    }
}
