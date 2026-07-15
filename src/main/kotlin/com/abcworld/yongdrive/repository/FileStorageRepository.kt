package com.abcworld.yongdrive.repository

import com.abcworld.yongdrive.config.StorageProperties
import com.abcworld.yongdrive.entity.BucketInfo
import com.abcworld.yongdrive.entity.ObjectInfo
import com.abcworld.yongdrive.util.PathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.stereotype.Repository
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readAttributes

@Repository
class FileStorageRepository(
    private val storageProperties: StorageProperties,
) {

    fun listBuckets(): List<BucketInfo> {
        val root = PathUtils.storageRoot(storageProperties.root)
        if (!root.exists()) return emptyList()
        return Files.list(root).use { stream ->
            stream
                .filter { it.isDirectory() }
                .map { path ->
                    val attrs = path.readAttributes<BasicFileAttributes>()
                    BucketInfo(
                        name = path.name,
                        creationDate = attrs.creationTime().toInstant(),
                    )
                }
                .sorted(compareBy({ it.name }))
                .toList()
        }
    }

    fun listObjects(bucket: String, prefix: String): List<ObjectInfo> {
        val dir = PathUtils.resolveSafe(storageProperties.root, bucket, prefix)
        if (!dir.exists() || !dir.isDirectory()) return emptyList()
        return Files.list(dir).use { stream ->
            stream.map { path ->
                if (path.isDirectory()) {
                    ObjectInfo(type = ObjectInfo.TYPE_FOLDER, name = "${path.name}/")
                } else {
                    ObjectInfo(type = ObjectInfo.TYPE_FILE, name = path.name, size = path.fileSize())
                }
            }.sorted(compareBy({ if (it.type == ObjectInfo.TYPE_FOLDER) 0 else 1 }, { it.name })).toList()
        }
    }

    fun exists(bucket: String, key: String): Boolean {
        val path = PathUtils.resolveSafe(storageProperties.root, bucket, key)
        return path.exists()
    }

    fun createFolder(bucket: String, key: String): Boolean {
        val folderKey = if (key.endsWith("/")) key else "$key/"
        val path = PathUtils.resolveSafe(storageProperties.root, bucket, folderKey)
        if (path.exists()) return false
        Files.createDirectories(path)
        return true
    }

    fun resolveExisting(bucket: String, key: String): Path? {
        val path = PathUtils.resolveSafe(storageProperties.root, bucket, key)
        return if (path.exists()) path else null
    }

    /**
     * 업로드 스트림을 파일로 쓴다. DataBufferUtils.write(AsynchronousFileChannel 기반)는
     * 청크(~8KB)마다 request(1) → 스레드 풀 hop을 반복해서, 느린 CPU(라즈베리파이 A72)에서는
     * 청크당 왕복 지연이 처리량을 지배한다(실측 ~13MB/s). Flow 브리지가 이미 청크를
     * 프리페치하므로 Dispatchers.IO에서 FileChannel에 순차 blocking write 하는 쪽이 빠르다.
     */
    suspend fun writeStream(bucket: String, key: String, body: Flow<DataBuffer>) {
        val path = withContext(Dispatchers.IO) {
            val p = PathUtils.resolveSafe(storageProperties.root, bucket, key)
            Files.createDirectories(p.parent)
            p
        }
        withContext(Dispatchers.IO) {
            FileChannel.open(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            ).use { channel ->
                body.collect { dataBuffer ->
                    try {
                        dataBuffer.readableByteBuffers().use { buffers ->
                            for (buffer in buffers) {
                                while (buffer.hasRemaining()) {
                                    channel.write(buffer)
                                }
                            }
                        }
                    } finally {
                        DataBufferUtils.release(dataBuffer)
                    }
                }
            }
        }
    }

    /**
     * 다운로드용 파일을 Resource로 연다. FileSystemResource를 반환하면 WebFlux의
     * ResourceHttpMessageWriter가 Content-Length / Accept-Ranges / Range(206) 를 자동 처리한다.
     * 일반 파일이 아니면(폴더·없음) null.
     */
    fun openFile(bucket: String, key: String): Resource? {
        val path = PathUtils.resolveSafe(storageProperties.root, bucket, key)
        return if (path.isRegularFile()) FileSystemResource(path) else null
    }

    fun copyInputStreamTo(bucket: String, key: String, out: OutputStream) {
        val path = PathUtils.resolveSafe(storageProperties.root, bucket, key)
        Files.newInputStream(path).use { it.copyTo(out) }
    }

    fun delete(bucket: String, keys: List<String>) {
        for (key in keys) {
            val path = PathUtils.resolveSafe(storageProperties.root, bucket, key)
            if (!path.exists()) continue
            if (path.isDirectory()) deleteRecursive(path) else Files.deleteIfExists(path)
        }
    }

    fun copy(bucket: String, srcKey: String, dstKey: String) {
        val src = PathUtils.resolveSafe(storageProperties.root, bucket, srcKey)
        val dst = PathUtils.resolveSafe(storageProperties.root, bucket, dstKey)
        if (!src.exists()) return
        if (src.isDirectory()) {
            copyRecursive(src, dst)
        } else {
            Files.createDirectories(dst.parent)
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun walkFiles(bucket: String, key: String): List<Pair<String, Path>> {
        val basePath = PathUtils.resolveSafe(storageProperties.root, bucket, key)
        if (!basePath.exists()) return emptyList()
        val result = mutableListOf<Pair<String, Path>>()
        Files.walk(basePath).use { stream ->
            stream.filter { it.isRegularFile() }.forEach { file ->
                val relative = basePath.relativize(file).toString().replace('\\', '/')
                result += relative to file
            }
        }
        return result
    }

    private fun deleteRecursive(path: Path) {
        Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    private fun copyRecursive(src: Path, dst: Path) {
        Files.walk(src).use { stream ->
            stream.forEach { source ->
                val target = dst.resolve(src.relativize(source))
                if (source.isDirectory()) {
                    Files.createDirectories(target)
                } else {
                    Files.createDirectories(target.parent)
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
}
