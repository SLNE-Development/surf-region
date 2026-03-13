package dev.slne.surf.region.manager

import dev.slne.surf.region.testInstance
import dev.slne.surf.region.testManager
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RegionManagerTest {

    @TempDir
    lateinit var tempDir: Path

    // -------------------------------------------------------------------------
    // getRegion / getRegionPair
    // -------------------------------------------------------------------------

    @Test
    fun `getRegion creates region when createIfNotExists=true (default)`() = runTest {
        val manager = testManager(folder = tempDir)
        val region = manager.getRegion(0, 0)
        assertNotNull(region)
    }

    @Test
    fun `getRegion returns same instance on repeated calls`() = runTest {
        val manager = testManager(folder = tempDir)
        val first = manager.getRegion(0, 0)
        val second = manager.getRegion(0, 0)
        assertSame(first, second)
    }

    @Test
    fun `getRegion returns different instances for different coords`() = runTest {
        val manager = testManager(folder = tempDir)
        val a = manager.getRegion(0, 0)
        val b = manager.getRegion(1, 0)
        assert(a !== b)
    }

    @Test
    fun `getRegion returns null when createIfNotExists=false and region missing`() = runTest {
        val manager = testManager(folder = tempDir)
        val region = manager.getRegion(99, 99, createIfNotExists = false)
        assertNull(region)
    }

    @Test
    fun `getRegion loads region when loadIfNotLoaded=true (default)`() = runTest {
        val manager = testManager(folder = tempDir)
        val region = manager.getRegion(0, 0)
        assertNotNull(region)
        assertTrue(region.loaded)
    }

    @Test
    fun `getRegion does not load when loadIfNotLoaded=false`() = runTest {
        val manager = testManager(folder = tempDir)
        val region = manager.getRegion(0, 0, loadIfNotLoaded = false)
        assertNotNull(region)
        assertFalse(region.loaded)
    }

    @Test
    fun `getRegionPair second element is true on first creation`() = runTest {
        val manager = testManager(folder = tempDir)
        val (_, created) = manager.getRegionPair(0, 0, loadIfNotLoaded = false)
        assertTrue(created)
    }

    @Test
    fun `getRegionPair second element is false on subsequent call`() = runTest {
        val manager = testManager(folder = tempDir)
        manager.getRegionPair(0, 0, loadIfNotLoaded = false)
        val (_, created) = manager.getRegionPair(0, 0, loadIfNotLoaded = false)
        assertFalse(created)
    }

    // -------------------------------------------------------------------------
    // regions list
    // -------------------------------------------------------------------------

    @Test
    fun `regions list is empty initially`() {
        val manager = testManager(folder = tempDir)
        assertEquals(0, manager.regions.size)
    }

    @Test
    fun `regions list grows as regions are created`() = runTest {
        val manager = testManager(folder = tempDir)
        manager.getRegion(0, 0)
        manager.getRegion(1, 0)
        assertEquals(2, manager.regions.size)
    }

    // -------------------------------------------------------------------------
    // loadRegion / load handlers
    // -------------------------------------------------------------------------

    @Test
    fun `loadRegion invokes registered load handler`() = runTest {
        val callCount = AtomicInteger(0)
        val instance = testInstance(tempDir).also { inst ->
            inst.registerLoadHandler { callCount.incrementAndGet() }
        }
        val manager = testManager(folder = tempDir, instance = instance)
        val region = manager.getRegion(0, 0, loadIfNotLoaded = false, createIfNotExists = true)!!
        manager.loadRegion(region)
        assertEquals(1, callCount.get())
    }

    @Test
    fun `loadRegion is idempotent - handler called only once`() = runTest {
        val callCount = AtomicInteger(0)
        val instance = testInstance(tempDir).also { inst ->
            inst.registerLoadHandler { callCount.incrementAndGet() }
        }
        val manager = testManager(folder = tempDir, instance = instance)
        val region = manager.getRegion(0, 0, loadIfNotLoaded = false, createIfNotExists = true)!!
        manager.loadRegion(region)
        manager.loadRegion(region) // second call — region already loaded
        assertEquals(1, callCount.get())
    }

    // -------------------------------------------------------------------------
    // saveRegion / save handlers
    // -------------------------------------------------------------------------

    @Test
    fun `saveRegion invokes registered save handler`() = runTest {
        val callCount = AtomicInteger(0)
        val instance = testInstance(tempDir).also { inst ->
            inst.registerSaveHandler { callCount.incrementAndGet() }
        }
        val manager = testManager(folder = tempDir, instance = instance)
        val region = manager.getRegion(0, 0)!!
        region.getBlockAt(0, 64, 0) // make dirty
        manager.saveRegion(region)
        assertEquals(1, callCount.get())
    }

    // -------------------------------------------------------------------------
    // saveAll
    // -------------------------------------------------------------------------

    @Test
    fun `saveAll saves all loaded dirty regions`() = runTest {
        val worldId = UUID.randomUUID()
        val instance = testInstance(tempDir)
        val manager = testManager(worldId = worldId, folder = tempDir, instance = instance)

        val r0 = manager.getRegion(0, 0)!!
        r0.getBlockAt(0, 64, 0) // dirty

        val r1 = manager.getRegion(1, 0)!!
        r1.getBlockAt(0, 64, 0) // dirty

        manager.saveAll()

        assertTrue(r0.file.exists())
        assertTrue(r1.file.exists())
    }

    // -------------------------------------------------------------------------
    // unloadRegion
    // -------------------------------------------------------------------------

    @Test
    fun `unloadRegion removes region from collection`() = runTest {
        val manager = testManager(folder = tempDir)
        val region = manager.getRegion(0, 0)!!
        assertEquals(1, manager.regions.size)
        manager.unloadRegion(region, saveBeforeUnload = false)
        assertEquals(0, manager.regions.size)
    }

    @Test
    fun `unloadRegion invokes unload handler`() = runTest {
        val callCount = AtomicInteger(0)
        val instance = testInstance(tempDir).also { inst ->
            inst.registerUnloadHandler { callCount.incrementAndGet() }
        }
        val manager = testManager(folder = tempDir, instance = instance)
        val region = manager.getRegion(0, 0)!!
        manager.unloadRegion(region, saveBeforeUnload = false)
        assertEquals(1, callCount.get())
    }

    @Test
    fun `unloadRegion with saveBeforeUnload=true saves dirty region`() = runTest {
        val worldId = UUID.randomUUID()
        val instance = testInstance(tempDir)
        val manager = testManager(worldId = worldId, folder = tempDir, instance = instance)

        val region = manager.getRegion(0, 0)!!
        region.getBlockAt(0, 64, 0) // make dirty
        assertTrue(region.isDirty)

        manager.unloadRegion(region, saveBeforeUnload = true)

        assertTrue(region.file.exists())
    }

    @Test
    fun `unloadRegion with saveBeforeUnload=false skips save`() = runTest {
        val instance = testInstance(tempDir)
        val manager = testManager(folder = tempDir, instance = instance)

        val region = manager.getRegion(0, 0)!!
        region.getBlockAt(0, 64, 0) // make dirty

        manager.unloadRegion(region, saveBeforeUnload = false)

        // File should not exist because we skipped save
        assertFalse(region.file.exists())
    }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Test
    fun `two managers with same worldId are equal`() {
        val worldId = UUID.randomUUID()
        val instance = testInstance(tempDir)
        val a = RegionManager(worldId, tempDir, instance)
        val b = RegionManager(worldId, tempDir, instance)
        assertEquals(a, b)
    }

    @Test
    fun `two managers with different worldIds are not equal`() {
        val instance = testInstance(tempDir)
        val a = RegionManager(UUID.randomUUID(), tempDir, instance)
        val b = RegionManager(UUID.randomUUID(), tempDir, instance)
        assert(a != b)
    }
}
