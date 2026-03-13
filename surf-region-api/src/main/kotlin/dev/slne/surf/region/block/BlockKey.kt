package dev.slne.surf.region.block

fun blockKey(x: Int, y: Int, z: Int): Long {
    return (x.toLong() and 0x3FFFFFF) shl 38 or
            (y.toLong() and 0xFFF) shl 26 or
            (z.toLong() and 0x3FFFFFF)
}