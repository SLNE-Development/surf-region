package dev.slne.surf.region.region

import dev.slne.surf.region.block.SurfBlock
import dev.slne.surf.region.chunk.SurfChunk
import dev.slne.surf.region.chunk.chunkKey
import dev.slne.surf.region.data.RegionDataSerializer
import dev.slne.surf.surfapi.core.api.util.mutableLong2ObjectMapOf
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class SurfRegion(
    val worldId: UUID,
    val x: Int,
    val z: Int,
    folder: Path
) {
    private val file = folder.resolve("r.$x.$z.json").toFile()
    private val ioMutex = Mutex()

    val chunksLoaded = AtomicInteger(0)
    private val chunks = Long2ObjectMaps.synchronize(mutableLong2ObjectMapOf<SurfChunk>())

    @Volatile
    var loaded: Boolean = false
        private set

    val isDirty: Boolean get() = chunks.values.any { it.isDirty }

    fun getBlockAt(x: Int, y: Int, z: Int): SurfBlock {
        val chunkX = x shr 4
        val chunkZ = z shr 4
        val chunk = getChunkAt(chunkX, chunkZ)

        return chunk.getBlockAt(x, y, z)
    }

    fun getChunkAt(x: Int, z: Int): SurfChunk {
        return chunks.getOrPut(chunkKey(x, z)) {
            SurfChunk(x, z)
        }
    }

    suspend fun load() = ioMutex.withLock {
        if (loaded) return@withLock

        if (!file.exists()) {
            loaded = true

            return@withLock
        }

        val content = withContext(Dispatchers.IO) { file.readText() }
        val deserialized = RegionDataSerializer.decode(content)

        chunks.clear()
        chunks.putAll(deserialized.associateBy { chunkKey(it.chunkX, it.chunkZ) })

        loaded = true
    }

    suspend fun save() = ioMutex.withLock {
        if (!loaded || !isDirty) return@withLock

        withContext(Dispatchers.IO) {
            file.parentFile.mkdirs()

            if (!file.exists()) {
                file.createNewFile()
            }

            file.writeText(RegionDataSerializer.encode(chunks.values.toList()))
        }

        chunks.forEach { (_, chunk) ->
            chunk.markClean()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SurfRegion

        if (x != other.x) return false
        if (z != other.z) return false
        if (worldId != other.worldId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + z
        result = 31 * result + worldId.hashCode()
        return result
    }
}