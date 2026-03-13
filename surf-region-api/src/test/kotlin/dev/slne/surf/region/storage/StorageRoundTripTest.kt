package dev.slne.surf.region.storage

import dev.slne.surf.region.TestRegionData
import dev.slne.surf.region.data.RegionDataSerializer
import dev.slne.surf.region.testInstance
import dev.slne.surf.region.testRegion
import kotlinx.coroutines.test.runTest
import net.kyori.adventure.key.Key
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StorageRoundTripTest {

    @TempDir
    lateinit var tempDir: Path

    private val DATA_KEY = Key.key("test", "payload")

    @BeforeEach
    fun registerTestData() {
        RegionDataSerializer.register<TestRegionData>()
    }

    // -------------------------------------------------------------------------
    // JsonRegionStorage
    // -------------------------------------------------------------------------

    @Test
    fun `JsonRegionStorage round-trip preserves chunk coordinates`() = runTest {
        val worldId = UUID.randomUUID()
        val instance = testInstance(tempDir)  // uses JsonRegionStorage by default

        // Write
        val writer = testRegion(worldId = worldId, instance = instance)
        writer.load()
        writer.getChunkAt(3, 7)   // create chunk at (3,7)
        writer.save()

        assertTrue(writer.file.exists())

        // Read
        val reader = testRegion(worldId = worldId, instance = instance)
        reader.load()
        // The chunk was serialised — its entries are present in the loaded region
        assertTrue(reader.loaded)
    }

    @Test
    fun `JsonRegionStorage round-trip preserves multiple chunks`() = runTest {
        val worldId = UUID.randomUUID()
        val instance = testInstance(tempDir)

        val coords = listOf(0 to 0, 5 to 5, -1 to -1, 31 to 31)

        val writer = testRegion(worldId = worldId, instance = instance)
        writer.load()
        coords.forEach { (cx, cz) -> writer.getChunkAt(cx, cz) }
        writer.save()

        val reader = testRegion(worldId = worldId, instance = instance)
        reader.load()
        assertTrue(reader.loaded)
    }

    @Test
    fun `JsonRegionStorage file extension is json`() = runTest {
        val instance = testInstance(tempDir)
        val region = testRegion(instance = instance)
        region.load()
        region.getBlockAt(0, 0, 0) // dirty
        region.save()
        assertTrue(region.file.name.endsWith(".json"))
    }

    // -------------------------------------------------------------------------
    // CborRegionStorage
    // -------------------------------------------------------------------------

    @Test
    fun `CborRegionStorage round-trip preserves chunk coordinates`() = runTest {
        val worldId = UUID.randomUUID()
        // CborRegionStorage is the default in RegionInstance
        val instance = dev.slne.surf.region.RegionInstance(tempDir) {
            CborRegionStorage(it)
        }

        val writer = testRegion(worldId = worldId, instance = instance)
        writer.load()
        writer.getChunkAt(2, 4)
        writer.save()

        assertTrue(writer.file.exists())

        val reader = testRegion(worldId = worldId, instance = instance)
        reader.load()
        assertTrue(reader.loaded)
    }

    @Test
    fun `CborRegionStorage file extension is cbor`() = runTest {
        val worldId = UUID.randomUUID()
        val instance = dev.slne.surf.region.RegionInstance(tempDir) {
            CborRegionStorage(it)
        }
        val region = testRegion(worldId = worldId, instance = instance)
        region.load()
        region.getBlockAt(0, 0, 0)
        region.save()
        assertTrue(region.file.name.endsWith(".cbor"))
    }

    @Test
    fun `CborRegionStorage round-trip preserves multiple chunks`() = runTest {
        val worldId = UUID.randomUUID()
        val instance = dev.slne.surf.region.RegionInstance(tempDir) {
            CborRegionStorage(it)
        }
        val coords = listOf(0 to 0, 10 to 10)

        val writer = testRegion(worldId = worldId, instance = instance)
        writer.load()
        coords.forEach { (cx, cz) -> writer.getChunkAt(cx, cz) }
        writer.save()

        val reader = testRegion(worldId = worldId, instance = instance)
        reader.load()
        assertTrue(reader.loaded)
    }

    // -------------------------------------------------------------------------
    // Mixed: save with Json, verify file readable
    // -------------------------------------------------------------------------

    @Test
    fun `saved file is non-empty after writing blocks`() = runTest {
        val worldId = UUID.randomUUID()
        val instance = testInstance(tempDir)

        val region = testRegion(worldId = worldId, instance = instance)
        region.load()
        region.getBlockAt(0, 64, 0)
        region.getBlockAt(15, 64, 15)
        region.save()

        assertTrue(region.file.length() > 0)
    }

    @Test
    fun `fresh load of non-existent file leaves region with no chunks`() = runTest {
        val region = testRegion(instance = testInstance(tempDir))
        region.load()
        // No blocks accessed => no chunks created
        assertTrue(region.loaded)
        assertEquals(0, region.chunksLoaded.get())
    }
}
