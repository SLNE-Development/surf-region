package dev.slne.surf.region.paper.listener.listeners.modification

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.region.manager.RegionManager
import dev.slne.surf.region.paper.PaperRegionInstance
import dev.slne.surf.region.paper.utils.markBlockModified
import dev.slne.surf.region.paper.utils.markBlockUnmodified
import dev.slne.surf.region.region.SurfRegion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.event.Listener

open class ModificationListener(
    val instance: PaperRegionInstance
) : Listener {
    protected fun findRegionManager(world: World): RegionManager =
        instance.findOrCreateRegionManager(world.uid)

    protected fun launch(block: suspend CoroutineScope.() -> Unit) =
        instance.plugin.launch(block = block)

    private fun mark(block: Block, modification: (SurfRegion, Int, Int, Int) -> Unit) {
        val regionManager = findRegionManager(block.world)

        val worldX = block.x
        val worldY = block.y
        val worldZ = block.z

        // Integer arithmetic right-shift gives floor division for both positive and negative
        // block coordinates – required for regions in the negative quadrant of the world.
        val regionX = worldX shr 9
        val regionZ = worldZ shr 9

        instance.scope.launch {
            val region = regionManager.getRegion(
                x = regionX,
                z = regionZ,
                loadIfNotLoaded = true,
                createIfNotExists = true
            )
                ?: error("Region should have been loaded or created successfully at $regionX, $regionZ")

            modification(region, worldX, worldY, worldZ)
        }
    }

    protected fun markUnmodified(block: Block) = mark(block) { region, x, y, z ->
        region.markBlockUnmodified(x, y, z)
    }

    protected fun markModified(block: Block) = mark(block) { region, x, y, z ->
        region.markBlockModified(x, y, z)
    }
}