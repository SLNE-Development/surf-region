package dev.slne.surf.region

import dev.slne.surf.region.data.RegionData
import kotlinx.serialization.Serializable

/** Simple [RegionData] implementation used in tests. */
@Serializable
data class TestRegionData(val value: String) : RegionData
