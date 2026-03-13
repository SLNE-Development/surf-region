package dev.slne.surf.region.region

import dev.slne.surf.region.RegionInstance
import dev.slne.surf.region.block.SurfBlock
import dev.slne.surf.region.chunk.SurfChunk
import dev.slne.surf.region.storage.RegionStorage
import dev.slne.surf.surfapi.core.api.util.mutableLong2ObjectMapOf
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class SurfRegion(
    val worldId: UUID,
    val x: Int,
    val z: Int,
    val instance: RegionInstance
) {
    private val regionStorage: RegionStorage<*> = instance.regionStorageSelector(this)
    private val chunks = Long2ObjectMaps.synchronize(mutableLong2ObjectMapOf<SurfChunk>())
    private fun chunkKey(x: Int, z: Int) = (x.toLong() shl 32) or (z.toLong() and 0xffffffffL)

    val regionFolder: Path get() = instance.regionsFolder.resolve(worldId.toString())
    val file: File get() = regionStorage.file

    val chunksLoaded = AtomicInteger(0)

    @Volatile
    var loaded: Boolean = false
        private set

    val isDirty: Boolean get() = chunks.values.any { it.isDirty }

    suspend fun save() {
        if (!loaded || !isDirty) return

        regionStorage.writeData(chunks.values)
    }

    suspend fun load() {
        if (loaded) return

        if (!file.exists()) {
            loaded = true
            return
        }

        val data = regionStorage.readData()

        chunks.clear()
        chunks.putAll(data.associateBy { chunkKey(it.chunkX, it.chunkZ) })

        loaded = true
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
}