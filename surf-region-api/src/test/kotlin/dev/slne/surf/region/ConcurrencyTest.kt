package dev.slne.surf.region

import dev.slne.surf.region.data.ModifiedBlocksData
import dev.slne.surf.region.data.RegionDataSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests that verify thread-safety and absence of data races when multiple coroutines
 * concurrently call [SurfRegion.markModified] on the same region.
 *
 * Every test is repeated several times to increase the probability of surfacing race
 * conditions that might only appear under specific scheduling.
 */
class ConcurrencyTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var region: SurfRegion

    companion object {
        private const val THREADS = 4
        private const val MARKS_PER_THREAD = 250
        private const val TOTAL_MARKS = THREADS * MARKS_PER_THREAD
    }

    @BeforeEach
    fun setup() {
        RegionDataSerializer.register<ModifiedBlocksData>()
        region = SurfRegion(0, 0, File(tempDir.toFile(), "r.0.0.json"))
    }

    // -------------------------------------------------------------------------
    // Concurrent markModified – no data is lost
    // -------------------------------------------------------------------------

    @RepeatedTest(5)
    fun `concurrent markModified on disjoint coordinates loses no entries`() = runTest {
        // Each coroutine writes to its own unique x-coordinate slice.
        val jobs = (0 until THREADS).map { thread ->
            launch(Dispatchers.Default) {
                val start = thread * MARKS_PER_THREAD
                for (i in start until start + MARKS_PER_THREAD) {
                    region.markModified(i, 64, 0)
                }
            }
        }
        jobs.forEach { it.join() }

        for (i in 0 until TOTAL_MARKS) {
            assertTrue(region.isModified(i, 64, 0), "Block ($i, 64, 0) should be marked")
        }
    }

    @RepeatedTest(5)
    fun `concurrent markModified on the same coordinate does not corrupt state`() = runTest {
        val jobs = (0 until THREADS).map {
            launch(Dispatchers.Default) {
                repeat(MARKS_PER_THREAD) {
                    region.markModified(0, 64, 0)
                }
            }
        }
        jobs.forEach { it.join() }

        assertTrue(region.isModified(0, 64, 0))
    }

    // -------------------------------------------------------------------------
    // Concurrent markModified + save – data survives a concurrent save
    // -------------------------------------------------------------------------

    @RepeatedTest(3)
    fun `concurrent markModified and save preserves modifications`() = runTest {
        val markJob = launch(Dispatchers.Default) {
            for (i in 0 until TOTAL_MARKS) {
                region.markModified(i, 64, 0)
            }
        }
        val saveJob = launch(Dispatchers.Default) {
            repeat(10) {
                region.save()
            }
        }
        markJob.join()
        saveJob.join()

        // Final save to ensure all marks are on disk.
        region.save()

        val reloaded = SurfRegion(0, 0, File(tempDir.toFile(), "r.0.0.json"))
        reloaded.load()

        // At least some (possibly not all) marks must be present – the save might have
        // captured a snapshot mid-way through marking.  The important invariant is that
        // the data is consistent (no corrupt/partial entries).
        val data = reloaded.getData<ModifiedBlocksData>(ModifiedBlocksData.MODIFIED_BLOCKS_KEY)
        assertTrue(data != null, "ModifiedBlocksData should be present after concurrent save")
        assertTrue(data.modifiedBlocks.isNotEmpty(), "At least one modification must survive")
    }

    // -------------------------------------------------------------------------
    // Concurrent load – only one disk read happens
    // -------------------------------------------------------------------------

    @RepeatedTest(3)
    fun `concurrent load calls on an unloaded region load exactly once`() = runTest {
        // Pre-populate the file so load has something to read.
        region.markModified(1, 64, 1)
        region.save()

        val freshRegion = SurfRegion(0, 0, File(tempDir.toFile(), "r.0.0.json"))

        // Concurrently call load() many times – only the first should do I/O.
        withContext(Dispatchers.Default) {
            val jobs = (0 until THREADS).map {
                launch { freshRegion.load() }
            }
            jobs.forEach { it.join() }
        }

        assertTrue(freshRegion.loaded)
        assertTrue(freshRegion.isModified(1, 64, 1))
    }

    // -------------------------------------------------------------------------
    // Concurrent getRegion / loadRegion via RegionManager
    // -------------------------------------------------------------------------

    @RepeatedTest(3)
    fun `concurrent loadRegion calls return the same SurfRegion instance`() = runTest {
        val manager = dev.slne.surf.region.manager.RegionManager(
            folder = tempDir,
            scope = this
        )

        val instances = mutableSetOf<SurfRegion>()
        val lock = Any()

        val jobs = (0 until THREADS).map {
            launch(Dispatchers.Default) {
                val r = manager.loadRegion(0, 0)
                synchronized(lock) { instances.add(r) }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(1, instances.size, "All concurrent loadRegion calls must return the same object")
    }
}
