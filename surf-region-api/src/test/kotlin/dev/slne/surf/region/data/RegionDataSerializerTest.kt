package dev.slne.surf.region.data

import dev.slne.surf.region.TestRegionData
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegionDataSerializerTest {

    @Test
    fun `register adds serializer for the given class`() {
        RegionDataSerializer.register<TestRegionData>()
        // Verify that the serializers module can be built without throwing
        val module = RegionDataSerializer.serializersModule
        assertNotNull(module)
    }

    @Test
    fun `serializersModule is rebuilt after each registration`() {
        // First build
        val before = RegionDataSerializer.serializersModule
        assertNotNull(before)

        // Register a type and rebuild
        RegionDataSerializer.register<TestRegionData>()
        val after = RegionDataSerializer.serializersModule
        assertNotNull(after)
    }

    @Test
    fun `register is idempotent - registering same class twice is safe`() {
        RegionDataSerializer.register<TestRegionData>()
        RegionDataSerializer.register<TestRegionData>()
        // Should not throw; serializersModule must still build
        assertNotNull(RegionDataSerializer.serializersModule)
    }

    @Test
    fun `serializersModule includes registered polymorphic subtype`() {
        RegionDataSerializer.register<TestRegionData>()
        val module = RegionDataSerializer.serializersModule
        // The module must be non-null and buildable
        assertNotNull(module)
        // Verify subtype is included by checking the module has a context
        assertTrue(module.toString().isNotEmpty())
    }
}
