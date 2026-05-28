package com.abcworld.yongdrive.controller

import com.abcworld.yongdrive.dto.request.CreateFolderRequest
import com.abcworld.yongdrive.dto.request.DeleteRequest
import com.abcworld.yongdrive.dto.request.DownloadRequest
import com.abcworld.yongdrive.dto.request.GetObjectsRequest
import com.abcworld.yongdrive.dto.request.PasteRequest
import com.abcworld.yongdrive.dto.response.ApiResponse
import com.abcworld.yongdrive.service.DownloadService
import com.abcworld.yongdrive.service.ObjectService
import com.abcworld.yongdrive.service.UploadService
import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/s3/object")
@PreAuthorize("isAuthenticated()")
class ObjectController(
    private val objectService: ObjectService,
    private val uploadService: UploadService,
    private val downloadService: DownloadService,
) {

    @PostMapping("/get")
    suspend fun get(@RequestBody request: GetObjectsRequest): ApiResponse =
        ApiResponse.objects(objectService.list(request))

    @PostMapping("/create")
    suspend fun create(@RequestBody request: CreateFolderRequest): ApiResponse {
        objectService.createFolder(request)
        return ApiResponse.success()
    }

    @PostMapping("/delete")
    suspend fun delete(@RequestBody request: DeleteRequest): ApiResponse {
        objectService.delete(request)
        return ApiResponse.success()
    }

    @PostMapping("/paste")
    suspend fun paste(@RequestBody request: PasteRequest): ApiResponse {
        objectService.paste(request)
        return ApiResponse.success()
    }

    @PostMapping("/upload")
    suspend fun upload(
        @RequestParam bucket: String,
        @RequestParam path: String,
        @RequestParam filename: String,
        @RequestBody body: Flow<DataBuffer>,
    ): ApiResponse {
        uploadService.upload(bucket, path, filename, body)
        return ApiResponse.success()
    }

    @PostMapping("/download")
    suspend fun download(@RequestBody request: DownloadRequest): ResponseEntity<Flow<DataBuffer>> =
        downloadService.download(request)
}
