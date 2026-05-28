package com.abcworld.yongdrive.service

import com.abcworld.yongdrive.repository.FileStorageRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service

@Service
class UploadService(
    private val storage: FileStorageRepository,
    private val cache: ObjectCacheService,
) {

    suspend fun upload(
        bucket: String,
        path: String,
        filename: String,
        body: Flow<DataBuffer>,
    ) {
        val key = "$path$filename"
        storage.writeStream(bucket, key, body)
        cache.invalidate(bucket, path)
    }
}
