package com.kakaanime.provider

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.Assert.assertTrue

class ProviderRealSiteE2ETest {

    @Test
    fun realSiteProviderToStreamContract() = runBlocking {
        if (!System.getenv("ANILAB_REAL_E2E").equals("true", ignoreCase = true)) return@runBlocking

        val query = System.getenv("ANILAB_E2E_QUERY").orEmpty().ifBlank { "One Piece" }
        val timeoutMs = System.getenv("ANILAB_E2E_TIMEOUT_MS")?.toLongOrNull()?.coerceIn(10_000L, 180_000L) ?: 60_000L
        val engine = ProviderFactory.createEngine()

        val result = withTimeoutOrNull(timeoutMs) {
            val search = engine.search(query)
            require(search.isNotEmpty()) { "SEARCH_EMPTY query=$query" }
            println("E2E_SEARCH count=${search.size} first=${search.first().title} provider=${search.first().providerId}")

            val reports = mutableListOf<String>()
            var usableStreams = 0
            for (anime in search.take(8)) {
                val episodes = runCatching { engine.getEpisodes(anime.id) }.getOrDefault(emptyList())
                val episode = episodes.firstOrNull { it.number > 0 } ?: continue
                val streamResult = runCatching { engine.getEpisodeStreams(anime.id, episode.number) }
                val streams = streamResult.getOrNull()?.streams.orEmpty()
                if (streams.isNotEmpty()) usableStreams += streams.size
                reports += buildString {
                    append("provider=${anime.providerId} title=${anime.title} episode=${episode.number} streams=${streams.size}")
                    if (streamResult.isFailure) {
                        append(" error=${streamResult.exceptionOrNull()?.javaClass?.simpleName}:${streamResult.exceptionOrNull()?.message.orEmpty()}")
                    } else if (streams.isNotEmpty()) {
                        append(" urls=")
                        append(streams.take(3).joinToString(",") { it.url.take(160) })
                    }
                }
            }
            reports.forEach { println("E2E_REPORT $it") }
            assertTrue(usableStreams > 0, "NO_USABLE_STREAMS query=$query reports=${reports.joinToString(" || ")}")
        }
        assertTrue(result != null, "E2E_TIMEOUT query=$query timeoutMs=$timeoutMs")
    }
}