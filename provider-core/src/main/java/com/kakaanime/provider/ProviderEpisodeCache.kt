package com.kakaanime.provider

import kotlinx.coroutines.CoroutineScope

class ProviderEpisodeCache(
    scope: CoroutineScope,
    nowMs: () -> Long = { System.currentTimeMillis() },
    private val normalFreshTtlMs: Long = 15 * 60 * 1000L,
    private val pendingFreshTtlMs: Long = 2 * 60 * 1000L,
    private val staleTtlMs: Long = 60 * 60 * 1000L,
) {
    private val cache = ProviderTimedCache<String, List<ProviderEpisode>>(scope, nowMs)

    init {
        require(normalFreshTtlMs > 0)
        require(pendingFreshTtlMs > 0)
        require(staleTtlMs >= normalFreshTtlMs)
    }

    suspend fun getOrLoad(
        animeId: String,
        loader: suspend () -> List<ProviderEpisode>,
    ): List<ProviderEpisode> =
        cache.getOrLoad(
            key = animeId,
            freshTtlMs = { episodes ->
                if (episodes.any { it.availability == EpisodeAvailability.NOT_RELEASED }) {
                    pendingFreshTtlMs
                } else {
                    normalFreshTtlMs
                }
            },
            staleTtlMs = { staleTtlMs },
            loader = loader,
        )

    suspend fun invalidate(animeId: String) = cache.invalidate(animeId)

    suspend fun clear() = cache.clear()
}
