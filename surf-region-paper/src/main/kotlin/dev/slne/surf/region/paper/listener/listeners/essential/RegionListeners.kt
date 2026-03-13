package dev.slne.surf.region.paper.listener.listeners.essential

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.region.paper.PaperRegionInstance
import dev.slne.surf.surfapi.core.api.util.logger
import kotlinx.coroutines.withContext
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

class RegionListeners(
    private val instance: PaperRegionInstance,
) : Listener {
    @EventHandler
    fun onChunkUnload(event: ChunkUnloadEvent) {
        val chunk = event.chunk
        val (regionX, regionZ) = chunkToRegion(chunk)
        val chunkCoordinates = getRegionChunkCoordinates(regionX, regionZ)

        instance.plugin.launch {
            val world = event.world
            val anyChunkLoaded = anyChunkLoaded(world, chunkCoordinates)
            log.atInfo().log("Trying to unload region at ${regionX}, $regionZ")

            if (anyChunkLoaded) {
                log.atInfo()
                    .log("Not unloading region at ${regionX}, $regionZ because at least one chunk is still loaded")
                return@launch
            }

            val regionManager = instance.findRegionManager(world) ?: return@launch

            regionManager.unloadRegion(regionX, regionZ, true)

            log.atInfo().log("Region unloaded successfully")
        }
    }

    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        val chunk = event.chunk
        val world = event.world
        val (regionX, regionZ) = chunkToRegion(chunk)

        val regionManager = instance.findOrCreateRegionManager(world)

        instance.plugin.launch {
            regionManager.loadRegion(regionX, regionZ)
            log.atInfo().log("Trying to load region at ${regionX}, $regionZ")
        }
    }

    private suspend fun anyChunkLoaded(
        world: World,
        chunkCoordinates: List<Pair<Int, Int>>
    ): Boolean = withContext(instance.plugin.globalRegionDispatcher) {
        var loaded = false

        chunkCoordinates.forEach { (chunkX, chunkZ) ->
            val chunk = world.getChunkAt(chunkX, chunkZ)

            loaded = loaded || chunk.isLoaded
        }

        return@withContext loaded
    }

    private fun chunkToRegion(chunk: Chunk) =
        (chunk.x shr 5) to (chunk.z shr 5)

    private fun getRegionChunkCoordinates(regionX: Int, regionZ: Int): List<Pair<Int, Int>> {
        val chunkCoordinates = mutableListOf<Pair<Int, Int>>()

        val startX = regionX * 32
        val startZ = regionZ * 32

        for (x in startX until startX + 32) {
            for (z in startZ until startZ + 32) {
                chunkCoordinates.add(x to z)
            }
        }

        return chunkCoordinates
    }

    companion object {
        private val log = logger()
    }
}