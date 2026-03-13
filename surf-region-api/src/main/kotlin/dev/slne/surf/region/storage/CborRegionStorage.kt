@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.region.storage

import dev.slne.surf.region.chunk.SurfChunk
import dev.slne.surf.region.data.RegionDataSerializer
import dev.slne.surf.region.region.SurfRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

class CborRegionStorage(
    region: SurfRegion
) : RegionStorage<ByteArray>(region, "cbor") {
    private val cbor
        get() = Cbor {
            ignoreUnknownKeys = true
            serializersModule = RegionDataSerializer.serializersModule

            encodeDefaults = false
        }

    override fun encode(data: Collection<SurfChunk>): ByteArray {
        return cbor.encodeToByteArray(data)
    }

    override fun decode(data: ByteArray): Collection<SurfChunk> {
        return cbor.decodeFromByteArray(data)
    }

    override suspend fun write(data: ByteArray) = withContext(Dispatchers.IO) {
        file.writeBytes(data)
    }

    override suspend fun read(): ByteArray = withContext(Dispatchers.IO) {
        file.readBytes()
    }
}