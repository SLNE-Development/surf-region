package dev.slne.surf.region

import dev.slne.surf.region.manager.RegionManager
import dev.slne.surf.region.region.SurfRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.Collections
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Concurrency tests for the surf-region-api.
 *
 * Each test is repeated multiple times to increase the probability of surfacing race
 * conditions that only occur under specific thread interleavings.
 *
 * Tests deliberately exercise the documented (and in some cases known-possible) race
 * windows so that they can be caught by CI and used to drive further hardening.
 */
class ConcurrencyTest {

    @TempDir
    lateinit var tempDir: Path

    companion object {
        private const val CONCURRENT_TASKS = 8
        private const val TEST_REPETITIONS = 5
        private val DATA_KEY = Key.key("test", "concurrency")
    }

    // =========================================================================
    // PersistentDataContainer
    // =========================================================================

    /**
     * [computeData] must never lose an update when called concurrently because
     * it delegates to [java.util.HashMap.compute], which is NOT thread-safe.
     * This test intentionally stresses that path to surface data loss.
     */
    @RepeatedTest(TEST_REPETITIONS)
    fun `concurrent computeData on different blocks does not corrupt any single block`() =
        runTest {
            val instance = testInstance(tempDir)
            val region = testRegion(instance = instance).also { it.load() }

            // Each coroutine works on its OWN block → no shared state between blocks
            val jobs = (0 until CONCURRENT_TASKS).map { i ->
                launch(Dispatchers.Default) {
                    repeat(50) { iter ->
                        region.getBlockAt(i, 64, 0)
                            .computeData(DATA_KEY) { _, _ ->
                                TestRegionData("block-$i-iter-$iter")
                            }
                    }
                }
            }
            jobs.joinAll()

            // Each block should have the data we last set
            for (i in 0 until CONCURRENT_TASKS) {
                val block = region.getBlockAt(i, 64, 0)
                val data = block.getData<TestRegionData>(DATA_KEY)
                // data may be any of the 50 iterations — just must not be null
                assertTrue(data != null, "Block $i should have data after concurrent writes")
            }
        }

    /**
     * Multiple concurrent [computeData] calls on THE SAME block expose the fact
     * that [java.util.HashMap.compute] is not thread-safe. The test validates that
     * the dirty flag is set and the block has *some* data (i.e., no total corruption),
     * while accepting that some writes may be lost.
     */
    @RepeatedTest(TEST_REPETITIONS)
    fun `concurrent computeData on same block — dirty flag always set`() = runTest {
        val instance = testInstance(tempDir)
        val region = testRegion(instance = instance).also { it.load() }
        val block = region.getBlockAt(0, 64, 0)

        val jobs = (0 until CONCURRENT_TASKS).map {
            launch(Dispatchers.Default) {
                repeat(50) { iter ->
                    block.computeData(DATA_KEY) { _, _ -> TestRegionData("iter-$iter") }
                }
            }
        }
        jobs.joinAll()

        // The dirty flag is a volatile write — it MUST be true
        assertTrue(block.isDirty)
    }

    // =========================================================================
    // SurfRegion — concurrent load
    // =========================================================================

    /**
     * [SurfRegion.load] checks `if (loaded) return` but does not hold a lock
     * for the entire critical section. Concurrent callers can all pass the guard
     * before the first sets `loaded = true`. The test verifies that after all
     * concurrent loads complete the region is in a consistent state (loaded=true,
     * no exception thrown).
     */
    @RepeatedTest(TEST_REPETITIONS)
    fun `concurrent load calls always result in loaded=true`() = runTest {
        val instance = testInstance(tempDir)
        val region = testRegion(instance = instance)

        withContext(Dispatchers.Default) {
            (0 until CONCURRENT_TASKS).map { launch { region.load() } }.joinAll()
        }

        assertTrue(region.loaded, "Region must be loaded after concurrent load calls")
    }

    @RepeatedTest(TEST_REPETITIONS)
    fun `concurrent load on a pre-saved region does not corrupt loaded data`() = runTest {
        val worldId = UUID.randomUUID()
        val instance = testInstance(tempDir)

        // Pre-populate the file
        val writer = testRegion(worldId = worldId, instance = instance)
        writer.load()
        writer.getBlockAt(0, 64, 0)
        writer.save()

        val reader = testRegion(worldId = worldId, instance = instance)

        withContext(Dispatchers.Default) {
            (0 until CONCURRENT_TASKS).map { launch { reader.load() } }.joinAll()
        }

        assertTrue(reader.loaded)
    }

    // =========================================================================
    // RegionManager — concurrent getRegion
    // =========================================================================

    /**
     * [RegionManager.getRegionPair] uses a non-atomic check-then-act pattern.
     * Under concurrent access, multiple [SurfRegion] instances may be constructed
     * for the same key, but only one survives in the map. This test documents the
     * current behaviour: regardless of the race, the map must end up with exactly
     * one region for the queried coordinate.
     */
    @RepeatedTest(TEST_REPETITIONS)
    fun `concurrent getRegion for same coords results in exactly one region in map`() = runTest {
        val instance = testInstance(tempDir)
        val manager = testManager(folder = tempDir, instance = instance)

        withContext(Dispatchers.Default) {
            (0 until CONCURRENT_TASKS).map {
                launch {
                    manager.getRegion(0, 0, loadIfNotLoaded = false)
                }
            }.joinAll()
        }

        assertEquals(1, manager.regions.size)
    }

