package com.kakaanime.provider

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
    private val registry: ProviderRegistry
) {
    private data class Result(val streams: List<ProviderStream>)

    suspend fun getFirstStream(animeId: String, episodeNumber: Int): ProviderStream? = coroutineScope {
        val providers = registry.all()
        if (providers.isEmpty()) return@coroutineScope null

        val results = Channel<Result>(Channel.UNLIMITED)
        val jobs = providers.map { provider ->
            launch {
                val streams = runCatching {
                    provider.getStreams(animeId, episodeNumber)
                }.getOrDefault(emptyList())

                val usable = streams.filter { it.url.isNotBlank() && it.type != StreamType.UNKNOWN }
                if (usable.isNotEmpty()) results.send(Result(usable))
            }
        }

        try {
            results.receive().streams.firstOrNull()
        } finally {
            jobs.forEach { it.cancel() }
            results.close()
        }
    }
}
