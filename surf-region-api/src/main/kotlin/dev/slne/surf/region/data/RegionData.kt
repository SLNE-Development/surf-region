package dev.slne.surf.region.data

import kotlinx.serialization.Polymorphic

@Polymorphic
interface RegionData {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}
