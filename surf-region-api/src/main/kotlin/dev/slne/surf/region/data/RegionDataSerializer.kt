@file:OptIn(InternalSerializationApi::class)

package dev.slne.surf.region.data

import dev.slne.surf.region.chunk.SurfChunk
import dev.slne.surf.region.utils.InternalRegionApi
import dev.slne.surf.surfapi.core.api.serializer.SurfSerializerModule
import dev.slne.surf.surfapi.core.api.util.toObjectSet
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

@InternalRegionApi
object RegionDataSerializer {
    private val regionDataSerializers =
        mutableMapOf<KClass<out RegionData>, KSerializer<*>>()

    fun <T : RegionData> register(clazz: KClass<T>) {
        regionDataSerializers[clazz] = clazz.serializer()
    }

    inline fun <reified T : RegionData> register() {
        register(T::class)
    }

    val serializersModule
        get() = SerializersModule {
            include(SurfSerializerModule.all)

            polymorphic(RegionData::class) {
                regionDataSerializers.forEach { (clazz, serializer) ->
                    @Suppress("UNCHECKED_CAST")
                    subclass(clazz as KClass<RegionData>, serializer as KSerializer<RegionData>)
                }
            }
        }

    fun decode(data: String): Collection<SurfChunk> {
        val jsonElement = JSON.parseToJsonElement(data)

        val jsonArray = runCatching {
            jsonElement.jsonArray
        }.getOrNull() ?: error("Expected a JSON array")

        return jsonArray.mapNotNull { element ->
            try {
                JSON.decodeFromJsonElement<SurfChunk>(element)
            } catch (_: Exception) {
                null
            }
        }.toObjectSet()
    }

    fun encode(data: Collection<SurfChunk>): String {
        val jsonObject = data.map { chunk ->
            JSON.encodeToJsonElement(chunk)
        }

        return JSON.encodeToString(jsonObject)
    }

    private val JSON
        get() = Json {
            prettyPrint = false
            ignoreUnknownKeys = true
            serializersModule = RegionDataSerializer.serializersModule

            encodeDefaults = true
            explicitNulls = false
        }
}
