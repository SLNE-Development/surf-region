package dev.slne.surf.region.storage

import dev.slne.surf.region.chunk.SurfChunk
import dev.slne.surf.region.region.SurfRegion
import dev.slne.surf.surfapi.core.api.util.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

abstract class RegionStorage<D>(
    protected val region: SurfRegion,
    fileExtension: String,
    private val maxRetries: Int = 3,
) {
    protected val regionFolder get() = region.regionFolder

    val file: File = regionFolder
        .resolve("r.${region.x}.${region.z}.$fileExtension")
        .toFile()

    private val ioMutex = Mutex()

    private val readRetries = AtomicInteger(0)
    private val writeRetries = AtomicInteger(0)

    protected abstract fun encode(data: Collection<SurfChunk>): D
    protected abstract fun decode(data: D): Collection<SurfChunk>

    protected abstract suspend fun write(data: D)
    protected abstract suspend fun read(): D

    private suspend fun ensureFileExists() = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
    }

    suspend fun writeData(data: Collection<SurfChunk>) = ioMutex.withLock {
        retry(writeRetries) {
            ensureFileExists()

            val encodedData = encode(data)

            write(encodedData)
        }
    }

    suspend fun readData(): Collection<SurfChunk> = ioMutex.withLock {
        return@withLock retry(readRetries) {
            val data = read()

            decode(data)
        } ?: emptyList()
    }

    private suspend fun <T> retry(
        retries: AtomicInteger,
        operation: suspend () -> T,
    ): T? {
        while (true) {
            try {
                return operation().apply {
                    retries.set(0)
                }
            } catch (exception: Exception) {
                if (retries.getAndIncrement() >= maxRetries) {
                    log.atSevere()
                        .withCause(exception)
                        .log("Error while performing region storage operation")

                    backupFile()

                    return null
                }
            }
        }
    }

    private suspend fun backupFile() = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext

        val backupFile = regionFolder.resolve("${file.nameWithoutExtension}.bak").toFile()

        file.copyTo(backupFile, overwrite = true)
    }

    companion object {
        private val log = logger()
    }
}