package dev.slne.surf.region.paper.listener.listeners.essential

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.region.paper.PaperRegionInstance
import dev.slne.surf.surfapi.core.api.util.logger
import org.bukkit.Chunk
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

class RegionListeners(
    private val instance: PaperRegionInstance,
) : Listener {
    @EventHandler
    fun onChunkUnload(event: ChunkUnloadEvent) {
        val world = event.world
        val chunk = event.chunk
        val (regionX, regionZ) = chunkToRegion(chunk)
        val regionManager = instance.findRegionManager(world) ?: return

        instance.plugin.launch {
            val region = regionManager.getRegion(
                x = regionX,
                z = regionZ,
                loadIfNotLoaded = false,
                createIfNotExists = false
            ) ?: return@launch

            val loadedChunks = region.chunksLoaded.decrementAndGet()

            if (loadedChunks <= 0) {
                regionManager.unloadRegion(region, true)
            }
        }
    }

    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        val chunk = event.chunk
        val world = event.world
        val (regionX, regionZ) = chunkToRegion(chunk)

        val regionManager = instance.findOrCreateRegionManager(world)

        instance.plugin.launch {
            val region = regionManager.getRegion(regionX, regionZ) ?: return@launch

            region.chunksLoaded.incrementAndGet()
        }
    }

    private fun chunkToRegion(chunk: Chunk) =
        (chunk.x shr 5) to (chunk.z shr 5)

    companion object {
        private val log = logger()
    }
}