package dev.slne.surf.region.paper

import dev.slne.surf.region.data.ModifiedBlocksData
import dev.slne.surf.region.data.RegionDataSerializer
import dev.slne.surf.region.manager.RegionManager
import dev.slne.surf.region.paper.listener.RegionBlockModificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin

/**
 * Main entry point for the surf-region Paper plugin.
 *
 * On enable, the plugin:
 * 1. Registers [ModifiedBlocksData] with [RegionDataSerializer] so it can be serialised.
 * 2. Creates a [RegionManager] backed by this plugin's data folder.
 * 3. Registers [RegionBlockModificationListener] to start tracking block changes.
 *
 * On disable, all loaded regions are saved to disk.
 */
class SurfRegionPlugin : JavaPlugin() {

    companion object {
        /** Singleton plugin instance – available after [onEnable]. */
        lateinit var instance: SurfRegionPlugin
            private set
    }

    /** Coroutine scope used for async region I/O. Cancelled in [onDisable]. */
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The region manager exposed to other plugins via the plugin instance. */
    lateinit var regionManager: RegionManager
        private set

    override fun onEnable() {
        instance = this

        // Register all RegionData implementations used by this plugin.
        RegionDataSerializer.register<ModifiedBlocksData>()

        regionManager = RegionManager(
            folder = dataFolder.toPath().resolve("regions"),
            scope = scope
        )

        server.pluginManager.registerEvents(
            RegionBlockModificationListener(regionManager, scope),
            this
        )

        logger.info("surf-region enabled – tracking block modifications.")
    }

    override fun onDisable() {
        runBlocking {
            regionManager.saveAll()
        }
        scope.cancel()
        logger.info("surf-region disabled – all regions saved.")
    }
}
