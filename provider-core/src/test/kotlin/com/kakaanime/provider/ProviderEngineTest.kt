package com.kakaanime.provider

import kotlin.test.Test
import kotlin.test.assertEquals

class ProviderEngineTest {

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
    fun providersReflectRegistryOrderingAndDeduplication() {
        val registry = ProviderRegistry().apply {
            register(provider("late", 50))
            register(provider("first", 10))
            register(provider("first", 1))
            register(provider("middle", 30))
        }

        val engine = ProviderEngine(registry)

        assertEquals(listOf("first", "middle", "late"), engine.providers().map { it.id })
    }
}
