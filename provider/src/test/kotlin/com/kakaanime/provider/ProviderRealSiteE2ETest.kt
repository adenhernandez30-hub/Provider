package com.kakaanime.provider

import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@EnabledIfEnvironmentVariable(named = "ANILAB_REAL_E2E", matches = "true")
class ProviderRealSiteE2ETest {
    private data class StageResult(
        val id: String,
        val name: String,
        var search: String = "SKIP",
        var detail: String = "SKIP",
        var episodes: String = "SKIP",
        var stream: String = "SKIP",
        val details: MutableList<String> = mutableListOf(),
    )

    @Test
    fun runAllProvidersInOneBatch() = runBlocking {
        val query = System.getenv("ANILAB_E2E_QUERY").orEmpty().ifBlank { "One Piece" }
        val timeoutMs = System.getenv("ANILAB_E2E_TIMEOUT_MS")?.toLongOrNull()?.coerceIn(5_000L, 120_000L) ?: 60_000L
        val requiredIds = System.getenv("ANILAB_E2E_REQUIRED_PROVIDERS").orEmpty()
            .ifBlank { "otakudesu,samehadaku,kuramanime,animedao,animeisme" }
            .split(',').map(String::trim).filter(String::isNotBlank).toSet()
        val providers = ProviderFactory.createRegistry().all()
        val results = providers.map { provider -> runProvider(provider, query, timeoutMs) }

        val report = buildReport(query, timeoutMs, requiredIds, results)
        File("build/reports/provider-e2e-matrix.md").apply { parentFile.mkdirs(); writeText(report) }
        println(report)

        assertTrue(results.isNotEmpty(), "Provider registry is empty.")
        val missingRequired = requiredIds - results.map { it.id }.toSet()
        assertTrue(missingRequired.isEmpty(), "Required providers are missing from registry: ${missingRequired.joinToString()}")
        val failedRequired = results.filter { it.id in requiredIds && it.stream != "PASS" }
        assertTrue(failedRequired.isEmpty(), "Required provider stream E2E failed: ${failedRequired.joinToString { "${it.id}=${it.stream}" }}. See build/reports/provider-e2e-matrix.md.")
    }

    private suspend fun runProvider(provider: AnimeProvider, query: String, timeoutMs: Long): StageResult {
        val result = StageResult(provider.id, provider.name)
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
        result.detail = when {
            anime.isFailure -> { result.details += "DETAIL ERROR: ${errorSummary(anime.exceptionOrNull())}"; "ERROR" }
            animeValue == null -> { result.details += "DETAIL EMPTY: selected result could not be resolved"; "EMPTY" }
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
            streams.getOrNull().orEmpty().any { it.url.startsWith("http", true) && it.type != StreamType.UNKNOWN } -> "PASS"
            else -> { result.details += "STREAM EMPTY: no usable typed stream for episode ${episode.number}"; "EMPTY" }
        }
        return result
    }

    private suspend fun <T> runStage(timeoutMs: Long, block: suspend () -> T): Result<T> =
        runCatching { withTimeout(timeoutMs) { block() } }

    private fun buildReport(query: String, timeoutMs: Long, requiredIds: Set<String>, results: List<StageResult>): String = buildString {
        appendLine("# AniLab Provider Real-Site E2E Matrix")
        appendLine()
        appendLine("- Query: $query")
        appendLine("- Timeout per stage: ${timeoutMs}ms")
        appendLine("- Required stream providers: ${requiredIds.joinToString()}")
        appendLine("- Provider count: ${results.size}")
        appendLine()
        appendLine("| Provider | SEARCH | DETAIL | EPISODES | STREAM |")
        appendLine("|---|---|---|---|---|")
        results.forEach { result -> appendLine("| ${result.id} | ${result.search} | ${result.detail} | ${result.episodes} | ${result.stream} |") }
        appendLine()
        results.filter { it.details.isNotEmpty() }.forEach { result ->
            appendLine("## ${result.name} (${result.id})")
            result.details.forEach { detail -> appendLine("- $detail") }
            appendLine()
        }
    }

    private fun errorSummary(error: Throwable?): String {
        if (error == null) return "unknown error"
        val message = error.message?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
        return if (message.isBlank()) error::class.simpleName.orEmpty()
        else "${error::class.simpleName}: ${message.take(180)}"
    }
}