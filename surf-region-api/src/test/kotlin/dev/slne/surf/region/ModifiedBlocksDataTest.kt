package dev.slne.surf.region

import dev.slne.surf.region.data.BlockPosition
import dev.slne.surf.region.data.ModifiedBlocksData
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModifiedBlocksDataTest {

    @Test
    fun `empty data contains no positions`() {
        val data = ModifiedBlocksData()
        assertFalse(data.contains(0, 0, 0))
    }

    @Test
    fun `withModified returns new instance with position added`() {
        val original = ModifiedBlocksData()
        val updated = original.withModified(1, 64, 1)

        // Original is unchanged (immutable)
        assertFalse(original.contains(1, 64, 1))
        // Updated contains the new entry
        assertTrue(updated.contains(1, 64, 1))
    }

    @Test
    fun `withModified accumulates multiple positions`() {
        val data = ModifiedBlocksData()
            .withModified(0, 0, 0)
            .withModified(1, 1, 1)
            .withModified(2, 2, 2)

        assertTrue(data.contains(0, 0, 0))
        assertTrue(data.contains(1, 1, 1))
        assertTrue(data.contains(2, 2, 2))
        assertFalse(data.contains(3, 3, 3))
    }

    @Test
    fun `withModified is idempotent for duplicate positions`() {
        val data = ModifiedBlocksData()
            .withModified(5, 64, 5)
            .withModified(5, 64, 5)

        assertTrue(data.contains(5, 64, 5))
        assertTrue(data.modifiedBlocks.size == 1)
    }

    @Test
    fun `contains distinguishes all three axes`() {
        val data = ModifiedBlocksData(setOf(BlockPosition(1, 2, 3)))
        assertTrue(data.contains(1, 2, 3))
        assertFalse(data.contains(0, 2, 3))
        assertFalse(data.contains(1, 0, 3))
        assertFalse(data.contains(1, 2, 0))
    }

    @Test
    fun `negative coordinates are handled correctly`() {
        val data = ModifiedBlocksData()
            .withModified(-512, -64, -512)

        assertTrue(data.contains(-512, -64, -512))
        assertFalse(data.contains(-511, -64, -512))
    }
}
