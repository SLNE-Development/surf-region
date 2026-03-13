package dev.slne.surf.region.persistence

import dev.slne.surf.region.region.data.RegionData
import dev.slne.surf.surfapi.core.api.serializer.adventure.key.SerializableKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.kyori.adventure.key.Key


@Serializable
data class PersistentDataContainer(
    private val data: MutableMap<SerializableKey, RegionData> = mutableMapOf()
) {
    @Transient
    private var _dirty: Boolean = false
    val isDirty get() = _dirty

    fun hasData(key: Key): Boolean =
        data.containsKey(key)

    @Suppress("UNCHECKED_CAST")
    fun <T : RegionData> getData(key: Key): T? {
        return data[key] as T?
    }

    fun setData(key: Key, regionData: RegionData) {
        data[key] = regionData

        _dirty = true
    }

    fun computeData(key: Key, block: (Key, RegionData?) -> RegionData?) {
        data.compute(key) { k, existing ->
            block(k, existing)
        }

        _dirty = true
    }

    fun removeData(key: Key) {
        data.remove(key)
    }

    fun markClean() {
        _dirty = false
    }
}