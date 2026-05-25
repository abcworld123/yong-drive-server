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
import com.abcworld.yongdrive.util.PathUtils
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/s3/object")
class ObjectController(
    private val objectService: ObjectService,
    private val uploadService: UploadService,
    private val downloadService: DownloadService,
) {

    @PostMapping("/get")
    fun get(@RequestBody request: GetObjectsRequest): Mono<ApiResponse> =
        objectService.list(request).map { ApiResponse.objects(it) }

    @PostMapping("/create")
    fun create(@RequestBody request: CreateFolderRequest): Mono<ApiResponse> =
        objectService.createFolder(request).map { created ->
            if (created) ApiResponse.success()
            else ApiResponse.failure("폴더가 이미 존재합니다.")
        }

    @PostMapping("/delete")
    fun delete(@RequestBody request: DeleteRequest): Mono<ApiResponse> =
        objectService.delete(request).thenReturn(ApiResponse.success())

    @PostMapping("/paste")
    fun paste(@RequestBody request: PasteRequest): Mono<ApiResponse> =
        objectService.paste(request).thenReturn(ApiResponse.success())

    @PostMapping("/upload")
    fun upload(
        @RequestParam bucket: String,
        @RequestParam path: String,
        @RequestParam filename: String,
        @RequestBody body: Flux<DataBuffer>,
    ): Mono<ApiResponse> =
        uploadService.upload(bucket, path, filename, body)
            .thenReturn(ApiResponse.success())

    @PostMapping("/download")
    fun download(@RequestBody request: DownloadRequest): Mono<ResponseEntity<Flux<DataBuffer>>> {
        val filenames = request.filenames

        if (filenames.size == 1 && !PathUtils.isFolderKey(filenames[0])) {
            val filename = filenames[0]
            if (!downloadService.singleFileExists(request.bucket, request.path, filename)) {
                return Mono.just(ResponseEntity.notFound().build())
            }
            val size = downloadService.singleFileSize(request.bucket, request.path, filename)
            val stream = downloadService.singleFileStream(request.bucket, request.path, filename)
            val encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20")
            return Mono.just(
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$encodedName\"")
                    .header(HttpHeaders.CONTENT_LENGTH, size.toString())
                    .body(stream)
            )
        }

        val zipName = downloadService.resolveZipName(request.path, filenames)
        val encodedZipName = URLEncoder.encode(zipName, StandardCharsets.UTF_8).replace("+", "%20")

        val basePathAndNamesMono: Mono<Pair<String, List<String>>> =
            if (filenames.size == 1 && PathUtils.isFolderKey(filenames[0])) {
                val folder = filenames[0]
                val innerPath = "${request.path}$folder"
                objectService.list(GetObjectsRequest(bucket = request.bucket, path = innerPath))
                    .map { inner -> innerPath to inner.map { it.name } }
            } else {
                Mono.just(request.path to filenames)
            }

        return basePathAndNamesMono.map { (basePath, zipFilenames) ->
            val stream = downloadService.zipStream(request.bucket, basePath, zipFilenames)
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$encodedZipName.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream)
        }
    }
}
