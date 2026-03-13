package dev.slne.surf.region.handler

import dev.slne.surf.region.region.SurfRegion

fun interface RegionHandler {
    fun handle(region: SurfRegion)
}