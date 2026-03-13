package dev.slne.surf.region.chunk

import dev.slne.surf.region.block.SurfBlock
import dev.slne.surf.region.persistence.HasPersistentData
import dev.slne.surf.region.persistence.PersistentDataContainer
import kotlinx.serialization.Serializable

@Serializable
data class SurfChunk(
    val chunkX: Int,
    val chunkZ: Int,
) : HasPersistentData {
    override val persistentDataContainer: PersistentDataContainer = PersistentDataContainer()
    private val sections = arrayOfNulls<ChunkSection>(24)

    override val isDirty: Boolean
        get() = persistentDataContainer.isDirty || sections.any { it?.isDirty == true }

    override fun markClean() {
        persistentDataContainer.markClean()
        sections.forEach { it?.markClean() }
    }

    fun getBlockAt(x: Int, y: Int, z: Int): SurfBlock {
        val sectionIndex = y shr 4
        val sectionY = y and 15

        var section = sections[sectionIndex]

        if (section == null) {
            section = ChunkSection()
            sections[sectionIndex] = section
        }

        return section.getOrCreateBlock(
            x and 15,
            sectionY,
            z and 15
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SurfChunk

        if (chunkX != other.chunkX) return false
        if (chunkZ != other.chunkZ) return false
        if (!sections.contentEquals(other.sections)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = chunkX
        result = 31 * result + chunkZ
        result = 31 * result + sections.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "SurfChunk(chunkX=$chunkX, chunkZ=$chunkZ, persistentDataContainer=$persistentDataContainer, sections=${sections.contentToString()}, isDirty=$isDirty)"
    }
}
