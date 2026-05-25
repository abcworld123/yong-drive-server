package com.abcworld.yongdrive.service

import com.abcworld.yongdrive.config.CacheProperties
import com.abcworld.yongdrive.entity.ObjectInfo
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class ObjectCacheService(
    private val redis: ReactiveStringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val cacheProperties: CacheProperties,
) {

    fun get(bucket: String, path: String): Mono<List<ObjectInfo>> =
        redis.opsForValue().get(key(bucket, path))
            .map { objectMapper.readValue<List<ObjectInfo>>(it) }

    fun put(bucket: String, path: String, objects: List<ObjectInfo>): Mono<Boolean> {
        val json = objectMapper.writeValueAsString(objects)
        return redis.opsForValue().set(
            key(bucket, path),
            json,
            Duration.ofSeconds(cacheProperties.objectListTtlSeconds),
        )
    }

    fun invalidate(bucket: String, path: String): Mono<Long> =
        redis.delete(key(bucket, path))

    fun invalidatePrefix(bucket: String, pathPrefix: String): Mono<Long> {
        val pattern = "${key(bucket, pathPrefix)}*"
        return redis.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())
            .collectList()
            .flatMap { keys ->
                if (keys.isEmpty()) Mono.just(0L) else redis.delete(Flux.fromIterable(keys))
            }
    }

    private fun key(bucket: String, path: String): String = "$bucket/$path"
}
