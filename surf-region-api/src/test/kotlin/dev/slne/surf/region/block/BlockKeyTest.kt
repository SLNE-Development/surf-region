package dev.slne.surf.region.block

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BlockKeyTest {

    @Test
    fun `origin block returns zero`() {
        assertEquals(0L, blockKey(0, 0, 0))
    }

    @Test
    fun `same coordinates produce same key`() {
        assertEquals(blockKey(10, 64, -20), blockKey(10, 64, -20))
    }

    @Test
    fun `different x produces different key`() {
        assertNotEquals(blockKey(0, 64, 0), blockKey(1, 64, 0))
    }

    @Test
    fun `different y produces different key`() {
        assertNotEquals(blockKey(0, 64, 0), blockKey(0, 65, 0))
    }

    @Test
    fun `different z produces different key`() {
        assertNotEquals(blockKey(0, 64, 0), blockKey(0, 64, 1))
    }

    @Test
    fun `adjacent blocks have distinct keys`() {
        val keys = setOf(
            blockKey(0, 0, 0),
            blockKey(1, 0, 0),
            blockKey(0, 1, 0),
            blockKey(0, 0, 1),
            blockKey(-1, 0, 0),
            blockKey(0, -1, 0),
            blockKey(0, 0, -1),
        )
        assertEquals(7, keys.size)
    }

    @Test
    fun `large positive coordinates produce unique keys`() {
        // Two blocks that differ in only one axis at a large positive value
        assertNotEquals(blockKey(1_000_000, 255, 1_000_000), blockKey(1_000_001, 255, 1_000_000))
    }
}
