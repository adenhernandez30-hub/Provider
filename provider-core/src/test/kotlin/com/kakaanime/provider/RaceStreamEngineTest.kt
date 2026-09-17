package com.kakaanime.provider

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RaceStreamEngineTest {

    private fun provider(
        id: String,
        priority: Int,
        delayMs: Long,
        stream: ProviderStream
    ) = object : AnimeProvider {
        override val id = id
        override val name = id
        override val priority = priority

        override suspend fun search(query: String) = emptyList<ProviderAnime>()
        override suspend fun getAnime(animeId: String) = null
        override suspend fun getEpisodes(animeId: String) = emptyList<ProviderEpisode>()

        override suspend fun getStreams(
            animeId: String,
            episodeNumber: Int
        ): List<ProviderStream> {
            delay(delayMs)
            return listOf(stream)
        }
    }

    @Test
    fun firstUsableStreamWins() = runBlocking {
        val registry = ProviderRegistry()

        val slow = provider(
            id = "slow",
            priority = 1,
            delayMs = 200,
            stream = ProviderStream(
                providerId = "slow",
                url = "https://slow.example/stream.m3u8",
                type = StreamType.HLS
            )
        )

        val fast = provider(
            id = "fast",
            priority = 2,
            delayMs = 10,
            stream = ProviderStream(
                providerId = "fast",
                url = "https://fast.example/stream.m3u8",
                type = StreamType.HLS
            )
        )

        registry.registerAll(listOf(slow, fast))

        val result = RaceStreamEngine(registry)
            .getFirstStream("anime-1", 1)

        assertEquals("fast", result?.providerId)
        assertEquals("https://fast.example/stream.m3u8", result?.url)
    }

    @Test
    fun returnsNullWhenNoProviderIsRegistered() = runBlocking {
        val registry = ProviderRegistry()

        val result = RaceStreamEngine(registry)
            .getFirstStream("anime-1", 1)

        assertNull(result)
    }
}
