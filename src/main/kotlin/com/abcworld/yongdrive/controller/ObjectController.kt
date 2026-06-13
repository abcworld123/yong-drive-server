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
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.core.io.Resource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

@RestController
@RequestMapping("/object")
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

    /**
     * 브라우저 네이티브 다운로드(form submit)는 application/x-www-form-urlencoded로 들어온다.
     * WebFlux에서 @RequestParam은 쿼리 파라미터만 읽으므로, form 바디는 exchange.formData로 직접 읽는다.
     * filenames는 같은 이름의 반복 필드(filenames=a&filenames=b)로 받는다.
     */
    @PostMapping("/download")
    suspend fun download(exchange: ServerWebExchange): ResponseEntity<Resource> {
        val form = exchange.formData.awaitSingle()
        val request = DownloadRequest(
            bucket = form.getFirst("bucket").orEmpty(),
            path = form.getFirst("path").orEmpty(),
            filenames = form["filenames"] ?: emptyList(),
        )
        return downloadService.download(request)
    }
}
