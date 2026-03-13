package dev.slne.surf.region.chunk

import dev.slne.surf.region.TestRegionData
import net.kyori.adventure.key.Key
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SurfChunkTest {

    private lateinit var chunk: SurfChunk
    private val KEY = Key.key("test", "key")

    @BeforeEach
    fun setUp() {
        chunk = SurfChunk(0, 0)
    }

    // -------------------------------------------------------------------------
    // getSection
    // -------------------------------------------------------------------------

    @Test
    fun `getSection creates section for new y-index`() {
        assertNotNull(chunk.getSection(0))
        assertNotNull(chunk.getSection(15))
    }

    @Test
    fun `getSection returns same section on repeated calls`() {
        val first = chunk.getSection(4)
        val second = chunk.getSection(4)
        assertSame(first, second)
    }

    @Test
    fun `getSection returns different sections for different y-indices`() {
        val s0 = chunk.getSection(0)
        val s1 = chunk.getSection(1)
        assert(s0 !== s1)
    }

    // -------------------------------------------------------------------------
    // getBlockAt — coordinate translation
    // -------------------------------------------------------------------------

    @Test
    fun `getBlockAt stores world-local coordinates in block`() {
        // The chunk is at (chunkX=0, chunkZ=0), so world block (7, 64, 11)
        // local within chunk is also (7, 64, 11)
        val block = chunk.getBlockAt(7, 64, 11)
        // Section local: x = 7 & 15 = 7, sectionY = 64 & 15 = 0, z = 11 & 15 = 11
        assertEquals(7, block.x)
        assertEquals(0, block.y)   // sectionY = 64 & 15 = 0
        assertEquals(11, block.z)
    }

    @Test
    fun `getBlockAt returns same block on repeated calls`() {
        val first = chunk.getBlockAt(3, 64, 5)
        val second = chunk.getBlockAt(3, 64, 5)
        assertSame(first, second)
    }

    @Test
    fun `getBlockAt places blocks into correct section by y`() {
        // y=0..15 → sectionIndex 0, y=16..31 → 1, etc.
        chunk.getBlockAt(0, 0, 0)   // sectionIndex = 0
        chunk.getBlockAt(0, 16, 0)  // sectionIndex = 1
        chunk.getBlockAt(0, 32, 0)  // sectionIndex = 2
        assertEquals(3, chunk.sections.size)
    }

    @Test
    fun `same y-block range stays in same section`() {
        chunk.getBlockAt(0, 64, 0)  // sectionIndex = 4 (64 shr 4)
        chunk.getBlockAt(7, 71, 7)  // sectionIndex = 4 (71 shr 4)
        assertEquals(1, chunk.sections.size)
    }

    // -------------------------------------------------------------------------
    // isDirty
    // -------------------------------------------------------------------------

    @Test
    fun `isDirty is false on fresh chunk`() {
        assertFalse(chunk.isDirty)
    }

    @Test
    fun `isDirty is true after creating a block (section becomes dirty)`() {
        chunk.getBlockAt(0, 0, 0)
        assertTrue(chunk.isDirty)
    }

    @Test
    fun `isDirty is true when block has dirty PDC`() {
        val block = chunk.getBlockAt(0, 0, 0)
        chunk.markClean()
        assertFalse(chunk.isDirty)
        block.setData(KEY, TestRegionData("v"))
        assertTrue(chunk.isDirty)
    }

    @Test
    fun `isDirty is true when chunk PDC is dirty`() {
        chunk.markClean()
        chunk.setData(KEY, TestRegionData("v"))
        assertTrue(chunk.isDirty)
    }

    // -------------------------------------------------------------------------
    // markClean
    // -------------------------------------------------------------------------

    @Test
    fun `markClean clears chunk isDirty`() {
        chunk.getBlockAt(0, 0, 0)
        assertTrue(chunk.isDirty)
        chunk.markClean()
        assertFalse(chunk.isDirty)
    }

    @Test
    fun `markClean propagates to sections and blocks`() {
        val block = chunk.getBlockAt(0, 0, 0)
        block.setData(KEY, TestRegionData("v"))
        assertTrue(block.isDirty)
        chunk.markClean()
        assertFalse(block.isDirty)
        assertFalse(chunk.isDirty)
    }
}
