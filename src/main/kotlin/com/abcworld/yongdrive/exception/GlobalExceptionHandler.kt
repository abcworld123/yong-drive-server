package com.abcworld.yongdrive.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.server.ServerWebExchange

/**
 * RFC 9457 ProblemDetail(application/problem+json) 기반 에러 응답.
 *
 * ResponseEntityExceptionHandler를 상속하면 잘못된 요청 바디(ServerWebInputException) 등
 * 프레임워크 예외도 적절한 상태코드의 ProblemDetail로 변환된다. 아래 핸들러는 앱 고유 예외만 처리.
 */
@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.message ?: "Unauthorized")
    }

    // @PreAuthorize("isAuthenticated()") 거부 = 로그인 안 한 정상 흐름 → 로그 없이 401.
    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAccessDenied(ex: AuthorizationDeniedException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required")
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.message ?: "Conflict")
    }

    @ExceptionHandler(ApiException::class)
    fun handleApi(ex: ApiException): ProblemDetail {
        log.warn("ApiException: {}", ex.message)
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Bad request")
    }

    @ExceptionHandler(Exception::class)
    suspend fun handleAny(ex: Exception, exchange: ServerWebExchange): ProblemDetail? {
        // 분할/이어받기 다운로드에서 클라이언트(IDM 등)가 세그먼트 연결을 끊으면 sendfile이
        // "Connection reset by peer"로 실패한다. 응답은 이미 커밋된 상태라 에러 본문을 쓸 수 없고
        // (정상 상황), ProblemDetail을 반환하면 오히려 인코딩 에러가 난다 → 조용히 무시(null).
        if (exchange.response.isCommitted || isClientDisconnect(ex)) {
            log.debug("Response not writable (client disconnect / already committed): {}", ex.message)
            return null
        }
        // 예상치 못한 예외: 내부 메시지를 노출하지 않고 로그만 남긴다.
        log.error("Unhandled error", ex)
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error")
    }

    private fun isClientDisconnect(ex: Throwable): Boolean {
        val message = ex.message ?: return false
        return "Connection reset by peer" in message || "Broken pipe" in message
    }
}
