package dev.slne.surf.region.manager

import dev.slne.surf.region.RegionInstance
import dev.slne.surf.region.region.SurfRegion
import dev.slne.surf.surfapi.core.api.util.mutableLong2ObjectMapOf
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.nio.file.Path
import java.util.*

class RegionManager(
    val worldId: UUID,
    regionsFolder: Path,
    private val instance: RegionInstance
) {
    private val _regions = Long2ObjectMaps.synchronize(mutableLong2ObjectMapOf<SurfRegion>())
    val regions get() = _regions.values.toList()

    private val worldRegionsFolder = regionsFolder.resolve(worldId.toString())

    private fun regionKey(x: Int, z: Int) =
        (x.toLong() shl 32) or (z.toLong() and 0xffffffffL)

    suspend fun getRegion(
        x: Int,
        z: Int,
        loadIfNotLoaded: Boolean = true,
        createIfNotExists: Boolean = true,
    ) = getRegionPair(x, z, loadIfNotLoaded, createIfNotExists).first

    suspend fun getRegionPair(
        x: Int,
        z: Int,
        loadIfNotLoaded: Boolean = true,
        createIfNotExists: Boolean = true,
    ): Pair<SurfRegion?, Boolean> {
        var region = _regions[regionKey(x, z)]
        var created = false

        if (region == null) {
            if (!createIfNotExists) {
                return region to false
            }

            region = SurfRegion(worldId, x, z, instance).apply {
                _regions[regionKey(x, z)] = this
            }

            created = true
        }

        if (loadIfNotLoaded) {
            loadRegion(region)
        }

        return region to created
    }

    suspend fun loadRegion(region: SurfRegion) {
        if (!region.loaded) {
            region.load()

            instance.loadHandlers.forEach { handler ->
                handler.handle(region)
            }
        }
    }

    suspend fun saveRegion(region: SurfRegion) {
        region.save()

        instance.saveHandlers.forEach { handler ->
            handler.handle(region)
        }
    }

    suspend fun saveAll() = coroutineScope {
        _regions.values.map { region ->
            async {
                saveRegion(region)
            }
        }.awaitAll()
    }

    suspend fun unloadRegion(
        region: SurfRegion,
        saveBeforeUnload: Boolean = true,
    ) {
        if (region.loaded && saveBeforeUnload) {
            saveRegion(region)
        }

        _regions.remove(regionKey(region.x, region.z))

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
