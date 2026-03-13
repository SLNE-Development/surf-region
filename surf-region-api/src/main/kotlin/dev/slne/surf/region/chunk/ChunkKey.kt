package dev.slne.surf.region.chunk

fun chunkKey(x: Int, z: Int) =
    (x.toLong() shl 32) or (z.toLong() and 0xffffffffL)