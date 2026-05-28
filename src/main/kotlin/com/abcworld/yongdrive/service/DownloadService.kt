package com.abcworld.yongdrive.service

import com.abcworld.yongdrive.dto.request.DownloadRequest
import com.abcworld.yongdrive.dto.request.GetObjectsRequest
import com.abcworld.yongdrive.repository.FileStorageRepository
import com.abcworld.yongdrive.util.PathUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Service
class DownloadService(
    private val storage: FileStorageRepository,
    private val objectService: ObjectService,
) {

    suspend fun download(request: DownloadRequest): ResponseEntity<Flow<DataBuffer>> {
        val filenames = request.filenames
        if (filenames.size == 1 && !PathUtils.isFolderKey(filenames[0])) {
            return singleFileResponse(request.bucket, request.path, filenames[0])
        }
        return zipResponse(request.bucket, request.path, filenames)
    }

    private fun singleFileResponse(
        bucket: String,
        path: String,
        filename: String,
    ): ResponseEntity<Flow<DataBuffer>> {
        val key = "$path$filename"
        if (!storage.exists(bucket, key)) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${encodeFilename(filename)}\"")
            .header(HttpHeaders.CONTENT_LENGTH, storage.size(bucket, key).toString())
            .body(storage.readStream(bucket, key))
    }

    private suspend fun zipResponse(
        bucket: String,
        path: String,
        filenames: List<String>,
    ): ResponseEntity<Flow<DataBuffer>> {
        val (basePath, zipFilenames) = resolveZipSource(bucket, path, filenames)
        val zipName = resolveZipName(path, filenames)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${encodeFilename(zipName)}.zip\"")
            .contentType(MediaType.parseMediaType("application/zip"))
            .body(zipStream(bucket, basePath, zipFilenames))
    }

    private suspend fun resolveZipSource(
        bucket: String,
        path: String,
        filenames: List<String>,
    ): Pair<String, List<String>> {
        if (filenames.size == 1 && PathUtils.isFolderKey(filenames[0])) {
            val innerPath = "$path${filenames[0]}"
            val inner = objectService.list(GetObjectsRequest(bucket = bucket, path = innerPath))
            return innerPath to inner.map { it.name }
        }
        return path to filenames
    }

    private fun zipStream(bucket: String, basePath: String, filenames: List<String>): Flow<DataBuffer> {
        val factory = DefaultDataBufferFactory.sharedInstance
        val input = PipedInputStream(BUFFER_SIZE)
        val output = PipedOutputStream(input)

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                ZipOutputStream(output).use { zip ->
                    zip.setLevel(Deflater.BEST_SPEED)
                    for (name in filenames) {
                        val key = "$basePath$name"
                        if (PathUtils.isFolderKey(name)) {
                            val files = storage.walkFiles(bucket, key)
                            for ((relative, _) in files) {
                                val entryName = "$name$relative"
                                zip.putNextEntry(ZipEntry(entryName))
                                storage.copyInputStreamTo(bucket, "$key$relative", zip)
                                zip.closeEntry()
                            }
                        } else {
                            if (!storage.exists(bucket, key)) continue
                            zip.putNextEntry(ZipEntry(name))
                            storage.copyInputStreamTo(bucket, key, zip)
                            zip.closeEntry()
                        }
                    }
                }
            }.onFailure { runCatching { output.close() } }
        }

        return DataBufferUtils.readInputStream({ input }, factory, BUFFER_SIZE).asFlow()
    }

    private fun resolveZipName(path: String, filenames: List<String>): String {
        if (filenames.size == 1 && PathUtils.isFolderKey(filenames[0])) {
            return PathUtils.stripTrailingSlash(filenames[0])
        }
        if (path.isBlank()) return "index"
        val segments = path.split('/').filter { it.isNotEmpty() }
        return segments.lastOrNull() ?: "index"
    }

    private fun encodeFilename(name: String): String =
        URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20")

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
    }
}
