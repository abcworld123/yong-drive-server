package com.abcworld.yongdrive.service

import com.abcworld.yongdrive.entity.BucketInfo
import com.abcworld.yongdrive.repository.FileStorageRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class BucketService(
    private val storage: FileStorageRepository,
) {

    fun listBuckets(): Mono<List<BucketInfo>> =
        Mono.fromCallable { storage.listBuckets() }
            .subscribeOn(Schedulers.boundedElastic())
}
