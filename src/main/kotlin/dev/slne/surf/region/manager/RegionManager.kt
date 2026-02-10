package dev.slne.surf.region.manager

import dev.slne.surf.region.SurfRegion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class RegionManager(
    private val folder: Path,
    private val scope: CoroutineScope
) {
    private val regions = ConcurrentHashMap<Pair<Int, Int>, SurfRegion>()

    fun getRegion(x: Int, z: Int): SurfRegion = regions.computeIfAbsent(Pair(x, z)) {
        val file = File(folder.toFile(), "r.$x.$z.json")

        SurfRegion(x, z, file)
    }

    suspend fun loadRegion(x: Int, z: Int): SurfRegion {
        val region = getRegion(x, z)

        region.load()

        return region
    }

    suspend fun saveRegion(x: Int, z: Int) {
        regions[Pair(x, z)]?.save()
    }

    suspend fun saveAll() = coroutineScope {
        regions.values.map {
            async {
                it.save()
            }
        }.awaitAll()
    }

    fun unloadRegion(x: Int, z: Int) {
        regions.remove(Pair(x, z))
    }
}