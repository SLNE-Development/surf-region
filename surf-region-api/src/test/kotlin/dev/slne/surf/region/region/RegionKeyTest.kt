package dev.slne.surf.region.region

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RegionKeyTest {

    @Test
    fun `equals returns true for same coordinates`() {
        val a = RegionKey(2, -5)
        val b = RegionKey(2, -5)
        assertEquals(a, b)
    }

    @Test
    fun `equals returns false when x or z differs`() {
        val key = RegionKey(0, 0)
        assertNotEquals(key, RegionKey(1, 0))
        assertNotEquals(key, RegionKey(0, 1))
    }

    @Test
    fun `hashCode is consistent with equals`() {
        val a = RegionKey(3, 7)
        val b = RegionKey(3, 7)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `copy produces independent value`() {
        val original = RegionKey(1, 2)
        val copy = original.copy(x = 99)
        assertEquals(99, copy.x)
        assertEquals(2, copy.z)
        assertNotEquals(original, copy)
    }

    @Test
    fun `negative coordinates are preserved`() {
        val key = RegionKey(-100, -200)
        assertEquals(-100, key.x)
        assertEquals(-200, key.z)
    }

    @Test
    fun `can be used as map key`() {
        val map = mutableMapOf(RegionKey(0, 0) to "spawn_region")
        assertEquals("spawn_region", map[RegionKey(0, 0)])
    }
}
