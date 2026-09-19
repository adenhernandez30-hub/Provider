package com.kakaanime.providerv2

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OtakudesuProviderTest {

    @Test
    fun search_filtersNonAnimeLinks() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(
            "<a href=\"/anime/naruto/\">Naruto</a>" +
                "<a href=\"/batch/naruto/\">Naruto Batch</a>" +
                "<a href=\"/series/one-piece/\">One Piece</a>"
        ))
        server.start()
        try {
            val provider = OtakudesuProvider(server.url("/").toString().trimEnd('/'))
            val results = provider.search("naruto")
            assertEquals(listOf("Naruto", "One Piece"), results.map { it.title })
            assertTrue(results.all { it.providerId == "otakudesu" })
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun load_parsesOnlyEpisodeLinks() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(
            "<h1>Naruto</h1>" +
                "<a href=\"/episode/naruto-episode-2-sub-indo/\">Episode 2</a>" +
                "<a href=\"/batch/naruto-batch/\">Batch</a>" +
                "<a href=\"/episode/naruto-episode-1-sub-indo/\">Episode 1</a>"
        ))
        server.start()
        try {
            val provider = OtakudesuProvider(server.url("/").toString().trimEnd('/'))
            val anime = provider.load(server.url("/anime/naruto/").toString())
            assertEquals("Naruto", anime?.title)
            assertEquals(listOf(1, 2), anime?.episodes?.map { it.number })
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun loadLinks_returnsEpisodePageForExtractorPipeline() = runBlocking {
        val provider = OtakudesuProvider("https://otakudesu.io")
        val result = provider.loadLinks("https://otakudesu.io/episode/naruto-episode-1/")
        assertEquals(1, result.size)
        assertEquals("otakudesu-episode-page", result.single().serverId)
        assertEquals(AniLabStreamType.UNKNOWN, result.single().type)
    }
}

class OtakudesuEpisodeExtractorTest {

    @Test
    fun extractor_parsesDirectMediaAndMirrorAjax() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(
            "<script>" +
                "window.__x__nonce = true;" +
                "data:{action:\"aabbccdd\"};" +
                "nonce: \"x\", action: \"00112233445566778899aabbccddeeff\";" +
                "</script>" +
                "<video src=\"http://example.test/direct.m3u8?token=abc\"></video>" +
                "<div data-content=\"eyJpZCI6IjEyMyIsImkiOiJzZXJ2ZXItYSIsInEiOiI3MjBwIn0=\"></div>"
        ))
        server.enqueue(MockResponse().setBody(
            "{\"data\":\"MDAxMTIyMzM0NDU1NjY3Nzg4OTlhYWJiY2NkZGVlZmY=\"}"
        ))
        server.enqueue(MockResponse().setBody(
            "{\"data\":\"PGlmcmFtZSBzcmM9J2h0dHA6Ly9leGFtcGxlLnRlc3QvZW1iZWQnPg==\"}"
        ))
        server.start()
        try {
            val extractor = OtakudesuEpisodeExtractor(
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

            assertTrue(result.any { it.url.contains("/direct.m3u8?token=abc") })
            assertTrue(result.any { it.url.contains("/embed") })
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun extractor_rejectsNonEpisodeUrl() {
        val extractor = OtakudesuEpisodeExtractor(acceptedHosts = setOf("example.test"))
        assertEquals(false, extractor.canHandle("https://example.test/anime/naruto/"))
        assertEquals(true, extractor.canHandle("https://example.test/episode/naruto-episode-1/"))
    }
}
