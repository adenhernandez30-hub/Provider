package com.kakaanime.provider

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

class ProviderPerformanceComparisonTest {

    @Test
    fun boundedParallelSearchIsFasterThanSequentialBaseline() = runBlocking {
        val providers = (1..6).map { index ->
            object : AnimeProvider {
                override val id = "perf-$index"
                override val name = id
                override val priority = index
                override suspend fun search(query: String): List<ProviderAnime> {
                    delay(100)
                    return listOf(ProviderAnime(id, "Result $index", providerId = id))
                }
                override suspend fun getAnime(animeId: String) = null
                override suspend fun getEpisodes(animeId: String) = emptyList<ProviderEpisode>()
                override suspend fun getStreams(animeId: String, episodeNumber: Int) = emptyList<ProviderStream>()
            }
        }

        val registry = ProviderRegistry().also { it.registerAll(providers) }

        val sequentialMs = measureTimeMillis {
            providers.forEach { it.search("benchmark") }
        }
        val parallelMs = measureTimeMillis {
            SmartProviderRouter(
                registry = registry,
                maxParallelProviders = 3,
            ).search("benchmark")
        }

        println("=== AniLab Provider Performance Comparison ===")
        println("Sequential baseline: ${sequentialMs}ms")
        println("Bounded parallel:    ${parallelMs}ms")
        println("Speedup:             %.2fx".format(sequentialMs.toDouble() / parallelMs.coerceAtLeast(1)))

        assertTrue(parallelMs < sequentialMs, "bounded parallel should beat sequential baseline")
    }
}
