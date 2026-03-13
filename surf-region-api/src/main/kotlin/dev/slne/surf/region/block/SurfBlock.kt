package dev.slne.surf.region.block

import dev.slne.surf.region.persistence.HasPersistentData
import dev.slne.surf.region.persistence.PersistentDataContainer
import kotlinx.serialization.Serializable

@Serializable
data class SurfBlock(
    val x: Int,
    val y: Int,
    val z: Int,
    override val persistentDataContainer: PersistentDataContainer = PersistentDataContainer()
) : HasPersistentData {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SurfBlock

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }

    override fun toString(): String {
        return "SurfBlock(x=$x, y=$y, z=$z, persistentDataContainer=$persistentDataContainer)"
    }
}
