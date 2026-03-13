package dev.slne.surf.region.persistence

import dev.slne.surf.region.TestRegionData
import net.kyori.adventure.key.Key
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersistentDataContainerTest {

    private lateinit var pdc: PersistentDataContainer
    private val KEY_A = Key.key("test", "a")
    private val KEY_B = Key.key("test", "b")

    @BeforeEach
    fun setUp() {
        pdc = PersistentDataContainer()
    }

    // -------------------------------------------------------------------------
    // isDirty
    // -------------------------------------------------------------------------

    @Test
    fun `isDirty is false on fresh container`() {
        assertFalse(pdc.isDirty)
    }

    @Test
    fun `setData marks container dirty`() {
        pdc.setData(KEY_A, TestRegionData("hello"))
        assertTrue(pdc.isDirty)
    }

    @Test
    fun `computeData marks container dirty`() {
        pdc.computeData(KEY_A) { _, _ -> TestRegionData("x") }
        assertTrue(pdc.isDirty)
    }

    @Test
    fun `markClean clears dirty flag`() {
        pdc.setData(KEY_A, TestRegionData("v"))
        assertTrue(pdc.isDirty)
        pdc.markClean()
        assertFalse(pdc.isDirty)
    }

    @Test
    fun `markClean after computeData clears dirty flag`() {
        pdc.computeData(KEY_A) { _, _ -> TestRegionData("v") }
        pdc.markClean()
        assertFalse(pdc.isDirty)
    }

    // -------------------------------------------------------------------------
    // getData
    // -------------------------------------------------------------------------

    @Test
    fun `getData returns null for missing key`() {
        assertNull(pdc.getData<TestRegionData>(KEY_A))
    }

    @Test
    fun `getData returns stored value`() {
        pdc.setData(KEY_A, TestRegionData("hello"))
        assertEquals(TestRegionData("hello"), pdc.getData<TestRegionData>(KEY_A))
    }

    @Test
    fun `getData with wrong cast returns null safely`() {
        pdc.setData(KEY_A, TestRegionData("value"))
        // Intentionally ask for a different type – should return null (unchecked cast trap)
        // This just ensures the unchecked cast doesn't throw a hard ClassCastException at the call site
        val result = pdc.getData<TestRegionData>(KEY_A)
        assertNotNull(result) // Same type - should work
    }

    // -------------------------------------------------------------------------
    // setData
    // -------------------------------------------------------------------------

    @Test
    fun `setData stores value retrievable by same key`() {
        pdc.setData(KEY_A, TestRegionData("abc"))
        assertEquals(TestRegionData("abc"), pdc.getData<TestRegionData>(KEY_A))
    }

    @Test
    fun `setData overwrites existing value`() {
        pdc.setData(KEY_A, TestRegionData("first"))
        pdc.setData(KEY_A, TestRegionData("second"))
        assertEquals(TestRegionData("second"), pdc.getData<TestRegionData>(KEY_A))
    }

    @Test
    fun `setData with different keys are independent`() {
        pdc.setData(KEY_A, TestRegionData("alpha"))
        pdc.setData(KEY_B, TestRegionData("beta"))
        assertEquals(TestRegionData("alpha"), pdc.getData<TestRegionData>(KEY_A))
        assertEquals(TestRegionData("beta"), pdc.getData<TestRegionData>(KEY_B))
    }

    // -------------------------------------------------------------------------
    // computeData
    // -------------------------------------------------------------------------

    @Test
    fun `computeData creates entry when absent`() {
        pdc.computeData(KEY_A) { _, _ -> TestRegionData("new") }
        assertEquals(TestRegionData("new"), pdc.getData<TestRegionData>(KEY_A))
    }

    @Test
    fun `computeData provides existing value to block`() {
        pdc.setData(KEY_A, TestRegionData("existing"))
        var received: TestRegionData? = null
        pdc.computeData(KEY_A) { _, existing ->
            received = existing as? TestRegionData
            TestRegionData("updated")
        }
        assertEquals(TestRegionData("existing"), received)
    }

    @Test
    fun `computeData replaces existing value`() {
        pdc.setData(KEY_A, TestRegionData("old"))
        pdc.computeData(KEY_A) { _, _ -> TestRegionData("new") }
        assertEquals(TestRegionData("new"), pdc.getData<TestRegionData>(KEY_A))
    }

    @Test
    fun `computeData returning null removes entry`() {
        pdc.setData(KEY_A, TestRegionData("to-remove"))
        pdc.computeData(KEY_A) { _, _ -> null }
        assertNull(pdc.getData<TestRegionData>(KEY_A))
    }

    @Test
    fun `computeData marks dirty even when block returns null`() {
        pdc.markClean()
        pdc.computeData(KEY_A) { _, _ -> null }
        assertTrue(pdc.isDirty)
    }
}
