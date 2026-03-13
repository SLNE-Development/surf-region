package dev.slne.surf.region.paper

import dev.slne.surf.region.data.RegionData
import dev.slne.surf.surfapi.core.api.messages.adventure.key
import kotlinx.serialization.Serializable

@Serializable
class ModifiedBlockData : RegionData {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return true
    }

    override fun hashCode(): Int {
        return 0
    }

    companion object {
        val MODIFIED_BLOCKS_KEY = key("surf:modified_blocks")
    }
}