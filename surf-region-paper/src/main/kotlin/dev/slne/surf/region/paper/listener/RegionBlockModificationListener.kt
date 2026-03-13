package dev.slne.surf.region.paper.listener

import dev.slne.surf.region.manager.RegionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent

/**
 * Listens to all in-game events that **physically modify a block** and records the affected
 * block coordinates in the corresponding [dev.slne.surf.region.SurfRegion].
 *
 * ## Minecraft region coordinates
 * A region covers a 512 × 512 block area. The region coordinate is derived from world block
 * coordinates with a right-shift of 9 bits:
 * ```
 * regionX = blockX shr 9   // equivalent to Math.floorDiv(blockX, 512)
 * regionZ = blockZ shr 9
 * ```
 *
 * ## mcmmo coverage
 * mcmmo's super-ability block breaks (TreeFeller, SuperBreaker, GigaDrillBreaker, …) are fired
 * through `com.gmail.nossr50.events.fake.FakeBlockBreakEvent` which **extends**
 * `org.bukkit.event.block.BlockBreakEvent`. Therefore, the [BlockBreakEvent] handler below
 * captures all mcmmo ability breaks without requiring an explicit mcmmo dependency.
 *
 * Other mcmmo skill events:
 * - Mining / Woodcutting / Excavation abilities → [BlockBreakEvent] (via `FakeBlockBreakEvent`)
 * - Herbalism (Green Thumb, plant re-growth) → [BlockPlaceEvent]
 * - Fishing / Combat / Acrobatics → no block modification
 * - Repair / Salvage / Alchemy → no world-block modification
 *
 * ## Event priority
 * All handlers use [EventPriority.MONITOR] with `ignoreCancelled = true` so we only record
 * modifications that **actually happened** (i.e. were not vetoed by a protection plugin).
 */
class RegionBlockModificationListener(
    private val regionManager: RegionManager,
    private val scope: CoroutineScope
) : Listener {

    // -------------------------------------------------------------------------
    // Block break – covers normal breaks AND all mcmmo super-ability breaks
    // (FakeBlockBreakEvent extends BlockBreakEvent)
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        scheduleMarkModified(event.block)
    }

    // -------------------------------------------------------------------------
    // Block place – covers normal placements AND mcmmo Herbalism (Green Thumb)
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        scheduleMarkModified(event.block)
    }

    // -------------------------------------------------------------------------
    // Fire spreading / burning blocks
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBurn(event: BlockBurnEvent) {
        scheduleMarkModified(event.block)
    }

    // -------------------------------------------------------------------------
    // Block-based explosions (TNT, bed in Nether/End, respawn anchor)
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().forEach { scheduleMarkModified(it) }
    }

    // -------------------------------------------------------------------------
    // Entity-triggered explosions (Creeper, charged Creeper, TNT entity,
    // Wither, Wither Skull, End Crystal, …)
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        event.blockList().forEach { scheduleMarkModified(it) }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Converts [block]'s world coordinates to region coordinates and asynchronously marks
     * the block as modified in the corresponding [dev.slne.surf.region.SurfRegion].
     *
     * The region is loaded on first access; subsequent calls for the same region are cheap
     * because [RegionManager.loadRegion] is idempotent.
     */
    private fun scheduleMarkModified(block: Block) {
        val worldX = block.x
        val worldY = block.y
        val worldZ = block.z

        // Integer arithmetic right-shift gives floor division for both positive and negative
        // block coordinates – required for regions in the negative quadrant of the world.
        val regionX = worldX shr 9
        val regionZ = worldZ shr 9

        scope.launch {
            val region = regionManager.loadRegion(regionX, regionZ)
            region.markModified(worldX, worldY, worldZ)
        }
    }
}
