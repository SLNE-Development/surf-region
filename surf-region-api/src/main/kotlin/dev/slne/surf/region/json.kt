package dev.slne.surf.region

import dev.slne.surf.region.data.RegionDataSerializer
import kotlinx.serialization.json.Json

val SURF_REGION_JSON = Json {
    prettyPrint = false
    ignoreUnknownKeys = true
    serializersModule = RegionDataSerializer.serializersModule
}
