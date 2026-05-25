package com.abcworld.yongdrive.util

import com.abcworld.yongdrive.exception.ApiException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths

object PathUtils {

    fun storageRoot(rootProperty: String): Path =
        Paths.get(rootProperty).toAbsolutePath().normalize()

    fun bucketRoot(rootProperty: String, bucket: String): Path {
        require(bucket.isNotBlank()) { "bucket must not be blank" }
        require(!bucket.contains('/') && !bucket.contains('\\') && bucket != "." && bucket != "..") {
            "invalid bucket name: $bucket"
        }
        return storageRoot(rootProperty).resolve(bucket).normalize()
    }

    fun resolveSafe(rootProperty: String, bucket: String, key: String): Path {
        val bucketRoot = bucketRoot(rootProperty, bucket)
        val decoded = decode(key)
        val sanitized = decoded.trimStart('/')
        val resolved = bucketRoot.resolve(sanitized).normalize()
        if (!resolved.startsWith(bucketRoot) && resolved != bucketRoot) {
            throw ApiException("invalid path: $key")
        }
        return resolved
    }

    fun decode(value: String): String =
        try {
            URLDecoder.decode(value, StandardCharsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            value
        }

    fun isFolderKey(key: String): Boolean = key.endsWith("/")

    fun stripTrailingSlash(value: String): String =
        if (value.endsWith("/")) value.dropLast(1) else value
}
