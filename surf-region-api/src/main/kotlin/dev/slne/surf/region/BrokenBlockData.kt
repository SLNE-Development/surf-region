package dev.slne.surf.region

import dev.slne.surf.region.data.RegionData
import kotlinx.serialization.Serializable

@Serializable
data class BrokenBlockData(
    val brokenBlocks: List<Triple<Int, Int, Int>>
) : RegionData
