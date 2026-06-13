package com.abcworld.yongdrive.controller

import com.abcworld.yongdrive.dto.request.LoginRequest
import com.abcworld.yongdrive.dto.response.ApiResponse
import com.abcworld.yongdrive.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.CurrentSecurityContext
import org.springframework.security.core.context.SecurityContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {
    @GetMapping("/check")
    fun check(@CurrentSecurityContext context: SecurityContext?): ApiResponse =
        ApiResponse(success = context?.authentication?.isAuthenticated == true)

    @PostMapping("/login")
    suspend fun login(
        @RequestBody request: LoginRequest,
        exchange: ServerWebExchange,
    ): ResponseEntity<ApiResponse> {
        return authService.login(request, exchange)
    }
}
