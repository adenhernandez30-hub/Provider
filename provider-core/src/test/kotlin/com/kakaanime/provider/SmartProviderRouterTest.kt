package com.kakaanime.provider

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmartProviderRouterTest {

    @Test
    fun searchRunsProvidersInBoundedParallel() = runBlocking {
        var active = 0
        var peak = 0

        fun provider(id: String) = object : AnimeProvider {
            override val id = id
            override val name = id
            override val priority = 1
            override suspend fun search(query: String): List<ProviderAnime> {
                active++
                peak = maxOf(peak, active)
                delay(40)
                active--
                return listOf(ProviderAnime(id, id, providerId = id))
            }
            override suspend fun getAnime(animeId: String) = null
            override suspend fun getEpisodes(animeId: String) = emptyList<ProviderEpisode>()
            override suspend fun getStreams(animeId: String, episodeNumber: Int) = emptyList<ProviderStream>()
        }

        val registry = ProviderRegistry()
        registry.registerAll((1..6).map { provider("p$it") })

        val result = SmartProviderRouter(registry, maxParallelProviders = 2).search("one piece")

        assertEquals(6, result.size)
        assertTrue(peak <= 2, "peak concurrency was $peak")
    }

    @Test
    fun failingProviderDoesNotCancelOtherSearches() = runBlocking {
        val registry = ProviderRegistry()

        val failing = object : AnimeProvider {
            override val id = "failing"
            override val name = id
            override val priority = 1
            override suspend fun search(query: String): List<ProviderAnime> {
                delay(10)
                error("boom")
            }
            override suspend fun getAnime(animeId: String) = null
            override suspend fun getEpisodes(animeId: String) = emptyList<ProviderEpisode>()
            override suspend fun getStreams(animeId: String, episodeNumber: Int) = emptyList<ProviderStream>()
        }

        val healthy = object : AnimeProvider {
            override val id = "healthy"
            override val name = id
            override val priority = 2
            override suspend fun search(query: String) =
                listOf(ProviderAnime("one", "One Piece", providerId = id))
            override suspend fun getAnime(animeId: String) = null
            override suspend fun getEpisodes(animeId: String) = emptyList<ProviderEpisode>()
            override suspend fun getStreams(animeId: String, episodeNumber: Int) = emptyList<ProviderStream>()
        }

        registry.registerAll(listOf(failing, healthy))
        val monitor = ProviderHealthMonitor()

        val result = SmartProviderRouter(registry, monitor, maxParallelProviders = 2)
            .search("one piece")

        assertEquals(listOf("healthy"), result.map { it.providerId })
        assertEquals(1, monitor.snapshot("failing").failedRequests)
        assertEquals(1, monitor.snapshot("healthy").successfulRequests)
    }

    @Test
    fun singleResultOperationsKeepSequentialFallback() = runBlocking {
        val registry = ProviderRegistry()
        val first = object : AnimeProvider {
            override val id = "first"
            override val name = id
            override val priority = 1
            override suspend fun search(query: String) = emptyList<ProviderAnime>()
            override suspend fun getAnime(animeId: String) = null
            override suspend fun getEpisodes(animeId: String) = emptyList<ProviderEpisode>()
            override suspend fun getStreams(animeId: String, episodeNumber: Int) = emptyList<ProviderStream>()
        }
        val second = object : AnimeProvider {
            override val id = "second"
            override val name = id
            override val priority = 2
            override suspend fun search(query: String) = emptyList<ProviderAnime>()
            override suspend fun getAnime(animeId: String) = ProviderAnime(animeId, "Found", providerId = id)
            override suspend fun getEpisodes(animeId: String) = emptyList<ProviderEpisode>()
            override suspend fun getStreams(animeId: String, episodeNumber: Int) = emptyList<ProviderStream>()
        }
        registry.registerAll(listOf(first, second))

        assertEquals("second", SmartProviderRouter(registry).getAnime("a")?.providerId)
    }
}
