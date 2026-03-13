package dev.slne.surf.region.paper.listener.listeners.modification.listeners

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.region.paper.PaperRegionInstance
import dev.slne.surf.region.paper.listener.listeners.modification.ModificationListener
import dev.slne.surf.region.paper.utils.isModified
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockPistonEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent

class PistonListener(
    instance: PaperRegionInstance,
) : ModificationListener(instance) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPistonExtend(event: BlockPistonExtendEvent) {
        instance.plugin.launch {
            event.handlePiston(event.blocks)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPistonRetract(event: BlockPistonRetractEvent) {
        instance.plugin.launch {
            event.handlePiston(event.blocks)
        }
    }

    private suspend fun BlockPistonEvent.handlePiston(blocks: List<Block>) {
        val direction = direction
        val states = blocks.map {
            val beforeModified = it.isModified(instance)
            val afterBlock = it.getRelative(direction)

            PistonBlockState(
                beforeBlock = it,
                afterBlock = afterBlock,
                beforeModified = beforeModified,
            )
        }

        launch {
            states.forEach { state ->
                if (state.beforeModified) {
                    markModified(state.afterBlock)
                } else {
                    markUnmodified(state.afterBlock)
                }
            }
        }
    }

    data class PistonBlockState(
        val beforeBlock: Block,
        val afterBlock: Block,
        val beforeModified: Boolean,
    )
}