package dev.slne.surf.region.chunk

import dev.slne.surf.region.block.SurfBlock
import dev.slne.surf.region.persistence.HasPersistentData
import dev.slne.surf.region.persistence.PersistentDataContainer
import kotlinx.serialization.Serializable

@Serializable
data class SurfChunk(
    val chunkX: Int,
    val chunkZ: Int,
    private val blocks: MutableList<SurfBlock> = mutableListOf(),
    override val persistentDataContainer: PersistentDataContainer = PersistentDataContainer()
) : HasPersistentData {
    override val isDirty: Boolean
        get() = persistentDataContainer.isDirty || blocks.any { it.isDirty }

    override fun markClean() {
        persistentDataContainer.markClean()
        blocks.forEach { it.markClean() }
    }

    fun toCoordinates() = (chunkX shl 4) to (chunkZ shl 4)

    fun getBlockAt(x: Int, y: Int, z: Int): SurfBlock {
        val block = blocks.find { it.x == x && it.y == y && it.z == z }
        if (block != null) return block

        return SurfBlock(x, y, z).apply {
            blocks.add(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SurfChunk

        if (chunkX != other.chunkX) return false
        if (chunkZ != other.chunkZ) return false
        if (blocks != other.blocks) return false

        return true
    }

    override fun hashCode(): Int {
        var result = chunkX
        result = 31 * result + chunkZ
        result = 31 * result + blocks.hashCode()
        return result
    }

    override fun toString(): String {
        return "SurfChunk(chunkX=$chunkX, chunkZ=$chunkZ, blocks=$blocks, persistentDataContainer=$persistentDataContainer)"
    }
}
