package com.abcworld.yongdrive.service

import com.abcworld.yongdrive.dto.request.CreateFolderRequest
import com.abcworld.yongdrive.dto.request.DeleteRequest
import com.abcworld.yongdrive.dto.request.GetObjectsRequest
import com.abcworld.yongdrive.dto.request.PasteRequest
import com.abcworld.yongdrive.entity.ObjectInfo
import com.abcworld.yongdrive.exception.ConflictException
import com.abcworld.yongdrive.repository.FileStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class ObjectService(
    private val storage: FileStorageRepository,
    private val cache: ObjectCacheService,
) {

    suspend fun list(request: GetObjectsRequest): List<ObjectInfo> =
        cache.get(request.bucket, request.path)
            ?: withContext(Dispatchers.IO) {
                storage.listObjects(request.bucket, request.path)
            }.also { objects ->
                cache.put(request.bucket, request.path, objects)
            }

    suspend fun createFolder(request: CreateFolderRequest) {
        val folderKey = "${request.path}${request.foldername}/"
        withContext(Dispatchers.IO) {
            if (storage.exists(request.bucket, folderKey)) {
                throw ConflictException("폴더가 이미 존재합니다.")
            }
            storage.createFolder(request.bucket, folderKey)
        }
        cache.invalidate(request.bucket, request.path)
    }

    suspend fun delete(request: DeleteRequest) {
        withContext(Dispatchers.IO) {
            val keys = request.objects.map { "${request.path}$it" }
            storage.delete(request.bucket, keys)
        }
        cache.invalidatePrefix(request.bucket, request.path)
    }

    suspend fun paste(request: PasteRequest) {
        withContext(Dispatchers.IO) {
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
        if (request.mode == PasteRequest.MODE_CUT) {
            cache.invalidate(request.bucket, request.pathFrom)
        }
        cache.invalidate(request.bucket, request.pathTo)
    }
}
