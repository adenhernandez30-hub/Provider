package com.kakaanime.provider

import kotlin.test.Test
import kotlin.test.assertEquals

class ProviderRegistryTest {

    private fun provider(id: String, priority: Int) = object : AnimeProvider {
        override val id = id
        override val name = id
        override val priority = priority

        override suspend fun search(query: String) = emptyList<ProviderAnime>()
        override suspend fun getAnime(animeId: String) = null
        override suspend fun getEpisodes(animeId: String) = emptyList<ProviderEpisode>()
        override suspend fun getStreams(animeId: String, episodeNumber: Int) = emptyList<ProviderStream>()
    }

    @Test
    fun registerIgnoresDuplicateProviderId() {
        val registry = ProviderRegistry()
        val first = provider("duplicate", 50)
        val duplicate = provider("duplicate", 1)

        registry.register(first)
        registry.register(duplicate)

        assertEquals(listOf(first), registry.all())
    }

    @Test
    fun allReturnsProvidersSortedByPriority() {
        val low = provider("low", 30)
        val high = provider("high", 10)
        val middle = provider("middle", 20)

        val registry = ProviderRegistry().apply {
            register(low)
            register(high)
            register(middle)
        }

        assertEquals(listOf("high", "middle", "low"), registry.all().map { it.id })
    }

    @Test
    fun registerAllDeduplicatesWithoutDroppingDistinctProviders() {
        val first = provider("first", 30)
        val duplicate = provider("first", 5)
        val second = provider("second", 10)

        val registry = ProviderRegistry().apply {
            registerAll(listOf(first, duplicate, second))
        }

        assertEquals(listOf("second", "first"), registry.all().map { it.id })
    }
}
