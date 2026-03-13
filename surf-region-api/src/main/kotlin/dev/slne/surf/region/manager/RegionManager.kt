package dev.slne.surf.region.manager

import dev.slne.surf.region.RegionInstance
import dev.slne.surf.region.region.SurfRegion
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class RegionManager(
    val worldId: UUID,
    regionsFolder: Path,
    private val instance: RegionInstance
) {
    private val _regions = ConcurrentHashMap.newKeySet<SurfRegion>()
    val regions get() = _regions.toList()

    private val worldRegionsFolder = regionsFolder.resolve(worldId.toString())

    suspend fun getRegion(
        x: Int,
        z: Int,
        loadIfNotLoaded: Boolean = true,
    ) = getOrCreateRegion(x, z, loadIfNotLoaded).first

    suspend fun getOrCreateRegion(
        x: Int,
        z: Int,
        loadIfNotLoaded: Boolean = true,
    ): Pair<SurfRegion, Boolean> {
        val region = _regions.find { it.x == x && it.z == z }

        if (region != null) {
            if (!region.loaded && loadIfNotLoaded) {
                region.load()
            }

            return region to false
        }

        return SurfRegion(worldId, x, z, worldRegionsFolder).apply {
            _regions.add(this)

            if (loadIfNotLoaded) {
                load()
            }
        } to true
    }

    suspend fun loadRegion(x: Int, z: Int): SurfRegion = loadOrCreateRegion(x, z).first

    suspend fun loadOrCreateRegion(x: Int, z: Int): Pair<SurfRegion, Boolean> {
        val (region, created) = getOrCreateRegion(x, z, false)

        if (!region.loaded && !created) {
            region.load()

            instance.loadHandlers.forEach { handler ->
                handler.handle(region)
            }
        }

        return region to created
    }

    suspend fun saveRegion(x: Int, z: Int) {
        val region = getRegion(x, z, loadIfNotLoaded = false)

        region.save()

        instance.saveHandlers.forEach { handler ->
            handler.handle(region)
        }
    }

    suspend fun saveAll() = coroutineScope {
        _regions.map {
            async {
                saveRegion(it.x, it.z)
            }
        }.awaitAll()

        Unit
    }

    suspend fun unloadRegion(
        x: Int,
        z: Int,
        saveBeforeUnload: Boolean = true,
    ) {
        val region = _regions.find { it.x == x && it.z == z } ?: return

        if (region.loaded && saveBeforeUnload) {
            saveRegion(x, z)
        }

        _regions.remove(region)

        instance.unloadHandlers.forEach { handler ->
            handler.handle(region)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RegionManager

        return worldId == other.worldId
    }

    override fun hashCode(): Int {
        return worldId.hashCode()
    }

    override fun toString(): String {
        return "RegionManager(worldId=$worldId, regions=$_regions, worldRegionsFolder=$worldRegionsFolder)"
    }
}
