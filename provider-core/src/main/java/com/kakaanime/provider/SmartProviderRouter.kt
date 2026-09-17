package com.kakaanime.provider

/**
 * Provider router with lightweight request telemetry.
 * Provider calls remain sequential so routing behavior stays predictable
 * while the backend gains visibility into provider latency and failures.
 */
class SmartProviderRouter(
    private val registry: ProviderRegistry,
    private val healthMonitor: ProviderHealthMonitor = ProviderHealthMonitor(),
) {
    fun healthMonitor(): ProviderHealthMonitor = healthMonitor

    suspend fun search(query: String): List<ProviderAnime> {
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<ProviderAnime>()
        for (provider in registry.all()) {
            request(provider) { provider.search(query) }
                .getOrDefault(emptyList())
                .let(results::addAll)
        }
        return results.distinctBy { "${it.providerId}:${it.id}" }
    }

    suspend fun getAnime(animeId: String): ProviderAnime? {
        for (provider in registry.all()) {
            val result = request(provider) { provider.getAnime(animeId) }.getOrNull()
            if (result != null) return result
        }
        return null
    }

    suspend fun getEpisodes(animeId: String): List<ProviderEpisode> {
        for (provider in registry.all()) {
            val result = request(provider) { provider.getEpisodes(animeId) }
                .getOrDefault(emptyList())
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    suspend fun getStreams(animeId: String, episodeNumber: Int): List<ProviderStream> {
        val streams = mutableListOf<ProviderStream>()
        for (provider in registry.all()) {
            request(provider) { provider.getStreams(animeId, episodeNumber) }
                .getOrDefault(emptyList())
                .let(streams::addAll)
        }
        return streams
    }

    private suspend fun <T> request(
        provider: AnimeProvider,
        block: suspend () -> T,
    ): Result<T> {
        val startedAt = System.nanoTime()
        return runCatching { block() }
            .onSuccess {
                healthMonitor.recordSuccess(provider.id, elapsedMs(startedAt))
            }
            .onFailure { error ->
                healthMonitor.recordFailure(provider.id, elapsedMs(startedAt), error)
            }
    }

    private fun elapsedMs(startedAt: Long): Long =
        ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(0L)
}
