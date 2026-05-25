package com.abcworld.yongdrive.service

import com.abcworld.yongdrive.repository.FileStorageRepository
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UploadService(
    private val storage: FileStorageRepository,
    private val cache: ObjectCacheService,
) {

    fun upload(
        bucket: String,
        path: String,
        filename: String,
        body: Flux<DataBuffer>,
    ): Mono<Void> {
        val key = "$path$filename"
        return storage.writeStream(bucket, key, body)
            .then(cache.invalidate(bucket, path))
            .then()
    }
}
