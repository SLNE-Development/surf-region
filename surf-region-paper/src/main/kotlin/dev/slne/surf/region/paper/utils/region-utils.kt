package dev.slne.surf.region.paper.utils

import dev.slne.surf.region.paper.ModifiedBlocksData
import dev.slne.surf.region.region.SurfRegion

fun SurfRegion.isBlockModified(x: Int, y: Int, z: Int): Boolean {
    val block = getBlockAt(x, y, z)
    val data = block.getData<ModifiedBlocksData>(ModifiedBlocksData.MODIFIED_BLOCKS_KEY)

    return data?.contains(x, y, z) ?: false
}

fun SurfRegion.markBlockModified(x: Int, y: Int, z: Int) {
    val block = getBlockAt(x, y, z)

    block.computeData(ModifiedBlocksData.MODIFIED_BLOCKS_KEY) { _, existing ->
        val data = existing as? ModifiedBlocksData ?: ModifiedBlocksData()

        data.withModified(x, y, z)
    }
}

fun SurfRegion.markBlockUnmodified(x: Int, y: Int, z: Int) {
    val block = getBlockAt(x, y, z)

    block.computeData(ModifiedBlocksData.MODIFIED_BLOCKS_KEY) { _, existing ->
        val data = existing as? ModifiedBlocksData ?: return@computeData null

        data.withUnmodified(x, y, z)
    }
}