package dev.slne.surf.region

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RegionInstanceTest {

    @TempDir
    lateinit var tempDir: Path

    // -------------------------------------------------------------------------
    // createRegionManager
    // -------------------------------------------------------------------------

    @Test
    fun `createRegionManager creates a manager for given worldId`() {
        val instance = testInstance(tempDir)
        val worldId = UUID.randomUUID()
        val manager = instance.createRegionManager(worldId)
        assertNotNull(manager)
        assertEquals(worldId, manager.worldId)
    }

    @Test
    fun `createRegionManager returns same manager for duplicate worldId`() {
        val instance = testInstance(tempDir)
        val worldId = UUID.randomUUID()
        val first = instance.createRegionManager(worldId)
        val second = instance.createRegionManager(worldId)
        assertSame(first, second)
    }

    @Test
    fun `createRegionManager registers manager in regionManagers set`() {
        val instance = testInstance(tempDir)
        val worldId = UUID.randomUUID()
        val manager = instance.createRegionManager(worldId)
        assertTrue(instance.regionManagers.contains(manager))
    }

    // -------------------------------------------------------------------------
    // findRegionManager
    // -------------------------------------------------------------------------

    @Test
    fun `findRegionManager returns null when no manager exists`() {
        val instance = testInstance(tempDir)
        assertNull(instance.findRegionManager(UUID.randomUUID()))
    }

    @Test
    fun `findRegionManager returns existing manager`() {
        val instance = testInstance(tempDir)
        val worldId = UUID.randomUUID()
        val manager = instance.createRegionManager(worldId)
        assertSame(manager, instance.findRegionManager(worldId))
    }

    @Test
    fun `findRegionManager returns null for different worldId`() {
        val instance = testInstance(tempDir)
        instance.createRegionManager(UUID.randomUUID())
        assertNull(instance.findRegionManager(UUID.randomUUID()))
    }

    // -------------------------------------------------------------------------
    // findOrCreateRegionManager
    // -------------------------------------------------------------------------

    @Test
    fun `findOrCreateRegionManager creates when absent`() {
        val instance = testInstance(tempDir)
        val worldId = UUID.randomUUID()
        val manager = instance.findOrCreateRegionManager(worldId)
        assertNotNull(manager)
        assertEquals(worldId, manager.worldId)
    }

    @Test
    fun `findOrCreateRegionManager returns existing manager`() {
        val instance = testInstance(tempDir)
        val worldId = UUID.randomUUID()
        val created = instance.createRegionManager(worldId)
        val found = instance.findOrCreateRegionManager(worldId)
        assertSame(created, found)
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    @Test
    fun `registerLoadHandler is called on region load`() = runTest {
        val callCount = AtomicInteger(0)
        val instance = testInstance(tempDir)
        instance.registerLoadHandler { callCount.incrementAndGet() }

        val worldId = UUID.randomUUID()
        val manager = instance.createRegionManager(worldId)
        manager.getRegion(0, 0) // triggers load

        assertEquals(1, callCount.get())
    }

    @Test
    fun `registerSaveHandler is called on region save`() = runTest {
        val callCount = AtomicInteger(0)
        val instance = testInstance(tempDir)
        instance.registerSaveHandler { callCount.incrementAndGet() }

        val manager = instance.createRegionManager(UUID.randomUUID())
        val region = manager.getRegion(0, 0)!!
        region.getBlockAt(0, 64, 0) // make dirty
        manager.saveRegion(region)

        assertEquals(1, callCount.get())
    }

    @Test
    fun `registerUnloadHandler is called on region unload`() = runTest {
        val callCount = AtomicInteger(0)
        val instance = testInstance(tempDir)
        instance.registerUnloadHandler { callCount.incrementAndGet() }

        val manager = instance.createRegionManager(UUID.randomUUID())
        val region = manager.getRegion(0, 0)!!
        manager.unloadRegion(region, saveBeforeUnload = false)

        assertEquals(1, callCount.get())
    }

    @Test
    fun `multiple handlers are all invoked`() = runTest {
        val callCount = AtomicInteger(0)
        val instance = testInstance(tempDir)
        instance.registerLoadHandler { callCount.incrementAndGet() }
        instance.registerLoadHandler { callCount.incrementAndGet() }
        instance.registerLoadHandler { callCount.incrementAndGet() }

        val manager = instance.createRegionManager(UUID.randomUUID())
        manager.getRegion(0, 0)

        assertEquals(3, callCount.get())
    }

    // -------------------------------------------------------------------------
    // unloadRegions
    // -------------------------------------------------------------------------

    @Test
    fun `unloadRegions removes all regions from all managers`() = runTest {
        val instance = testInstance(tempDir)
        val m1 = instance.createRegionManager(UUID.randomUUID())
        val m2 = instance.createRegionManager(UUID.randomUUID())

        m1.getRegion(0, 0)
        m1.getRegion(1, 0)
        m2.getRegion(0, 0)

        assertEquals(2, m1.regions.size)
        assertEquals(1, m2.regions.size)

        instance.unloadRegions(saveBeforeUnload = false)

        assertEquals(0, m1.regions.size)
        assertEquals(0, m2.regions.size)
    }
}
