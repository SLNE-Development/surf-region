package dev.slne.surf.region.paper

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.folia.scope
import dev.slne.surf.region.RegionInstance
import dev.slne.surf.region.data.RegionDataSerializer
import dev.slne.surf.region.manager.RegionManager
import dev.slne.surf.region.paper.listener.events.SurfRegionLoadEvent
import dev.slne.surf.region.paper.listener.events.SurfRegionSaveEvent
import dev.slne.surf.region.paper.listener.events.SurfRegionUnloadEvent
import dev.slne.surf.region.paper.listener.listeners.essential.RegionListeners
import dev.slne.surf.region.paper.listener.listeners.modification.RegionBlockModificationListener
import dev.slne.surf.region.region.SurfRegion
import dev.slne.surf.region.storage.JsonRegionStorage
import dev.slne.surf.region.storage.RegionStorage
import dev.slne.surf.surfapi.bukkit.api.event.register
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import kotlinx.coroutines.CoroutineScope
import org.bukkit.World
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import java.nio.file.Path

class PaperRegionInstance(
    val plugin: SuspendingJavaPlugin,
    regionsFolder: Path = plugin.dataPath.resolve("regions"),
    private val registerModificationListeners: Boolean = true,
    regionStorageSelector: (SurfRegion) -> RegionStorage<*> = { region ->
        JsonRegionStorage(region)
    }
) : RegionInstance(regionsFolder, regionStorageSelector) {
    private val listeners = mutableObjectListOf<Listener>()
    val scope: CoroutineScope by lazy { plugin.scope }

    suspend fun onLoad() {
        RegionDataSerializer.register<ModifiedBlocksData>()
    }

    suspend fun onEnable() {
        registerListeners()
    }

    suspend fun onDisable() {
        unloadRegions(true)
        unregisterListeners()
    }

    private fun registerRegionHandlers() {
        registerLoadHandler { region ->
            SurfRegionLoadEvent(region).callEvent()
        }

        registerUnloadHandler { region ->
            SurfRegionUnloadEvent(region).callEvent()
        }

        registerSaveHandler { region ->
            SurfRegionSaveEvent(region).callEvent()
        }
    }

    private fun registerListeners() {
        if (registerModificationListeners) {
            registerModificationListeners()
        }

        registerEssentialListeners()

        listeners.forEach { listener ->
            listener.register()
        }

        registerRegionHandlers()
    }

    private fun unregisterListeners() {
        listeners.forEach { listener ->
            HandlerList.unregisterAll(listener)
        }
    }

    private fun registerEssentialListeners() {
        listeners.add(RegionListeners(this))
    }

    private fun registerModificationListeners() {
        listeners.add(RegionBlockModificationListener(this))
    }

    fun findRegionManager(world: World): RegionManager? =
        findRegionManager(world.uid)

    fun findOrCreateRegionManager(world: World): RegionManager =
        findOrCreateRegionManager(world.uid)
}