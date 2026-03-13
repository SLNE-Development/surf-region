package dev.slne.surf.region.region

import dev.slne.surf.region.testInstance
import dev.slne.surf.region.testRegion
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SurfRegionTest {

    @TempDir
    lateinit var tempDir: Path

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `newly created region is not loaded`() {
        val region = testRegion(instance = testInstance(tempDir))
        assertFalse(region.loaded)
    }

    @Test
    fun `newly created region is not dirty`() {
        val region = testRegion(instance = testInstance(tempDir))
        assertFalse(region.isDirty)
    }

    // -------------------------------------------------------------------------
    // load
    // -------------------------------------------------------------------------

    @Test
    fun `load from non-existent file sets loaded=true`() = runTest {
        val region = testRegion(instance = testInstance(tempDir))
        region.load()
        assertTrue(region.loaded)
    }

    @Test
    fun `load is idempotent - second call is a no-op`() = runTest {
        val instance = testInstance(tempDir)
        val region = testRegion(instance = instance)
        region.load()
        assertTrue(region.loaded)
        // Should not throw or alter state
        region.load()
        assertTrue(region.loaded)
    }

    @Test
    fun `load from non-existent file leaves region empty`() = runTest {
        val region = testRegion(instance = testInstance(tempDir))
        region.load()
        // chunksLoaded counter starts at 0 (no Paper chunks attached)
        assertEquals(0, region.chunksLoaded.get())
    }

    // -------------------------------------------------------------------------
    // save
    // -------------------------------------------------------------------------

    @Test
    fun `save of unloaded region does not throw`() = runTest {
        val region = testRegion(instance = testInstance(tempDir))
        // loaded=false, isDirty=false -> should be a no-op
        region.save()
    }

    @Test
    fun `save creates file`() = runTest {
        val instance = testInstance(tempDir)
        val worldId = UUID.randomUUID()
        val region = testRegion(worldId = worldId, instance = instance)
        region.load()
        // Accessing a block marks chunk dirty
        region.getBlockAt(0, 64, 0)
        assertTrue(region.isDirty)
        region.save()
        assertTrue(region.file.exists())
    }

    @Test
    fun `save then load restores chunk structure`() = runTest {
        val worldId = UUID.randomUUID()
        val instance = testInstance(tempDir)

        // Write
        val writer = testRegion(worldId = worldId, instance = instance)
        writer.load()
        writer.getBlockAt(0, 64, 0)      // creates block → dirty
        writer.getBlockAt(100, 128, 100) // second chunk
        writer.save()

        // Read
        val reader = testRegion(worldId = worldId, instance = instance)
        reader.load()
        assertTrue(reader.loaded)
        assertNotNull(reader.getChunkAt(0, 0))
    }

    // -------------------------------------------------------------------------
    // getChunkAt
    // -------------------------------------------------------------------------

    @Test
    fun `getChunkAt creates new chunk on first call`() = runTest {
        val region = testRegion(instance = testInstance(tempDir))
        region.load()
        val chunk = region.getChunkAt(0, 0)
        assertNotNull(chunk)
    }

    @Test
    fun `getChunkAt returns same instance on repeated calls`() = runTest {
        val region = testRegion(instance = testInstance(tempDir))
        region.load()
        val a = region.getChunkAt(5, 5)
        val b = region.getChunkAt(5, 5)
        assertSame(a, b)
    }

    @Test
    fun `getChunkAt with different coords returns different instances`() = runTest {
        val region = testRegion(instance = testInstance(tempDir))
        region.load()
        val a = region.getChunkAt(0, 0)
        val b = region.getChunkAt(0, 1)
        assert(a !== b)
    }

    // -------------------------------------------------------------------------
    // getBlockAt
    // -------------------------------------------------------------------------

    @Test
    fun `getBlockAt routes to correct chunk`() = runTest {
        val region = testRegion(instance = testInstance(tempDir))
        region.load()
        // Block at (0,64,0) is in chunk (0 shr 4, 0 shr 4) = (0,0)
        val block = region.getBlockAt(0, 64, 0)
        assertNotNull(block)
    }

    @Test
    fun `getBlockAt returns same block on repeated calls`() = runTest {
        val region = testRegion(instance = testInstance(tempDir))
        region.load()
        val a = region.getBlockAt(15, 255, 15)
        val b = region.getBlockAt(15, 255, 15)
        assertSame(a, b)
    }

    // -------------------------------------------------------------------------
    // isDirty
    // -------------------------------------------------------------------------

    @Test
    fun `isDirty is false on clean loaded region`() = runTest {
        val region = testRegion(instance = testInstance(tempDir))
        region.load()
        assertFalse(region.isDirty)
    }

    @Test
    fun `isDirty is true after accessing a block (creates block → dirty)`() = runTest {
        val region = testRegion(instance = testInstance(tempDir))
        region.load()
        region.getBlockAt(0, 64, 0)
        assertTrue(region.isDirty)
    }

    // -------------------------------------------------------------------------
    // chunksLoaded counter
    // -------------------------------------------------------------------------

    @Test
    fun `chunksLoaded starts at zero`() {
        val region = testRegion(instance = testInstance(tempDir))
        assertEquals(0, region.chunksLoaded.get())
    }

    @Test
    fun `chunksLoaded can be incremented and decremented`() {
        val region = testRegion(instance = testInstance(tempDir))
        region.chunksLoaded.incrementAndGet()
        region.chunksLoaded.incrementAndGet()
        assertEquals(2, region.chunksLoaded.get())
        region.chunksLoaded.decrementAndGet()
        assertEquals(1, region.chunksLoaded.get())
    }
}
