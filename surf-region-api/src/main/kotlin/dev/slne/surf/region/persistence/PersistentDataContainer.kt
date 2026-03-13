package dev.slne.surf.region.persistence

import dev.slne.surf.region.data.RegionData
import dev.slne.surf.surfapi.core.api.serializer.adventure.key.SerializableKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.kyori.adventure.key.Key

typealias PersistentData = MutableMap<SerializableKey, RegionData>

@Serializable
data class PersistentDataContainer(
    private val data: PersistentData = mutableMapOf()
) {
    @Transient
    private var _dirty: Boolean = false
    val isDirty get() = _dirty

    @Suppress("UNCHECKED_CAST")
    fun <T : RegionData> getData(key: Key): T? {
        return data[key] as T?
    }

    fun setData(key: Key, regionData: RegionData) {
        data[key] = regionData
    }

    fun computeData(key: Key, block: (Key, RegionData?) -> RegionData?) {
        data.compute(key) { k, existing ->
            block(k, existing)
        }

        _dirty = true
    }

    fun markClean() {
        _dirty = false
    }
}