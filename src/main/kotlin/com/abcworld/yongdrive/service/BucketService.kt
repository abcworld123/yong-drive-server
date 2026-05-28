package com.abcworld.yongdrive.service

import com.abcworld.yongdrive.entity.BucketInfo
import com.abcworld.yongdrive.repository.FileStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class BucketService(
    private val storage: FileStorageRepository,
) {

    suspend fun listBuckets(): List<BucketInfo> =
        withContext(Dispatchers.IO) { storage.listBuckets() }
}
