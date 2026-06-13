package com.abcworld.yongdrive.config

import com.abcworld.yongdrive.security.CidAuthenticationConverter
import com.abcworld.yongdrive.security.CidAuthenticationManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig {

    @Bean
    fun securityContextRepository(): ServerSecurityContextRepository =
        WebSessionServerSecurityContextRepository()

    @Bean
    fun authenticationEntryPoint(): ServerAuthenticationEntryPoint =
        HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)

    @Bean
    fun corsConfigurationSource(corsProperties: CorsProperties): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = corsProperties.allowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf(CorsConfiguration.ALL)
            exposedHeaders = listOf(HttpHeaders.CONTENT_DISPOSITION)
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }

    @Bean
    fun accessDeniedHandler(): ServerAccessDeniedHandler =
        ServerAccessDeniedHandler { exchange, _ ->
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            exchange.response.setComplete()
        }

    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity,
        securityContextRepository: ServerSecurityContextRepository,
        cidAuthenticationConverter: CidAuthenticationConverter,
        cidAuthenticationManager: CidAuthenticationManager,
        entryPoint: ServerAuthenticationEntryPoint,
        accessDeniedHandler: ServerAccessDeniedHandler,
        corsConfigurationSource: CorsConfigurationSource,
    ): SecurityWebFilterChain {
        val cidFilter = AuthenticationWebFilter(cidAuthenticationManager).apply {
            setServerAuthenticationConverter(cidAuthenticationConverter)
            setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            setAuthenticationFailureHandler { _, _ -> Mono.empty() }
        }

        return http
            .cors { it.configurationSource(corsConfigurationSource) }
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .securityContextRepository(securityContextRepository)
            .addFilterAt(cidFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .authorizeExchange { it.anyExchange().permitAll() }
            .exceptionHandling {
                it.authenticationEntryPoint(entryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }
            .build()
    }
}