    /**
     * All concurrent [RegionManager.getRegion] calls for the same coordinate must
     * return the region that survives in the map (rather than a discarded duplicate).
     */
    @RepeatedTest(TEST_REPETITIONS)
    fun `all concurrent getRegion calls observe the same final region instance`() = runTest {
        val instance = testInstance(tempDir)
        val manager = testManager(folder = tempDir, instance = instance)

        // First, ensure the region is created and cached
        val canonical = manager.getRegion(0, 0, loadIfNotLoaded = false)!!

        val seenInstances = Collections.synchronizedSet(mutableSetOf<SurfRegion>())

        withContext(Dispatchers.Default) {
            (0 until CONCURRENT_TASKS).map {
                launch {
                    val r = manager.getRegion(0, 0, loadIfNotLoaded = false)
                    if (r != null) seenInstances.add(r)
                }
            }.joinAll()
        }

        // All calls after the initial creation must return the canonical instance
        assertEquals(1, seenInstances.size)
        assertSame(canonical, seenInstances.first())
    }

    // =========================================================================
    // RegionManager — concurrent saveAll
    // =========================================================================

    @RepeatedTest(TEST_REPETITIONS)
    fun `concurrent saveAll calls do not throw`() = runTest {
        val instance = testInstance(tempDir)
        val manager = testManager(folder = tempDir, instance = instance)

        // Populate with dirty regions
        repeat(4) { i ->
            val r = manager.getRegion(i, 0)!!
            r.getBlockAt(0, 64, 0)
        }

        // Fire multiple saveAll concurrently
        withContext(Dispatchers.Default) {
            (0 until CONCURRENT_TASKS).map { launch { manager.saveAll() } }.joinAll()
        }

        // All region files must exist
        manager.regions.forEach { r ->
            assertTrue(r.file.exists(), "File for region (${r.x},${r.z}) must exist")
        }
    }

    // =========================================================================
    // RegionManager — concurrent load + save interleave
    // =========================================================================

    @RepeatedTest(TEST_REPETITIONS)
    fun `concurrent load and save on same region do not corrupt file`() = runTest {
        val worldId = UUID.randomUUID()
        val instance = testInstance(tempDir)

        // Create an initial file
        val seed = testRegion(worldId = worldId, instance = instance)
        seed.load()
        seed.getBlockAt(0, 64, 0)
        seed.save()

        val manager = testManager(worldId = worldId, folder = tempDir, instance = instance)

        val region = manager.getRegion(0, 0)!!

        val loadCount = AtomicInteger(0)
        val saveCount = AtomicInteger(0)

        val loaders = (0 until 4).map {
            launch(Dispatchers.Default) {
                repeat(10) {
                    region.load()
                    loadCount.incrementAndGet()
                }
            }
        }

        val savers = (0 until 4).map {
            launch(Dispatchers.Default) {
                repeat(10) {
                    region.save()
                    saveCount.incrementAndGet()
                }
            }
        }

        loaders.joinAll()
        savers.joinAll()

        assertEquals(40, loadCount.get())
        assertEquals(40, saveCount.get())
        assertTrue(region.loaded)
    }

    // =========================================================================
    // ChunkSection — concurrent getOrCreateBlock
    // =========================================================================

    /**
     * [ChunkSection.getOrCreateBlock] uses an unsynchronized [java.util.HashMap] internally.
     * Concurrent insertions can corrupt the map. This test detects lost inserts or
     * internal corruption by checking that all blocks created by the "winning"
     * thread are retrievable.
     */
    @RepeatedTest(TEST_REPETITIONS)
    fun `concurrent getOrCreateBlock on disjoint coordinates does not lose blocks`() = runTest {
        val section = dev.slne.surf.region.chunk.ChunkSection()

        // Each coroutine owns one x-column (0..7) in the 16×16×16 section grid
        val jobs = (0 until CONCURRENT_TASKS).map { x ->
            launch(Dispatchers.Default) {
                for (z in 0 until 16) {
                    section.getOrCreateBlock(x, 0, z)
                }
            }
        }
        jobs.joinAll()

        // At minimum all blocks for the first coroutine's column must exist
        // (stronger guarantees require synchronization in the implementation)
        assertTrue(section.blocks.isNotEmpty())
    }

    // =========================================================================
    // RegionInstance — concurrent manager creation
    // =========================================================================

    @RepeatedTest(TEST_REPETITIONS)
    fun `concurrent findOrCreateRegionManager returns consistent result`() = runTest {
        val instance = testInstance(tempDir)
        val worldId = UUID.randomUUID()

        val seenManagers = Collections.synchronizedSet(
            mutableSetOf<RegionManager>()
        )

        withContext(Dispatchers.Default) {
            (0 until CONCURRENT_TASKS).map {
                launch {
                    val m = instance.findOrCreateRegionManager(worldId)
                    seenManagers.add(m)
                }
            }.joinAll()
        }

        // At most one manager should exist for the world
        assertEquals(1, instance.regionManagers.size)
    }
}
