package com.kakaanime.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ProviderEngine(
    private val registry: ProviderRegistry,
    private val healthMonitor: ProviderHealthMonitor = ProviderHealthMonitor(),
) {
    private val router = SmartProviderRouter(registry, healthMonitor)
    private val race = RaceStreamEngine(registry, healthMonitor)
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val episodeCache = ProviderEpisodeCache(cacheScope)
    private val streamCache = ProviderStreamCache(cacheScope, healthMonitor)

    fun healthMonitor(): ProviderHealthMonitor = healthMonitor

    fun providers(): List<AnimeProvider> = registry.all()

    suspend fun search(query: String): List<ProviderAnime> = router.search(query)

    suspend fun getAnime(animeId: String): ProviderAnime? = router.getAnime(animeId)

    suspend fun getEpisodes(animeId: String): List<ProviderEpisode> =
        episodeCache.getOrLoad(animeId) { router.getEpisodes(animeId) }

    suspend fun getEpisodeStreams(animeId: String, episodeNumber: Int): NormalizedEpisodeStream {
        val streams = streamCache.getOrLoad(animeId, episodeNumber) {
            val raw = router.getStreams(animeId, episodeNumber)
            val priorities = registry.all().associate { it.id to it.priority }
            val deduplicated = ProviderStreamDeduplicator.deduplicate(raw, priorities)
            StreamNormalizer.normalize(deduplicated)
        }
        return NormalizedEpisodeStream(
            animeId = animeId,
            episodeNumber = episodeNumber,
            streams = streams
        )
    }

    /**
     * Returns the first usable stream produced by any registered provider.
     * This is intended for low-latency playback through the backend.
     */
    suspend fun getFirstStream(animeId: String, episodeNumber: Int): ProviderStream? =
        race.getFirstStream(animeId, episodeNumber)

    suspend fun getBestStream(
        animeId: String,
        episodeNumber: Int,
        preferredQuality: StreamQuality? = null,
        premium: Boolean = false
    ): NormalizedStream? = StreamSelector.best(
        streams = getEpisodeStreams(animeId, episodeNumber).streams,
        preferredQuality = preferredQuality,
        premium = premium
    )
}
