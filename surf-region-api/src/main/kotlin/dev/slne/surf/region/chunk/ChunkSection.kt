package dev.slne.surf.region.chunk

import dev.slne.surf.region.block.SurfBlock
import dev.slne.surf.surfapi.core.api.util.mutableInt2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.toObjectList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class ChunkSection {
    @SerialName("blocks")
    private val _blocks = mutableListOf<SurfBlock>()
    val blocks get() = _blocks.toObjectList()

    @Transient
    private val blocksMap = mutableInt2ObjectMapOf<SurfBlock>()

    @Transient
    private var dirty = false
    val isDirty get() = dirty || _blocks.any { it.isDirty }

    fun getOrCreateBlock(
        x: Int, y: Int, z: Int,
        worldX: Int, worldY: Int, worldZ: Int,
    ): SurfBlock {
        val key = x + (z shl 4) + (y shl 8)

        return blocksMap.getOrPut(key) {
            SurfBlock(worldX, worldY, worldZ).apply {
                _blocks.add(this)
                dirty = true
            }
        }
    }

    fun markClean() {
        dirty = false
        blocks.forEach { it.markClean() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChunkSection

        if (dirty != other.dirty) return false
        if (blocks != other.blocks) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dirty.hashCode()
        result = 31 * result + blocks.hashCode()
        return result
    }

    override fun toString(): String {
        return "ChunkSection(isDirty=$isDirty, blocks=${blocks.joinToString(", ")})"
    }
}