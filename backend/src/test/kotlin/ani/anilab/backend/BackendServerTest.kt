package ani.anilab.backend

import com.kakaanime.provider.AnimeProvider
import com.kakaanime.provider.ProviderAnime
import com.kakaanime.provider.ProviderEngine
import com.kakaanime.provider.ProviderEpisode
import com.kakaanime.provider.ProviderRegistry
import com.kakaanime.provider.ProviderStream
import com.kakaanime.provider.StreamType
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackendServerTest {

    @Test
    fun httpEndpointsExposeProviderDataAndErrors() {
        val port = freePort()
        val registry = ProviderRegistry().apply {
            register(FakeProvider())
        }
        val server = BackendServer(ProviderEngine(registry), port = port)

        server.start()
        try {
            val root = request(port, "/")
            assertEquals(200, root.status)
            assertTrue(root.body.contains("\"service\":\"AniLab Provider Backend\""))

            val providers = request(port, "/providers")
            assertEquals(200, providers.status)
            assertTrue(providers.body.contains("\"id\":\"fake\""))

            val search = request(port, "/search/test")
            assertEquals(200, search.status)
            assertTrue(search.body.contains("A \\\"quote\\\""))
            assertTrue(search.body.contains("\\n"))

            val anime = request(port, "/anime/fake-anime")
            assertEquals(200, anime.status)
            assertTrue(anime.body.contains("\"title\":\"Fake Anime\""))

            val episodes = request(port, "/anime/fake-anime/episodes")
            assertEquals(200, episodes.status)
            assertTrue(episodes.body.contains("\"number\":1"))

            val stream = request(port, "/anime/fake-anime/episode/1/streams")
            assertEquals(200, stream.status)
            assertTrue(stream.body.contains("\"providerId\":\"fake\""))
            assertTrue(stream.body.contains("\"type\":\"HLS\""))

            val health = request(port, "/health")
            assertEquals(200, health.status)
            assertTrue(health.body.contains("\"status\":\"HEALTHY\""))
            assertTrue(health.body.contains("\"successfulRequests\":4"))

            val invalidEpisode = request(port, "/anime/fake-anime/episode/nope/streams")
            assertEquals(400, invalidEpisode.status)
            assertTrue(invalidEpisode.body.contains("invalid_episode_number"))

            val missing = request(port, "/does-not-exist")
            assertEquals(404, missing.status)
            assertTrue(missing.body.contains("not_found"))

            val post = request(port, "/", "POST")
            assertEquals(405, post.status)
            assertTrue(post.body.contains("method_not_allowed"))
        } finally {
            server.stop()
        }
    }

    @Test
    fun providersAndHealthExposeSameRuntimeProviderSet() {
        val port = freePort()
        val alpha = FakeProvider(id = "alpha", priority = 20)
        val beta = FakeProvider(id = "beta", priority = 10)
        val registry = ProviderRegistry().apply {
            register(alpha)
            register(beta)
        }
        val server = BackendServer(ProviderEngine(registry), port = port)

        server.start()
        try {
            val providers = request(port, "/providers")
            assertEquals(200, providers.status)

            val health = request(port, "/health")
            assertEquals(200, health.status)

            assertEquals(
                extractProviderIdsFromProviders(providers.body),
                extractProviderIdsFromHealth(health.body),
            )
        } finally {
            server.stop()
        }
    }

    private fun freePort(): Int =
        ServerSocket(0).use { it.localPort }

    private fun request(port: Int, path: String, method: String = "GET"): Response {
        val connection = (URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5_000
            readTimeout = 5_000
        }

        return try {
            val status = connection.responseCode
            val body = (if (status >= 400) connection.errorStream else connection.inputStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            Response(status, body)
        } finally {
            connection.disconnect()
        }
    }

    private data class Response(
        val status: Int,
        val body: String,
    )

    private fun extractProviderIdsFromProviders(body: String): List<String> =
        Regex("\"id\":\"([^\"]+)\"").findAll(body).map { it.groupValues[1] }.toList()

    private fun extractProviderIdsFromHealth(body: String): List<String> =
        Regex("\"providerId\":\"([^\"]+)\"").findAll(body).map { it.groupValues[1] }.toList()

    private class FakeProvider(
        override val id: String = "fake",
        override val name: String = "Fake Provider",
        override val priority: Int = 1,
    ) : AnimeProvider {
        override suspend fun search(query: String): List<ProviderAnime> =
            if (query == "test") {
                listOf(
                    ProviderAnime(
                        id = "fake-anime",
                        title = "A \"quote\"\nline",
                        providerId = id,
                    )
                )
            } else {
                emptyList()
            }

        override suspend fun getAnime(animeId: String): ProviderAnime? =
            if (animeId == "fake-anime") {
                ProviderAnime(
                    id = animeId,
                    title = "Fake Anime",
                    providerId = id,
                    description = "Fake provider test",
                )
            } else {
                null
            }

        override suspend fun getEpisodes(animeId: String): List<ProviderEpisode> =
            if (animeId == "fake-anime") {
                listOf(
                    ProviderEpisode(
                        id = "fake-episode-1",
                        animeId = animeId,
                        number = 1,
                        providerId = id,
                        title = "Episode 1",
                    )
                )
            } else {
                emptyList()
            }

        override suspend fun getStreams(animeId: String, episodeNumber: Int): List<ProviderStream> =
            if (animeId == "fake-anime" && episodeNumber == 1) {
                listOf(
                    ProviderStream(
                        providerId = id,
                        url = "https://example.test/stream.m3u8",
                        quality = "1080p",
                        language = "id",
                        subtitleLanguage = "id",
                        type = StreamType.HLS,
                    )
                )
            } else {
                emptyList()
            }
    }
}
