package com.abcworld.yongdrive.service

import com.abcworld.yongdrive.dto.request.CreateFolderRequest
import com.abcworld.yongdrive.dto.request.DeleteRequest
import com.abcworld.yongdrive.dto.request.GetObjectsRequest
import com.abcworld.yongdrive.dto.request.PasteRequest
import com.abcworld.yongdrive.entity.ObjectInfo
import com.abcworld.yongdrive.repository.FileStorageRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class ObjectService(
    private val storage: FileStorageRepository,
    private val cache: ObjectCacheService,
) {

    fun list(request: GetObjectsRequest): Mono<List<ObjectInfo>> =
        cache.get(request.bucket, request.path)
            .switchIfEmpty(
                Mono.fromCallable { storage.listObjects(request.bucket, request.path) }
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap { objects ->
                        cache.put(request.bucket, request.path, objects)
                            .thenReturn(objects)
                    }
            )

    fun createFolder(request: CreateFolderRequest): Mono<Boolean> =
        Mono.fromCallable {
            val folderKey = "${request.path}${request.foldername}/"
            if (storage.exists(request.bucket, folderKey)) {
                false
            } else {
                storage.createFolder(request.bucket, folderKey)
                true
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { created ->
                cache.invalidate(request.bucket, request.path).thenReturn(created)
            }

    fun delete(request: DeleteRequest): Mono<Void> =
        Mono.fromRunnable<Void> {
            val keys = request.objects.map { "${request.path}$it" }
            storage.delete(request.bucket, keys)
        }
            .subscribeOn(Schedulers.boundedElastic())
            .then(cache.invalidatePrefix(request.bucket, request.path))
            .then()

    fun paste(request: PasteRequest): Mono<Void> =
        Mono.fromRunnable<Void> {
            for (key in request.objects) {
                val srcKey = "${request.pathFrom}$key"
                val dstKey = "${request.pathTo}$key"
                storage.copy(request.bucket, srcKey, dstKey)
            }
            if (request.mode == PasteRequest.MODE_CUT) {
                val srcKeys = request.objects.map { "${request.pathFrom}$it" }
                storage.delete(request.bucket, srcKeys)
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
            .then(
                if (request.mode == PasteRequest.MODE_CUT)
                    cache.invalidate(request.bucket, request.pathFrom)
                else Mono.empty()
            )
            .then(cache.invalidate(request.bucket, request.pathTo))
            .then()
}
