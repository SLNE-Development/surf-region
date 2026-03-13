package dev.slne.surf.region.region

import dev.slne.surf.region.block.SurfBlock
import dev.slne.surf.region.chunk.SurfChunk
import dev.slne.surf.region.data.RegionDataSerializer
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.*
import kotlin.io.path.readText

class SurfRegion(
    val worldId: UUID,
    val x: Int,
    val z: Int,
    private var folder: Path
) {
    private val file = folder.resolve("r.$x.$z.json").toFile()
    private val ioMutex = Mutex()
    private val chunks = mutableObjectListOf<SurfChunk>()

    @Volatile
    var loaded: Boolean = false
        private set

    val isDirty: Boolean get() = chunks.any { it.isDirty }

    fun getBlockAt(x: Int, y: Int, z: Int): SurfBlock {
        val chunkX = x shr 4
        val chunkZ = z shr 4
        val chunk = getChunkAt(chunkX, chunkZ)

        return chunk.getBlockAt(x, y, z)
    }

    fun getChunkAt(x: Int, z: Int): SurfChunk {
        val existing = chunks.find { it.chunkX == x && it.chunkZ == z }
        if (existing != null) return existing

        return SurfChunk(x, z).apply {
            chunks.add(this)
        }
    }

    suspend fun load() = ioMutex.withLock {
        if (loaded) return@withLock

        if (!file.exists()) {
            loaded = true

            return@withLock
        }

        val content = withContext(Dispatchers.IO) { folder.readText() }
        val deserialized = RegionDataSerializer.decode(content)

        chunks.clear()
        chunks.addAll(deserialized)

        loaded = true
    }

    suspend fun save() = ioMutex.withLock {
        if (loaded && !isDirty) return@withLock

        withContext(Dispatchers.IO) {
            file.parentFile.mkdirs()

            if (!file.exists()) {
                file.createNewFile()
            }

            file.writeText(RegionDataSerializer.encode(chunks))
        }

        chunks.forEach { chunk ->
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