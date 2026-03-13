package dev.slne.surf.region

import dev.slne.surf.region.data.ModifiedBlocksData
import dev.slne.surf.region.data.RegionDataSerializer
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SurfRegionTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var region: SurfRegion

    @BeforeEach
    fun setup() {
        RegionDataSerializer.register<ModifiedBlocksData>()
        region = SurfRegion(0, 0, File(tempDir.toFile(), "r.0.0.json"))
    }

    // -------------------------------------------------------------------------
    // isModified / markModified — basic
    // -------------------------------------------------------------------------

    @Test
    fun `isModified returns false for untouched block`() {
        assertFalse(region.isModified(0, 64, 0))
    }

    @Test
    fun `isModified returns true after markModified`() {
        region.markModified(10, 64, 20)
        assertTrue(region.isModified(10, 64, 20))
    }

    @Test
    fun `isModified is coordinate-specific`() {
        region.markModified(1, 64, 1)
        assertFalse(region.isModified(2, 64, 1))
        assertFalse(region.isModified(1, 65, 1))
        assertFalse(region.isModified(1, 64, 2))
    }

    @Test
    fun `markModified is idempotent`() {
        repeat(5) { region.markModified(5, 64, 5) }
        assertTrue(region.isModified(5, 64, 5))
    }

    @Test
    fun `markModified sets dirty flag`() {
        assertFalse(region.dirty)
        region.markModified(0, 64, 0)
        assertTrue(region.dirty)
    }

    @Test
    fun `multiple different blocks can be marked`() {
        val positions = listOf(
            Triple(0, 64, 0),
            Triple(100, 64, 200),
            Triple(-50, 32, -50),
            Triple(511, 255, 511),
        )
        positions.forEach { (x, y, z) -> region.markModified(x, y, z) }
        positions.forEach { (x, y, z) -> assertTrue(region.isModified(x, y, z)) }
    }

    // -------------------------------------------------------------------------
    // Persistence – save then load
    // -------------------------------------------------------------------------

    @Test
    fun `modified blocks persist after save and load`() = runTest {
        region.markModified(42, 64, 42)
        region.save()

        val reloaded = SurfRegion(0, 0, File(tempDir.toFile(), "r.0.0.json"))
        reloaded.load()

        assertTrue(reloaded.isModified(42, 64, 42))
    }

    @Test
    fun `unmodified block is not present after save and load`() = runTest {
        region.markModified(1, 64, 1)
        region.save()

        val reloaded = SurfRegion(0, 0, File(tempDir.toFile(), "r.0.0.json"))
        reloaded.load()

        assertFalse(reloaded.isModified(2, 64, 1))
    }

    @Test
    fun `dirty flag is cleared after save`() = runTest {
        region.markModified(0, 64, 0)
        assertTrue(region.dirty)
        region.save()
        assertFalse(region.dirty)
    }

    @Test
    fun `loading same region twice is idempotent`() = runTest {
        region.markModified(7, 64, 7)
        region.save()

        val reloaded = SurfRegion(0, 0, File(tempDir.toFile(), "r.0.0.json"))
        reloaded.load()
        reloaded.load() // second load must not overwrite in-memory state

        assertTrue(reloaded.isModified(7, 64, 7))
    }

    // -------------------------------------------------------------------------
    // Negative coordinates
    // -------------------------------------------------------------------------

    @Test
    fun `negative coordinates are tracked correctly`() {
        region.markModified(-1, -64, -1)
        assertTrue(region.isModified(-1, -64, -1))
        assertFalse(region.isModified(-2, -64, -1))
    }
}
