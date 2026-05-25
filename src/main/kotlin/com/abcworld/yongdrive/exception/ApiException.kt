package com.abcworld.yongdrive.exception

open class ApiException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
