package dev.slne.surf.region.chunk

import dev.slne.surf.region.TestRegionData
import net.kyori.adventure.key.Key
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ChunkSectionTest {

    private lateinit var section: ChunkSection
    private val KEY = Key.key("test", "key")

    @BeforeEach
    fun setUp() {
        section = ChunkSection()
    }

    // -------------------------------------------------------------------------
    // getOrCreateBlock
    // -------------------------------------------------------------------------

    @Test
    fun `getOrCreateBlock returns block with requested local coordinates`() {
        val block = section.getOrCreateBlock(3, 5, 7)
        assertEquals(3, block.x)
        assertEquals(5, block.y)
        assertEquals(7, block.z)
    }

    @Test
    fun `getOrCreateBlock returns same instance on repeated calls`() {
        val first = section.getOrCreateBlock(0, 0, 0)
        val second = section.getOrCreateBlock(0, 0, 0)
        assertSame(first, second)
    }

    @Test
    fun `getOrCreateBlock returns different instances for different coordinates`() {
        val a = section.getOrCreateBlock(0, 0, 0)
        val b = section.getOrCreateBlock(1, 0, 0)
        assertNotSame(a, b)
    }

    @Test
    fun `getOrCreateBlock at each corner of the 16x16x16 space`() {
        val corners = listOf(
            Triple(0, 0, 0), Triple(15, 0, 0),
            Triple(0, 15, 0), Triple(0, 0, 15),
            Triple(15, 15, 15),
        )
        val blocks = corners.map { (x, y, z) -> section.getOrCreateBlock(x, y, z) }
        // All different instances
        assertEquals(corners.size, blocks.toSet().size)
    }

    // -------------------------------------------------------------------------
    // isDirty / markClean
    // -------------------------------------------------------------------------

    @Test
    fun `isDirty is false on fresh section`() {
        assertFalse(section.isDirty)
    }

    @Test
    fun `creating a block marks section dirty`() {
        section.getOrCreateBlock(0, 0, 0)
        assertTrue(section.isDirty)
    }

    @Test
    fun `isDirty is true when a contained block is dirty`() {
        val block = section.getOrCreateBlock(1, 1, 1)
        section.markClean()
        assertFalse(section.isDirty)
        block.setData(KEY, TestRegionData("x"))
        assertTrue(section.isDirty)
    }

    @Test
    fun `markClean clears section dirty flag`() {
        section.getOrCreateBlock(0, 0, 0)
        assertTrue(section.isDirty)
        section.markClean()
        assertFalse(section.isDirty)
    }

    @Test
    fun `markClean also cleans contained blocks`() {
        val block = section.getOrCreateBlock(0, 0, 0)
        block.setData(KEY, TestRegionData("v"))
        assertTrue(block.isDirty)
        section.markClean()
        assertFalse(block.isDirty)
        assertFalse(section.isDirty)
    }

    // -------------------------------------------------------------------------
    // blocks list
    // -------------------------------------------------------------------------

    @Test
    fun `blocks list grows as new blocks are created`() {
        assertEquals(0, section.blocks.size)
        section.getOrCreateBlock(0, 0, 0)
        assertEquals(1, section.blocks.size)
        section.getOrCreateBlock(1, 0, 0)
        assertEquals(2, section.blocks.size)
    }

    @Test
    fun `existing block is not re-added to blocks list`() {
        section.getOrCreateBlock(5, 5, 5)
        section.getOrCreateBlock(5, 5, 5) // same coords
        assertEquals(1, section.blocks.size)
    }
}
