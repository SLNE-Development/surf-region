package dev.slne.surf.region.data

import kotlinx.serialization.Serializable

/**
 * Tracks which block coordinates inside a region have been modified by players.
 *
 * This is stored under the key [MODIFIED_BLOCKS_KEY] inside a [dev.slne.surf.region.SurfRegion]
 * and is updated atomically via [dev.slne.surf.region.SurfRegion.markModified].
 *
 * Instances are treated as *immutable snapshots*; use [withModified] to derive an updated copy.
 */
@Serializable
data class ModifiedBlocksData(
    val modifiedBlocks: Set<BlockPosition> = emptySet()
) : RegionData {

    /** Returns `true` when the block at world coordinates ([x], [y], [z]) has been modified. */
    fun contains(x: Int, y: Int, z: Int): Boolean = BlockPosition(x, y, z) in modifiedBlocks

    /** Returns a new [ModifiedBlocksData] that includes ([x], [y], [z]) in the modified set. */
    fun withModified(x: Int, y: Int, z: Int): ModifiedBlocksData =
        copy(modifiedBlocks = modifiedBlocks + BlockPosition(x, y, z))

    companion object {
        const val MODIFIED_BLOCKS_KEY = "modifiedBlocks"
    }
}
