package dev.slne.surf.region.persistence

import dev.slne.surf.region.data.RegionData
import net.kyori.adventure.key.Key

interface HasPersistentData {
    val persistentDataContainer: PersistentDataContainer
    val isDirty get() = persistentDataContainer.isDirty

    @Suppress("UNCHECKED_CAST")
    fun <T : RegionData> getData(key: Key): T? =
        persistentDataContainer.getData(key)

    fun setData(key: Key, data: RegionData) =
        persistentDataContainer.setData(key, data)

    fun computeData(key: Key, block: (Key, RegionData?) -> RegionData?) =
        persistentDataContainer.computeData(key, block)

    fun markClean() {
        persistentDataContainer.markClean()
    }
}