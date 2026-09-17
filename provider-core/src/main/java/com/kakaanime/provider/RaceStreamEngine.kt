package com.kakaanime.provider

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Races registered providers for an episode stream.
 *
 * The first provider that returns at least one usable stream wins. Other
 * provider requests are cancelled immediately so the player can start as
 * soon as possible instead of waiting for every provider.
 */
class RaceStreamEngine(
    private val registry: ProviderRegistry,
    private val healthMonitor: ProviderHealthMonitor = ProviderHealthMonitor(),
) {
    private data class Result(val streams: List<ProviderStream>)

    fun healthMonitor(): ProviderHealthMonitor = healthMonitor

    suspend fun getFirstStream(animeId: String, episodeNumber: Int): ProviderStream? = coroutineScope {
        val providers = registry.all()
        if (providers.isEmpty()) return@coroutineScope null

        val results = Channel<Result>(Channel.UNLIMITED)
        val jobs = providers.map { provider ->
            launch {
                val startedAt = System.nanoTime()
                try {
                    val streams = provider.getStreams(animeId, episodeNumber)
                    healthMonitor.recordSuccess(provider.id, elapsedMs(startedAt))

                    val usable = streams.filter { it.url.isNotBlank() && it.type != StreamType.UNKNOWN }
                    if (usable.isNotEmpty()) results.send(Result(usable))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    healthMonitor.recordFailure(provider.id, elapsedMs(startedAt), e)
                }
            }
        }

        try {
            results.receive().streams.firstOrNull()
        } finally {
            jobs.forEach { it.cancel() }
            results.close()
        }
    }

    private fun elapsedMs(startedAt: Long): Long =
        ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(0L)
}
