package com.abcworld.yongdrive.service

import com.abcworld.yongdrive.repository.FileStorageRepository
import com.abcworld.yongdrive.util.PathUtils
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Service
class DownloadService(
    private val storage: FileStorageRepository,
) {

    fun singleFileStream(bucket: String, path: String, filename: String): Flux<DataBuffer> =
        storage.readStream(bucket, "$path$filename")

    fun singleFileSize(bucket: String, path: String, filename: String): Long =
        storage.size(bucket, "$path$filename")

    fun singleFileExists(bucket: String, path: String, filename: String): Boolean =
        storage.exists(bucket, "$path$filename")

    fun zipStream(bucket: String, basePath: String, filenames: List<String>): Flux<DataBuffer> {
        val factory = DefaultDataBufferFactory.sharedInstance
        val input = PipedInputStream(BUFFER_SIZE)
        val output = PipedOutputStream(input)

        Schedulers.boundedElastic().schedule {
            try {
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
            } catch (_: Exception) {
                runCatching { output.close() }
            }
        }

        return DataBufferUtils.readInputStream({ input }, factory, BUFFER_SIZE)
    }

    fun resolveZipName(path: String, filenames: List<String>): String {
        if (filenames.size == 1 && PathUtils.isFolderKey(filenames[0])) {
            return PathUtils.stripTrailingSlash(filenames[0])
        }
        if (path.isBlank()) return "index"
        val segments = path.split('/').filter { it.isNotEmpty() }
        return segments.lastOrNull() ?: "index"
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
    }
}
