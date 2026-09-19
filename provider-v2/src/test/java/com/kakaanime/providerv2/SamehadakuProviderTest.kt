package com.kakaanime.providerv2

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SamehadakuProviderTest {
    @Test
    fun search_filtersNonAnimeLinks() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(
            "<a href=\"/anime/naruto/\">Naruto</a>" +
                "<a href=\"/batch/naruto/\">Naruto Batch</a>" +
                "<a href=\"/anime/one-piece/\">One Piece</a>",
        ))
        server.start()
        try {
            val provider = SamehadakuProvider(server.url("/").toString().trimEnd('/'))
            val results = provider.search("naruto")
            assertEquals(listOf("Naruto", "One Piece"), results.map { it.title })
            assertTrue(results.all { it.providerId == "samehadaku" })
        } finally { server.shutdown() }
    }

    @Test
    fun load_parsesEpisodeLinks() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(
            "<h1>Naruto</h1>" +
                "<a href=\"/episode/naruto-episode-2/\">Episode 2</a>" +
                "<a href=\"/anime/naruto/\">Anime</a>" +
                "<a href=\"/episode/naruto-episode-1/\">Episode 1</a>",
        ))
        server.start()
        try {
            val provider = SamehadakuProvider(server.url("/").toString().trimEnd('/'))
            val anime = provider.load(server.url("/anime/naruto/").toString())
            assertEquals("Naruto", anime?.title)
            assertEquals(listOf(1, 2), anime?.episodes?.map { it.number })
        } finally { server.shutdown() }
    }

    @Test
    fun loadLinks_returnsEpisodePageForExtractorPipeline() = runBlocking {
        val provider = SamehadakuProvider("https://v2.samehadaku.how")
        val result = provider.loadLinks("https://v2.samehadaku.how/episode/naruto-episode-1/")
        assertEquals(1, result.size)
        assertEquals("samehadaku-episode-page", result.single().serverId)
        assertEquals(AniLabStreamType.UNKNOWN, result.single().type)
    }
}

class SamehadakuEpisodeExtractorTest {
    @Test
    fun extractor_collectsDirectMediaAndNestedPlayerMedia() = runBlocking {
        val server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setBody(
            "<video src=\"http://${server.hostName}/media/episode.m3u8\"></video>" +
                "<iframe src=\"${server.url("/player/")}\"></iframe>",
        ))
        server.enqueue(MockResponse().setBody(
            "<video src=\"http://${server.hostName}/media/fallback.mp4\"></video>",
        ))
        try {
            val extractor = SamehadakuEpisodeExtractor(
                acceptedHosts = setOf(server.hostName),
                client = OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.SECONDS)
                    .readTimeout(2, TimeUnit.SECONDS)
                    .callTimeout(5, TimeUnit.SECONDS)
                    .build(),
            )
            val result = extractor.extract(
                server.url("/episode/naruto-episode-1/").toString(),
                AniLabExtractionContext(),
            )
            assertTrue(result.any { it.url.endsWith("/media/episode.m3u8") })
            assertTrue(result.any { it.url.endsWith("/media/fallback.mp4") })
        } finally { server.shutdown() }
    }

    @Test
    fun extractor_resolvesEastPlayAjaxPlayerOption() = runBlocking {
        val server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setBody(
            "<div class=\"east_player_option\" data-post=\"123\" data-nume=\"abc\" data-type=\"schtml\" data-name=\"VIP\"></div>",
        ))
        server.enqueue(MockResponse().setBody(
            "<iframe src=\"${server.url("/player/")}\"></iframe>",
        ))
        server.enqueue(MockResponse().setBody(
            "<video src=\"${server.url("/media/eastplay.m3u8")}\"></video>",
        ))
        try {
            val ajaxUrl = server.url("/wp-admin/admin-ajax.php").toString()
            val extractor = SamehadakuEpisodeExtractor(
                acceptedHosts = setOf(server.hostName),
                ajaxUrl = ajaxUrl,
                client = OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.SECONDS)
                    .readTimeout(2, TimeUnit.SECONDS)
                    .callTimeout(5, TimeUnit.SECONDS)
                    .build(),
            )
            val episodeUrl = server.url("/episode/naruto-episode-1/").toString()
            val result = extractor.extract(episodeUrl, AniLabExtractionContext())
            assertTrue(result.any { it.url.endsWith("/media/eastplay.m3u8") })

            val ajaxRequest = server.takeRequest(2, TimeUnit.SECONDS)
            assertEquals("POST", ajaxRequest?.method)
            val ajaxBody = ajaxRequest?.body?.readUtf8().orEmpty()
            assertTrue(ajaxBody.contains("action=player_ajax"))
            assertTrue(ajaxBody.contains("post=123"))
            assertTrue(ajaxBody.contains("nume=abc"))
            assertTrue(ajaxBody.contains("type=schtml"))
        } finally { server.shutdown() }
    }

    @Test
    fun extractor_rejectsNonEpisodeUrl() {
        val extractor = SamehadakuEpisodeExtractor(acceptedHosts = setOf("example.test"))
        assertEquals(false, extractor.canHandle("https://example.test/anime/naruto/"))
        assertEquals(true, extractor.canHandle("https://example.test/episode/naruto-episode-1/"))
    }
}
