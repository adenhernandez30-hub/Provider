package com.kakaanime.provider

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@EnabledIfEnvironmentVariable(named = "ANILAB_REAL_E2E", matches = "true")
class ProviderRealSiteE2ETest {
    private data class StageResult(
        val name: String,
        var search: String = "SKIP",
        var anime: String = "SKIP",
        var episodes: String = "SKIP",
        var stream: String = "SKIP",
        val details: MutableList<String> = mutableListOf(),
    )

    @Test
    fun runAllProvidersInOneBatch() = runBlocking {
        val query = System.getenv("ANILAB_E2E_QUERY").orEmpty().ifBlank { "One Piece" }
        val timeoutMs = System.getenv("ANILAB_E2E_TIMEOUT_MS")?.toLongOrNull()?.coerceIn(5_000L, 120_000L) ?: 60_000L
        val providers = ProviderFactory.createRegistry().all()
        val results = providers.map { provider -> runProvider(provider, query, timeoutMs) }

        println()
        println("=== AniLab Provider Real-Site E2E ===")
        println("Query: $query")
        println("Providers: ${results.size}")
        println()
        println("%-24s %-10s %-10s %-10s %-10s".format("PROVIDER", "SEARCH", "ANIME", "EPISODES", "STREAM"))
        results.forEach { println("%-24s %-10s %-10s %-10s %-10s".format(it.name, it.search, it.anime, it.episodes, it.stream)) }
        println()
        results.filter { it.details.isNotEmpty() }.forEach { result ->
            result.details.forEach { detail -> println("[E2E] ${result.name}: $detail") }
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
        assertTrue(results.isNotEmpty() && searchPass > 0, "No provider completed SEARCH.")
        assertTrue(streamPass > 0, "No provider produced a usable stream. See E2E diagnostics.")
    }

    private suspend fun runProvider(provider: AnimeProvider, query: String, timeoutMs: Long): StageResult {
        val result = StageResult(provider.name)
        val search = runStage(timeoutMs) { provider.search(query) }
        val animeResults = search.getOrNull()
        result.search = when {
            search.isFailure -> { result.details += "SEARCH ERROR: ${errorSummary(search.exceptionOrNull())}"; "ERROR" }
            animeResults.isNullOrEmpty() -> { result.details += "SEARCH EMPTY: no result for query '$query'"; "EMPTY" }
            else -> "PASS"
        }
        if (animeResults.isNullOrEmpty()) return result
        val selected = animeResults.first()
        val anime = runStage(timeoutMs) { provider.getAnime(selected.id) }
        val animeValue = anime.getOrNull()
        result.anime = when {
            anime.isFailure -> { result.details += "ANIME ERROR: ${errorSummary(anime.exceptionOrNull())}"; "ERROR" }
            animeValue == null -> { result.details += "ANIME EMPTY: selected result could not be resolved"; "EMPTY" }
            else -> "PASS"
        }
        if (animeValue == null) return result
        val episodes = runStage(timeoutMs) { provider.getEpisodes(selected.id) }
        val episodeList = episodes.getOrNull().orEmpty()
        result.episodes = when {
            episodes.isFailure -> { result.details += "EPISODES ERROR: ${errorSummary(episodes.exceptionOrNull())}"; "ERROR" }
            episodeList.isEmpty() -> { result.details += "EPISODES EMPTY: no episodes returned"; "EMPTY" }
            else -> "PASS"
        }
        if (episodeList.isEmpty()) return result
        val targetEpisode = when (provider.id) {
            "otakudesu" -> 1
            "samehadaku" -> 1086
            "kuramanime" -> 989
            "animedao" -> 1174
            else -> null
        }
        val episode = targetEpisode?.let { target -> episodeList.firstOrNull { it.number == target } }
            ?: episodeList.filter { it.number > 0 }.minByOrNull { it.number }
        if (episode == null) {
            result.stream = "SKIP"
            result.details += "STREAM SKIP: no positive episode number was returned"
            return result
        }
        val streams = runStage(timeoutMs) { provider.getStreams(selected.id, episode.number) }
        result.stream = when {
            streams.isFailure -> { result.details += "STREAM ERROR: ${errorSummary(streams.exceptionOrNull())}"; "ERROR" }
            streams.getOrNull().orEmpty().any { it.url.isNotBlank() && it.type != StreamType.UNKNOWN } -> "PASS"
            else -> { result.details += "STREAM EMPTY: no usable typed stream for episode ${episode.number}"; "EMPTY" }
        }
        return result
    }

    private suspend fun <T> runStage(timeoutMs: Long, block: suspend () -> T): Result<T> =
        runCatching { withTimeout(timeoutMs) { block() } }

    private fun errorSummary(error: Throwable?): String {
        if (error == null) return "unknown error"
        val message = error.message?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
        return if (message.isBlank()) error::class.simpleName.orEmpty()
        else "${error::class.simpleName}: ${message.take(180)}"
    }
}
