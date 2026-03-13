package dev.slne.surf.region.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BlockPositionTest {

    @Test
    fun `equals returns true for same coordinates`() {
        val a = BlockPosition(1, 64, -3)
        val b = BlockPosition(1, 64, -3)
        assertEquals(a, b)
    }

    @Test
    fun `equals returns false when any axis differs`() {
        val origin = BlockPosition(0, 0, 0)
        assertNotEquals(origin, BlockPosition(1, 0, 0))
        assertNotEquals(origin, BlockPosition(0, 1, 0))
        assertNotEquals(origin, BlockPosition(0, 0, 1))
    }

    @Test
    fun `hashCode is consistent with equals`() {
        val a = BlockPosition(10, 64, -10)
        val b = BlockPosition(10, 64, -10)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `copy produces independent value`() {
        val original = BlockPosition(5, 100, 5)
        val copy = original.copy(y = 200)
        assertEquals(5, copy.x)
        assertEquals(200, copy.y)
        assertEquals(5, copy.z)
        assertNotEquals(original, copy)
    }

    @Test
    fun `destructuring works`() {
        val pos = BlockPosition(7, 8, 9)
        val (x, y, z) = pos
        assertEquals(7, x)
        assertEquals(8, y)
        assertEquals(9, z)
    }

    @Test
    fun `negative coordinates are preserved`() {
        val pos = BlockPosition(-512, -64, -512)
        assertEquals(-512, pos.x)
        assertEquals(-64, pos.y)
        assertEquals(-512, pos.z)
    }

    @Test
    fun `can be used as set element for deduplication`() {
        val set = mutableSetOf(
            BlockPosition(1, 2, 3),
            BlockPosition(1, 2, 3),
            BlockPosition(4, 5, 6),
        )
        assertEquals(2, set.size)
        assertTrue(BlockPosition(1, 2, 3) in set)
        assertTrue(BlockPosition(4, 5, 6) in set)
    }

    @Test
    fun `can be used as map key`() {
        val map = mutableMapOf(BlockPosition(0, 64, 0) to "origin")
        assertEquals("origin", map[BlockPosition(0, 64, 0)])
    }
}
