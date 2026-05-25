package com.abcworld.yongdrive.exception

import com.abcworld.yongdrive.dto.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<Void> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException): ResponseEntity<ApiResponse> =
        ResponseEntity.ok(ApiResponse.failure(ex.message))

    @ExceptionHandler(ApiException::class)
    fun handleApi(ex: ApiException): ResponseEntity<ApiResponse> {
        log.warn("ApiException: {}", ex.message)
        return ResponseEntity.ok(ApiResponse.failure(ex.message))
    }

    @ExceptionHandler(Exception::class)
    fun handleAny(ex: Exception): ResponseEntity<ApiResponse> {
        log.error("Unhandled error", ex)
        return ResponseEntity.ok(ApiResponse.failure())
    }
}
