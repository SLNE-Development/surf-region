package dev.slne.surf.region

import dev.slne.surf.region.handler.RegionHandler
import dev.slne.surf.region.manager.RegionManager
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import java.nio.file.Path
import java.util.*

open class RegionInstance(
    private val regionsFolder: Path
) {
    private val _regionManagers = mutableObjectSetOf<RegionManager>()
    val regionManagers get() = _regionManagers.freeze()

    private val _loadHandlers = mutableObjectListOf<RegionHandler>()
    private val _unloadHandlers = mutableObjectListOf<RegionHandler>()
    private val _saveHandlers = mutableObjectListOf<RegionHandler>()

    val loadHandlers get() = _loadHandlers.freeze()
    val unloadHandlers get() = _unloadHandlers.freeze()
    val saveHandlers get() = _saveHandlers.freeze()

    fun registerLoadHandler(handler: RegionHandler) {
        _loadHandlers.add(handler)
    }

    fun registerSaveHandler(handler: RegionHandler) {
        _saveHandlers.add(handler)
    }

    fun registerUnloadHandler(handler: RegionHandler) {
        _unloadHandlers.add(handler)
    }

    fun createRegionManager(worldId: UUID): RegionManager {
        val existing = findRegionManager(worldId)
        if (existing != null) return existing

        return RegionManager(worldId, regionsFolder, this).apply {
            _regionManagers.add(this)
        }
    }

    suspend fun unloadRegions(saveBeforeUnload: Boolean = true) {
        regionManagers.forEach { regionManager ->
            regionManager.regions.forEach { region ->
                regionManager.unloadRegion(region, saveBeforeUnload)
            }
        }
    }

    fun findRegionManager(worldId: UUID): RegionManager? =
        _regionManagers.firstOrNull { it.worldId == worldId }

    fun findOrCreateRegionManager(worldId: UUID): RegionManager {
        val existing = findRegionManager(worldId)
        if (existing != null) return existing

        return createRegionManager(worldId)
    }
}