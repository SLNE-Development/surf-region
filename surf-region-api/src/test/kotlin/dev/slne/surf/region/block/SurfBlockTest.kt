package dev.slne.surf.region.block

import dev.slne.surf.region.TestRegionData
import dev.slne.surf.region.persistence.PersistentDataContainer
import net.kyori.adventure.key.Key
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SurfBlockTest {

    private val KEY = Key.key("surf", "test")

    @Test
    fun `equals is based on coordinates only`() {
        val a = SurfBlock(1, 64, 1)
        val b = SurfBlock(1, 64, 1)
        assertEquals(a, b)
    }

    @Test
    fun `equals ignores persistentDataContainer content`() {
        val a = SurfBlock(0, 0, 0, PersistentDataContainer())
        val b = SurfBlock(0, 0, 0, PersistentDataContainer())
        b.setData(KEY, TestRegionData("hello"))
        assertEquals(a, b)
    }

    @Test
    fun `equals returns false when any coordinate differs`() {
        val base = SurfBlock(5, 5, 5)
        assertNotEquals(base, SurfBlock(6, 5, 5))
        assertNotEquals(base, SurfBlock(5, 6, 5))
        assertNotEquals(base, SurfBlock(5, 5, 6))
    }

    @Test
    fun `hashCode consistent with coordinate equality`() {
        val a = SurfBlock(3, 64, -3)
        val b = SurfBlock(3, 64, -3)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `isDirty is false by default`() {
        val block = SurfBlock(0, 0, 0)
        assertFalse(block.isDirty)
    }

    @Test
    fun `setData marks block dirty`() {
        val block = SurfBlock(1, 1, 1)
        assertFalse(block.isDirty)
        block.setData(KEY, TestRegionData("value"))
        assertTrue(block.isDirty)
    }

    @Test
    fun `getData returns null when key absent`() {
        val block = SurfBlock(0, 0, 0)
        val result = block.getData<TestRegionData>(KEY)
        assertNull(result)
    }

    @Test
    fun `getData returns stored value`() {
        val block = SurfBlock(0, 0, 0)
        block.setData(KEY, TestRegionData("hello"))
        val result = block.getData<TestRegionData>(KEY)
        assertEquals(TestRegionData("hello"), result)
    }

    @Test
    fun `setData overwrites previous value`() {
        val block = SurfBlock(0, 0, 0)
        block.setData(KEY, TestRegionData("first"))
        block.setData(KEY, TestRegionData("second"))
        val result = block.getData<TestRegionData>(KEY)
        assertEquals(TestRegionData("second"), result)
    }

    @Test
    fun `computeData creates new entry when absent`() {
        val block = SurfBlock(0, 0, 0)
        block.computeData(KEY) { _, _ -> TestRegionData("computed") }
        assertEquals(TestRegionData("computed"), block.getData<TestRegionData>(KEY))
    }

    @Test
    fun `computeData receives existing value`() {
        val block = SurfBlock(0, 0, 0)
        block.setData(KEY, TestRegionData("initial"))
        var receivedExisting: TestRegionData? = null
        block.computeData(KEY) { _, existing ->
            receivedExisting = existing as? TestRegionData
            TestRegionData("updated")
        }
        assertEquals(TestRegionData("initial"), receivedExisting)
        assertEquals(TestRegionData("updated"), block.getData<TestRegionData>(KEY))
    }

    @Test
    fun `computeData returning null removes entry`() {
        val block = SurfBlock(0, 0, 0)
        block.setData(KEY, TestRegionData("to-remove"))
        block.computeData(KEY) { _, _ -> null }
        assertNull(block.getData<TestRegionData>(KEY))
    }

    @Test
    fun `markClean clears dirty flag`() {
        val block = SurfBlock(0, 0, 0)
        block.setData(KEY, TestRegionData("x"))
        assertTrue(block.isDirty)
        block.markClean()
        assertFalse(block.isDirty)
    }
}
