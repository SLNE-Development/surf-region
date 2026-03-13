package dev.slne.surf.region.paper

import dev.slne.surf.region.data.RegionData
import dev.slne.surf.region.utils.BlockPosition
import dev.slne.surf.surfapi.core.api.messages.adventure.key
import kotlinx.serialization.Serializable

@Serializable
data class ModifiedBlocksData(
    val modifiedBlocks: Set<BlockPosition> = emptySet()
) : RegionData {
    fun contains(x: Int, y: Int, z: Int): Boolean = BlockPosition(x, y, z) in modifiedBlocks

    fun withModified(x: Int, y: Int, z: Int): ModifiedBlocksData =
        copy(modifiedBlocks = modifiedBlocks + BlockPosition(x, y, z))

    fun withUnmodified(x: Int, y: Int, z: Int): ModifiedBlocksData =
        copy(modifiedBlocks = modifiedBlocks - BlockPosition(x, y, z))

    companion object {
        val MODIFIED_BLOCKS_KEY = key("surf:modified_blocks")
    }
}