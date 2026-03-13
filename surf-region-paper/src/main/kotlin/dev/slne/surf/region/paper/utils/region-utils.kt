package dev.slne.surf.region.paper.utils

import dev.slne.surf.region.manager.RegionManager
import dev.slne.surf.region.paper.ModifiedBlockData
import dev.slne.surf.region.paper.PaperRegionInstance
import dev.slne.surf.region.region.SurfRegion
import org.bukkit.Location
import org.bukkit.block.Block

suspend fun RegionManager.getRegion(
    block: Block,
    loadIfNotLoaded: Boolean = true,
    createIfNotExists: Boolean = true,
) = getRegion(block.location, loadIfNotLoaded, createIfNotExists)

suspend fun RegionManager.getRegion(
    location: Location,
    loadIfNotLoaded: Boolean = true,
    createIfNotExists: Boolean = true,
) = getRegion(
    x = location.blockX shr 9,
    z = location.blockZ shr 9,
    loadIfNotLoaded = loadIfNotLoaded,
    createIfNotExists = createIfNotExists
)

suspend fun Block.isModified(instance: PaperRegionInstance) =
    location.isLocationModified(instance)

suspend fun Location.isLocationModified(instance: PaperRegionInstance): Boolean {
    val regionManager = instance.findRegionManager(world) ?: return false
    val region = regionManager.getRegion(
        location = this,
        loadIfNotLoaded = true,
        createIfNotExists = false
    ) ?: return false

    return region.isBlockModified(this)
}

fun SurfRegion.isBlockModified(location: Location) =
    isBlockModified(location.blockX, location.blockY, location.blockZ)

fun SurfRegion.isBlockModified(block: Block) =
    isBlockModified(block.location)

fun SurfRegion.isBlockModified(x: Int, y: Int, z: Int): Boolean {
    val block = getBlockAt(x, y, z)

    return block.hasData(ModifiedBlockData.MODIFIED_BLOCKS_KEY)
}

fun SurfRegion.markBlockModified(x: Int, y: Int, z: Int) {
    val block = getBlockAt(x, y, z)

    block.setData(ModifiedBlockData.MODIFIED_BLOCKS_KEY, ModifiedBlockData())
}

fun SurfRegion.markBlockUnmodified(x: Int, y: Int, z: Int) {
    val block = getBlockAt(x, y, z)

    block.removeData(ModifiedBlockData.MODIFIED_BLOCKS_KEY)
}