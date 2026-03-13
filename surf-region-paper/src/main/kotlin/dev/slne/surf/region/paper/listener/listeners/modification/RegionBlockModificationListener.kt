package dev.slne.surf.region.paper.listener.listeners.modification

import dev.slne.surf.region.paper.PaperRegionInstance
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent

class RegionBlockModificationListener(
    instance: PaperRegionInstance,
) : ModificationListener(instance) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        markModified(event.block)
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        markUnmodified(event.block)
    }
}