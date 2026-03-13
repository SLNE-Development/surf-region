package dev.slne.surf.region.manager

import dev.slne.surf.region.SurfRegion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the lifecycle (load / save / cache) of [SurfRegion] instances.
 *
 * Regions are keyed by their *region coordinates* (not block coordinates). To convert world
 * block coordinates to region coordinates use integer division by 512, or equivalently a
 * right-shift by 9 bits (`blockX shr 9`, `blockZ shr 9`).
 */
class RegionManager(
    val folder: Path,
    private val scope: CoroutineScope
) {
    private val regions = ConcurrentHashMap<Pair<Int, Int>, SurfRegion>()

    /** Returns the cached [SurfRegion] for ([x], [z]) or creates an empty placeholder. */
    fun getRegion(x: Int, z: Int): SurfRegion = regions.computeIfAbsent(Pair(x, z)) {
        val file = File(folder.toFile(), "r.$x.$z.json")
        SurfRegion(x, z, file)
    }

    /**
     * Returns the [SurfRegion] for ([x], [z]), loading it from disk if not yet loaded.
     *
     * Idempotent: if the region is already loaded the disk read is skipped.
     */
    suspend fun loadRegion(x: Int, z: Int): SurfRegion {
        val region = getRegion(x, z)
        if (!region.loaded) {
            region.load()
        }
        return region
    }

    /** Persists the region at ([x], [z]) if it exists in the cache. */
    suspend fun saveRegion(x: Int, z: Int) {
        regions[Pair(x, z)]?.save()
    }

    /** Concurrently persists all cached regions. */
    suspend fun saveAll() = coroutineScope {
        regions.values.map {
            async {
                it.save()
            }
        }.awaitAll()
    }

    /** Removes the region at ([x], [z]) from the in-memory cache. */
    fun unloadRegion(x: Int, z: Int) {
        regions.remove(Pair(x, z))
    }
}
