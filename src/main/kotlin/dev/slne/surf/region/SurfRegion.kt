@file:OptIn(InternalSerializationApi::class)

package dev.slne.surf.region

import dev.slne.surf.region.data.RegionData
import dev.slne.surf.region.data.RegionDataSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty

class SurfRegion(
    val x: Int,
    val z: Int,
    private val file: File
) {
    private val mutex = Mutex()
    private val cache = ConcurrentHashMap<String, RegionData>()

    @Volatile
    var loaded: Boolean = false
        private set

    @Volatile
    var dirty: Boolean = false
        private set

    suspend fun load() = mutex.withLock {
        if (!file.exists()) {
            cache.clear()
            loaded = true
            return@withLock
        }

        val content = withContext(Dispatchers.IO) { file.readText() }
        val jsonObject = SURF_REGION_JSON.parseToJsonElement(content).jsonObject

        cache.clear()

        jsonObject.forEach { (key, element) ->
            val value: RegionData? = try {
                SURF_REGION_JSON.decodeFromJsonElement(
                    RegionDataSerializer.serializersModule.serializer(),
                    element
                )
            } catch (_: Exception) {
                null
            }

            if (value != null) {
                cache[key] = value
            }
        }
    }

    suspend fun save() = mutex.withLock {
        if (loaded && !dirty) return@withLock

        val jsonObject = cache.mapValues { (_, value) ->
            SURF_REGION_JSON.encodeToJsonElement(
                RegionDataSerializer.serializersModule.serializer(),
                value
            )
        }

        withContext(Dispatchers.IO) {
            file.parentFile.mkdirs()
            file.writeText(SURF_REGION_JSON.encodeToString(jsonObject))
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : RegionData> getData(key: String): T? = cache[key] as? T

    fun setData(key: String, data: RegionData) {
        cache[key] = data
        dirty = true
    }

    operator fun <T : RegionData> get(key: String): T? = getData(key)
    operator fun set(key: String, data: RegionData) = setData(key, data)

    operator fun <T : RegionData> getValue(thisRef: Any?, property: KProperty<*>): T? =
        get(property.name)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: RegionData) =
        set(property.name, value)

    override fun toString(): String {
        return "SurfRegion(x=$x, z=$z, file=$file, mutex=$mutex, cache=$cache, loaded=$loaded, dirty=$dirty)"
    }
}