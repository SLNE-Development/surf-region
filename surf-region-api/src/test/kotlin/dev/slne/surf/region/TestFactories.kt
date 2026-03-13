package dev.slne.surf.region

import dev.slne.surf.region.RegionInstance
import dev.slne.surf.region.manager.RegionManager
import dev.slne.surf.region.region.SurfRegion
import dev.slne.surf.region.storage.JsonRegionStorage
import java.nio.file.Path
import java.util.UUID

/** Convenience factory: a [RegionInstance] backed by [folder] using [JsonRegionStorage]. */
internal fun testInstance(folder: Path): RegionInstance =
    RegionInstance(folder) { JsonRegionStorage(it) }

/** Convenience factory: a [RegionManager] for [worldId] inside [folder]. */
internal fun testManager(worldId: UUID = UUID.randomUUID(), folder: Path, instance: RegionInstance = testInstance(folder)): RegionManager =
    RegionManager(worldId, folder, instance)

/** Convenience factory: an unloaded [SurfRegion] at region coords (0,0). */
internal fun testRegion(worldId: UUID = UUID.randomUUID(), x: Int = 0, z: Int = 0, instance: RegionInstance): SurfRegion =
    SurfRegion(worldId, x, z, instance)
