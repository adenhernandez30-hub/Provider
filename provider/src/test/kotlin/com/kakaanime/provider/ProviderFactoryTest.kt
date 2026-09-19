package com.kakaanime.provider

import kotlin.test.Test
import kotlin.test.assertEquals

class ProviderFactoryTest {

    @Test
    fun createRegistryRegistersThirtyUniqueProviders() {
        val providers = ProviderFactory.createRegistry().all()

        assertEquals(30, providers.size)
        assertEquals(30, providers.map { it.id }.distinct().size)
    }
}
