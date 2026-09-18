package com.kakaanime.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProviderCacheTest {

    @Test
    fun episodeCacheUsesFreshValueWithoutReload() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var now = 1_000L
        var calls = 0
        val cache = ProviderEpisodeCache(scope, nowMs = { now })

        val episodes = listOf(ProviderEpisode("ep-1", "anime-1", 1))
        assertEquals(episodes, cache.getOrLoad("anime-1") { calls++; episodes })
        assertEquals(episodes, cache.getOrLoad("anime-1") { calls++; error("should not load") })
        assertEquals(1, calls)
    }

    @Test
    fun episodeCacheRefreshesInBackgroundFromStaleValue() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var now = 1_000L
        var calls = 0
        val first = listOf(ProviderEpisode("ep-1", "anime-1", 1))
        val second = listOf(ProviderEpisode("ep-2", "anime-1", 2))
        val cache = ProviderEpisodeCache(scope, nowMs = { now })

        cache.getOrLoad("anime-1") { calls++; first }
        now = 16 * 60 * 1000L
        assertEquals(first, cache.getOrLoad("anime-1") { calls++; delay(10); second })
        delay(100)
        assertEquals(2, calls)
        assertEquals(second, cache.getOrLoad("anime-1") { calls++; error("should be fresh") })
    }

    @Test
    fun unreleasedEpisodeUsesShorterFreshTtl() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var now = 1_000L
        var calls = 0
        val pending = listOf(
            ProviderEpisode(
                id = "ep-1",
                animeId = "anime-1",
                number = 1,
                availability = EpisodeAvailability.NOT_RELEASED,
            )
        )
        val cache = ProviderEpisodeCache(scope, nowMs = { now })
        cache.getOrLoad("anime-1") { calls++; pending }

        now += 2 * 60 * 1000L + 1
        val loaded = cache.getOrLoad("anime-1") { calls++; pending }
        assertEquals(pending, loaded)
        delay(100)
        assertEquals(2, calls)
    }

    @Test
    fun streamCacheUsesShorterTtlForDegradedProviders() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var now = 1_000L
        var calls = 0
        val health = ProviderHealthMonitor()
        repeat(3) { health.recordFailure("degraded", 10, RuntimeException("x")) }

        val stream = NormalizedStream(
            providerId = "degraded",
            url = "https://example.test/stream.m3u8",
            quality = "1080p",
            type = StreamType.HLS,
        )
        val cache = ProviderStreamCache(scope, health, nowMs = { now })

        cache.getOrLoad("anime-1", 1) { calls++; listOf(stream) }
        now += 2 * 60 * 1000L + 1
        cache.getOrLoad("anime-1", 1) { calls++; listOf(stream) }
        delay(100)

        assertEquals(2, calls)
        assertTrue(health.snapshot("degraded").status == "FAILING")
    }
}
