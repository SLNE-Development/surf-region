package dev.slne.surf.region.paper

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import dev.slne.surf.region.paper.utils.isBlockModified
import dev.slne.surf.surfapi.bukkit.api.extensions.server
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import kotlin.time.Duration.Companion.milliseconds

class VisualizerJob(
    val instance: PaperRegionInstance
) {
    private lateinit var job: Job

    fun start() {
        job = instance.plugin.launch {
            while (isActive) {
                for (manager in instance.regionManagers) {
                    for (region in manager.regions) {
                        for (chunk in region.chunks) {
                            for (section in chunk.sections) {
                                for (block in section.blocks) {
                                    val modified = region.isBlockModified(block.x, block.y, block.z)
                                    println("Block $modified $block")
                                    if (!modified) continue

                                    val location = Location(
                                        server.getWorld(region.worldId)
                                            ?: error("World with UUID ${region.worldId} not found"),
                                        block.x.toDouble(),
                                        block.y.toDouble(),
                                        block.z.toDouble()
                                    )

                                    withContext(instance.plugin.regionDispatcher(location)) {
                                        Particle.DUST.builder()
                                            .count(5)
                                            .color(Color.RED)
                                            .allPlayers()
                                            .location(location.add(0.5, 0.5, 0.5))
                                            .spawn()
                                    }
                                }
                            }
                        }
                    }
                }

                delay(500.milliseconds)
            }
        }
    }

    fun stop() {
        if (::job.isInitialized && job.isActive) {
            job.cancel()
        }
    }
}