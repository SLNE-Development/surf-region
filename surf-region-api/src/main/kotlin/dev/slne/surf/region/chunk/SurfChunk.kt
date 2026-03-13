package dev.slne.surf.region.chunk

import dev.slne.surf.region.block.SurfBlock
import dev.slne.surf.region.persistence.HasPersistentData
import dev.slne.surf.region.persistence.PersistentDataContainer
import dev.slne.surf.surfapi.core.api.util.mutableInt2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.toObjectList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class SurfChunk(
    val chunkX: Int,
    val chunkZ: Int,
) : HasPersistentData {
    override val persistentDataContainer: PersistentDataContainer = PersistentDataContainer()

    @SerialName("sections")
    private val _sections = mutableListOf<ChunkSection>()
    val sections get() = _sections.toObjectList()

    @Transient
    private val sectionMap = mutableInt2ObjectMapOf<ChunkSection>()

    override val isDirty: Boolean
        get() = persistentDataContainer.isDirty || _sections.any { it.isDirty }

    override fun markClean() {
        persistentDataContainer.markClean()
        _sections.forEach { it.markClean() }
    }

    fun getSection(yIndex: Int): ChunkSection {
        return sectionMap.getOrPut(yIndex) {
            ChunkSection().apply {
                _sections.add(this)
            }
        }
    }

    fun getBlockAt(x: Int, y: Int, z: Int): SurfBlock {
        val sectionIndex = y shr 4
        val sectionY = y and 15

        val section = getSection(sectionIndex)

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
        if (sections != other.sections) return false

        return true
    }

    override fun hashCode(): Int {
        var result = chunkX
        result = 31 * result + chunkZ
        result = 31 * result + sections.hashCode()
        return result
    }

    override fun toString(): String {
        return "SurfChunk(chunkX=$chunkX, chunkZ=$chunkZ, persistentDataContainer=$persistentDataContainer, sections=${
            _sections.joinToString(", ")
        }, isDirty=$isDirty)"
    }
}
