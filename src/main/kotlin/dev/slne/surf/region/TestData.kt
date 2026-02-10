package dev.slne.surf.region

import dev.slne.surf.region.data.RegionData
import kotlinx.serialization.Serializable

@Serializable
data class TestData(
    val key: String,
    val value: String
) : RegionData {
    override fun toString(): String {
        return "TestData(key='$key', value='$value')"
    }
}