package dev.slne.surf.region.paper.listener.listeners.modification

import dev.slne.surf.region.manager.RegionManager
import dev.slne.surf.region.paper.PaperRegionInstance
import dev.slne.surf.region.paper.utils.markBlockModified
import kotlinx.coroutines.launch
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent

class RegionBlockModificationListener(
    private val instance: PaperRegionInstance,
) : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        markModified(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        markModified(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBurn(event: BlockBurnEvent) {
        markModified(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().forEach { markModified(it) }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        event.blockList().forEach { markModified(it) }
    }

    private fun findRegionManager(world: World): RegionManager =
        instance.findOrCreateRegionManager(world.uid)

    private fun markModified(block: Block) {
        val regionManager = findRegionManager(block.world)

        val worldX = block.x
        val worldY = block.y
        val worldZ = block.z

        // Integer arithmetic right-shift gives floor division for both positive and negative
        // block coordinates – required for regions in the negative quadrant of the world.
        val regionX = worldX shr 9
        val regionZ = worldZ shr 9

        instance.scope.launch {
            val region = regionManager.loadRegion(regionX, regionZ)

            region.markBlockModified(worldX, worldY, worldZ)
        }
    }
}