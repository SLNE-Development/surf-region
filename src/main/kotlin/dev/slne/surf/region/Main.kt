package dev.slne.surf.region

import dev.slne.surf.region.data.RegionDataSerializer
import dev.slne.surf.region.manager.RegionManager
import kotlinx.coroutines.*
import kotlin.io.path.Path
import kotlin.time.Duration
import kotlin.time.measureTime

fun main() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    RegionDataSerializer.register<TestData>()
    RegionDataSerializer.register<BrokenBlockData>()

    val regionManager = RegionManager(
        folder = Path("regions"),
        scope = scope
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            println("Saving all regions")
            regionManager.saveAll()
        }
    })

    runBlocking {
        val deferred = mutableListOf<Deferred<Unit>>()
        val durations = mutableListOf<Duration>()

        for (x in 0 until 1) {
            for (z in 0 until 1) {
                deferred.add(async {
                    durations.add(measureTime {
                        saveRegion(regionManager, x, z, this)
                    })

                    Unit
                })
            }
        }

        deferred.awaitAll()

        val min = durations.minOrNull() ?: Duration.ZERO
        val max = durations.maxOrNull() ?: Duration.ZERO
        val avg =
            if (durations.isNotEmpty()) durations.reduce { acc, d -> acc + d } / durations.size else Duration.ZERO

        println("Min save time: $min")
        println("Max save time: $max")
        println("Avg save time: $avg")
    }
}

suspend fun loadRegion(
    manager: RegionManager,
    x: Int,
    z: Int
): SurfRegion {
    return manager.loadRegion(x, z)
}

suspend fun saveRegion(
    manager: RegionManager,
    x: Int,
    z: Int,
    scope: CoroutineScope
) {
    val region = manager.loadRegion(x, z)

    region["brokenBlocks"] = BrokenBlockData(
        listOf(
            Triple(0, 0, 0)
        )
    )

    scope.launch {
        region.save()
        println("Region $x, $z saved")
    }
}