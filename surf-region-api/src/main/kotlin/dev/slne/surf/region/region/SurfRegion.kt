package dev.slne.surf.region.region

import dev.slne.surf.region.block.SurfBlock
import dev.slne.surf.region.chunk.SurfChunk
import dev.slne.surf.region.data.RegionDataSerializer
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.mutableLong2ObjectMapOf
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class SurfRegion(
    val x: Int,
    val z: Int,
    private val regionFolder: Path
) {
    private val chunks = Long2ObjectMaps.synchronize(mutableLong2ObjectMapOf<SurfChunk>())
    private fun chunkKey(x: Int, z: Int) = (x.toLong() shl 32) or (z.toLong() and 0xffffffffL)

    private val file = regionFolder.resolve("r.$x.$z.json").toFile()
    private val ioMutex = Mutex()
    private val loadRetries = AtomicInteger(0)

    val chunksLoaded = AtomicInteger(0)

    @Volatile
    var loaded: Boolean = false
        private set

    val isDirty: Boolean get() = chunks.values.any { it.isDirty }

    suspend fun save() = ioMutex.withLock {
        if (!loaded || !isDirty) return@withLock

        withContext(Dispatchers.IO) {
            file.parentFile.mkdirs()

            if (!file.exists()) {
                file.createNewFile()
            }

            file.writeText(RegionDataSerializer.encode(chunks.values))
        }
    }

    suspend fun load() = ioMutex.withLock {
        if (loaded) return@withLock

        if (!file.exists()) {
            loaded = true
            return@withLock
        }

        val content = withContext(Dispatchers.IO) {
            file.readText()
        }

        val saveData = decodeData(content)

        chunks.clear()
        chunks.putAll(saveData.associateBy { chunkKey(it.chunkX, it.chunkZ) })

        loaded = true
    }

    private suspend fun decodeData(content: String): Collection<SurfChunk> {
        val retry = loadRetries.get()

        if (retry >= 3) {
            tryBackupFile()

            log.atSevere()
                .log("Failed to decode region data after 3 attempts, backup file created. Region: ($x, $z)")

            return emptyList()
        }
        try {
            return RegionDataSerializer.decode(content)
        } catch (exception: Exception) {
            log.atWarning()
                .withCause(exception)
                .log("Failed to decode region data on attempt ${retry + 1}, retrying... Region: ($x, $z)")

            loadRetries.incrementAndGet()

            return decodeData(content)
        }
    }

    private suspend fun tryBackupFile() = withContext(Dispatchers.IO) {
        val backupFile = regionFolder.resolve("r.$x.$z.json.bak").toFile()

        if (backupFile.exists()) {
            backupFile.delete()
        }

        file.copyTo(backupFile, overwrite = true)
    }

    fun getChunkAt(x: Int, z: Int): SurfChunk {
        val key = chunkKey(x, z)
        var chunk = chunks[key]

        if (chunk == null) {
            chunk = SurfChunk(x, z)
            chunks[key] = chunk
        }

        return chunk
    }

    fun getBlockAt(x: Int, y: Int, z: Int): SurfBlock {
        val chunkX = x shr 4
        val chunkZ = z shr 4
        val chunk = getChunkAt(chunkX, chunkZ)

        return chunk.getBlockAt(x, y, z)
    }

    companion object {
        private val log = logger()
    }
}