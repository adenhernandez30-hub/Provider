package com.kakaanime.provider

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

/**
 * One-run real-site smoke E2E for every provider registered by the production factory.
 *
 * Enable explicitly with ANILAB_REAL_E2E=true. The normal provider test suite never
 * touches external sites.
 */
@EnabledIfEnvironmentVariable(named = "ANILAB_REAL_E2E", matches = "true")
class ProviderRealSiteE2ETest {

    private data class StageResult(
        val name: String,
        var search: String = "SKIP",
        var anime: String = "SKIP",
        var episodes: String = "SKIP",
        var stream: String = "SKIP",
    )

    @Test
    fun runAllProvidersInOneBatch() = runBlocking {
        val query = System.getenv("ANILAB_E2E_QUERY").orEmpty().ifBlank { "One Piece" }
        val timeoutMs = System.getenv("ANILAB_E2E_TIMEOUT_MS")
            ?.toLongOrNull()
            ?.coerceIn(5_000L, 120_000L)
            ?: 60_000L

        val providers = ProviderFactory.createRegistry().all()
        val results = providers.map { provider ->
            runProvider(provider, query, timeoutMs)
        }

        println()
        println("=== AniLab Provider Real-Site E2E ===")
        println("Query: $query")
        println("Providers: ${results.size}")
        println()
        println("%-24s %-10s %-10s %-10s %-10s".format("PROVIDER", "SEARCH", "ANIME", "EPISODES", "STREAM"))
        results.forEach {
            println("%-24s %-10s %-10s %-10s %-10s".format(it.name, it.search, it.anime, it.episodes, it.stream))
        }
        println()

        val searchPass = results.count { it.search == "PASS" }
        val animePass = results.count { it.anime == "PASS" }
        val episodePass = results.count { it.episodes == "PASS" }
        val streamPass = results.count { it.stream == "PASS" }

        println("SEARCH PASS    : $searchPass/${results.size}")
        println("ANIME PASS     : $animePass/${results.size}")
        println("EPISODES PASS  : $episodePass/${results.size}")
        println("STREAM PASS    : $streamPass/${results.size}")

        assertTrue(
            results.isNotEmpty() && searchPass > 0,
            "No registered provider completed SEARCH for query '$query'."
        )
    }

    private suspend fun runProvider(
        provider: AnimeProvider,
        query: String,
        timeoutMs: Long,
    ): StageResult {
        val result = StageResult(provider.name)

        val search = runStage(timeoutMs) { provider.search(query) }
        val animeResults = search.getOrNull()
        result.search = when {
            search.isFailure -> "ERROR"
            animeResults.isNullOrEmpty() -> "EMPTY"
            else -> "PASS"
        }
        if (animeResults.isNullOrEmpty()) return result

        val selected = animeResults.first()
        val anime = runStage(timeoutMs) { provider.getAnime(selected.id) }
        result.anime = when {
            anime.isFailure -> "ERROR"
            anime.getOrNull() == null -> "EMPTY"
            else -> "PASS"
        }
        if (anime.getOrNull() == null) return result

        val episodes = runStage(timeoutMs) { provider.getEpisodes(selected.id) }
        val episodeList = episodes.getOrNull().orEmpty()
        result.episodes = when {
            episodes.isFailure -> "ERROR"
            episodeList.isEmpty() -> "EMPTY"
            else -> "PASS"
        }
        if (episodeList.isEmpty()) return result

        val firstEpisode = episodeList.minByOrNull { it.number } ?: return result
        val streams = runStage(timeoutMs) {
            provider.getStreams(selected.id, firstEpisode.number)
        }
        result.stream = when {
            streams.isFailure -> "ERROR"
            streams.getOrNull().orEmpty().any { it.url.isNotBlank() && it.type != StreamType.UNKNOWN } -> "PASS"
            else -> "EMPTY"
        }

        return result
    }

    private suspend fun <T> runStage(
        timeoutMs: Long,
        block: suspend () -> T,
    ): Result<T> = runCatching {
        withTimeout(timeoutMs) { block() }
    }
}
