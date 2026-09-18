package com.kakaanime.provider

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Provider router with bounded fan-out and adaptive provider ordering. */
class SmartProviderRouter(
    private val registry: ProviderRegistry,
    private val healthMonitor: ProviderHealthMonitor = ProviderHealthMonitor(),
    private val maxParallelProviders: Int = DEFAULT_MAX_PARALLEL_PROVIDERS,
) {
    init {
        require(maxParallelProviders > 0) { "maxParallelProviders must be positive" }
    }

    fun healthMonitor(): ProviderHealthMonitor = healthMonitor

    suspend fun search(query: String): List<ProviderAnime> {
        if (query.isBlank()) return emptyList()
        return parallelRequests { provider -> provider.search(query) }
            .flatMap { it.getOrDefault(emptyList()) }
            .distinctBy { "${it.providerId}:${it.id}" }
    }

    suspend fun getAnime(animeId: String): ProviderAnime? {
        for (provider in orderedProviders()) {
            val result = request(provider) { provider.getAnime(animeId) }.getOrNull()
            if (result != null) return result
        }
        return null
    }

    suspend fun getEpisodes(animeId: String): List<ProviderEpisode> {
        for (provider in orderedProviders()) {
            val result = request(provider) { provider.getEpisodes(animeId) }
                .getOrDefault(emptyList())
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    suspend fun getStreams(animeId: String, episodeNumber: Int): List<ProviderStream> =
        parallelRequests { provider -> provider.getStreams(animeId, episodeNumber) }
            .flatMap { it.getOrDefault(emptyList()) }

    private fun orderedProviders(): List<AnimeProvider> =
        ProviderRoutingPolicy.order(registry.all(), healthMonitor)

    private suspend fun <T> parallelRequests(
        block: suspend (AnimeProvider) -> T,
    ): List<Result<T>> = coroutineScope {
        val semaphore = Semaphore(maxParallelProviders)
        orderedProviders().map { provider ->
            async {
                semaphore.withPermit {
                    request(provider) { block(provider) }
                }
            }
        }.awaitAll()
    }

    private suspend fun <T> request(
        provider: AnimeProvider,
        block: suspend () -> T,
    ): Result<T> {
        val startedAt = System.nanoTime()
        return runCatching { block() }
            .onSuccess { healthMonitor.recordSuccess(provider.id, elapsedMs(startedAt)) }
            .onFailure { error -> healthMonitor.recordFailure(provider.id, elapsedMs(startedAt), error) }
    }

    private fun elapsedMs(startedAt: Long): Long =
        ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(0L)

    companion object {
        const val DEFAULT_MAX_PARALLEL_PROVIDERS: Int = 4
    }
}