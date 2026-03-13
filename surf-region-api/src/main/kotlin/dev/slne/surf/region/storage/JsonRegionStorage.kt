package dev.slne.surf.region.storage

import dev.slne.surf.region.chunk.SurfChunk
import dev.slne.surf.region.data.RegionDataSerializer
import dev.slne.surf.region.region.SurfRegion
import dev.slne.surf.surfapi.core.api.util.toObjectSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray

class JsonRegionStorage(
    region: SurfRegion
) : RegionStorage<String>(region, "json") {
    private val json
        get() = Json {
            prettyPrint = false
            ignoreUnknownKeys = true
            serializersModule = RegionDataSerializer.serializersModule

            encodeDefaults = false
            explicitNulls = false
        }

    override suspend fun read(): String = withContext(Dispatchers.IO) {
        return@withContext file.readText()
    }

    override suspend fun write(data: String) = withContext(Dispatchers.IO) {
        file.writeText(data)
    }

    override fun encode(data: Collection<SurfChunk>): String {
        val jsonObject = data.map { chunk ->
            json.encodeToJsonElement(chunk)
        }

        return json.encodeToString(jsonObject)
    }

    override fun decode(data: String): Collection<SurfChunk> {
        val jsonElement = json.parseToJsonElement(data)

        val jsonArray = runCatching {
            jsonElement.jsonArray
        }.getOrNull() ?: error("Expected a JSON array")

        return jsonArray.mapNotNull { element ->
            try {
                json.decodeFromJsonElement<SurfChunk>(element)
            } catch (_: Exception) {
                null
            }
        }.toObjectSet()
    }
}