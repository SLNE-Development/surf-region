package dev.slne.surf.region.data

import kotlinx.serialization.Serializable

/**
 * A three-dimensional block coordinate in world space.
 */
@Serializable
data class BlockPosition(val x: Int, val y: Int, val z: Int)
