package com.kakaanime.provider

import kotlinx.coroutines.CoroutineScope

class ProviderStreamCache(
    scope: CoroutineScope,
    private val healthMonitor: ProviderHealthMonitor,
    nowMs: () -> Long = { System.currentTimeMillis() },
    private val healthyFreshTtlMs: Long = 10 * 60 * 1000L,
    private val degradedFreshTtlMs: Long = 2 * 60 * 1000L,
    private val failingFreshTtlMs: Long = 30 * 1000L,
    private val unknownFreshTtlMs: Long = 60 * 1000L,
    private val emptyFreshTtlMs: Long = 30 * 1000L,
    private val staleTtlMs: Long = 30 * 60 * 1000L,
) {
    private data class Key(val animeId: String, val episodeNumber: Int)

    private val cache = ProviderTimedCache<Key, List<NormalizedStream>>(scope, nowMs)

    suspend fun getOrLoad(
        animeId: String,
        episodeNumber: Int,
        loader: suspend () -> List<NormalizedStream>,
    ): List<NormalizedStream> =
        cache.getOrLoad(
            key = Key(animeId, episodeNumber),
            freshTtlMs = ::freshTtl,
            staleTtlMs = { staleTtlMs },
            loader = loader,
        )

    suspend fun invalidate(animeId: String, episodeNumber: Int) =
        cache.invalidate(Key(animeId, episodeNumber))

    suspend fun clear() = cache.clear()

    private fun freshTtl(streams: List<NormalizedStream>): Long {
        if (streams.isEmpty()) return emptyFreshTtlMs
        return streams.minOf { stream ->
            when (healthMonitor.snapshot(stream.providerId).status) {
                "HEALTHY" -> healthyFreshTtlMs
                "DEGRADED" -> degradedFreshTtlMs
                "FAILING" -> failingFreshTtlMs
                else -> unknownFreshTtlMs
            }
        }
    }
}
