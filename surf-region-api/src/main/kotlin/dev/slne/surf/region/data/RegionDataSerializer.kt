@file:OptIn(InternalSerializationApi::class)

package dev.slne.surf.region.data

import dev.slne.surf.region.utils.InternalRegionApi
import dev.slne.surf.surfapi.core.api.serializer.SurfSerializerModule
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
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
}
