package dev.slne.surf.region.chunk

import dev.slne.surf.region.block.SurfBlock

class ChunkSection {
    private val blocks = arrayOfNulls<SurfBlock>(4096)
    private var dirty = false

    val isDirty get() = dirty || blocks.any { it?.isDirty == true }

    fun getBlock(x: Int, y: Int, z: Int): SurfBlock? {
        return blocks[index(x, y, z)]
    }

    fun getOrCreateBlock(x: Int, y: Int, z: Int): SurfBlock {
        val index = index(x, y, z)
        var block = blocks[index]

        if (block == null) {
            block = SurfBlock(x, y, z)
            blocks[index] = block
            dirty = true
        }

        return block
    }

    private fun index(x: Int, y: Int, z: Int) =
        (y shl 8) or (z shl 4) or x

    fun markClean() {
        dirty = false
        blocks.forEach { it?.markClean() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChunkSection

        if (dirty != other.dirty) return false
        if (!blocks.contentEquals(other.blocks)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dirty.hashCode()
        result = 31 * result + blocks.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "ChunkSection(isDirty=$isDirty, blocks=${blocks.contentToString()})"
    }
}