package com.kakaanime.providerv2

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OtakudesuEmbedExtractorTest {

    @Test
    fun extractor_resolvesDirectMediaAndNestedEmbed() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                "<iframe src=\"http://host.test/embed\"></iframe>" +
                    "<video src=\"http://cdn.test/inline.m3u8?token=1\"></video>"
            ),
        )
        server.enqueue(
            MockResponse().setBody(
                "<script>file = \"http://cdn.test/nested.mp4?token=2\"</script>"
            ),
        )
        server.start()
        try {
            val extractor = OtakudesuEmbedExtractor(
                acceptedHosts = setOf(server.url("/").host),
                client = OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.SECONDS)
                    .readTimeout(2, TimeUnit.SECONDS)
                    .callTimeout(5, TimeUnit.SECONDS)
                    .build(),
            )

            val result = extractor.extract(
                server.url("/embed-root").toString(),
                AniLabExtractionContext(),
            )

            assertEquals(AniLabStreamType.HLS, result.first { it.url.contains("inline.m3u8") }.type)
            assertEquals(AniLabStreamType.MP4, result.first { it.url.contains("nested.mp4") }.type)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun extractor_readsFiledonDataPage() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                "<div id=\"app\" data-page=\"{&quot;props&quot;:{&quot;url&quot;:&quot;http://cdn.test/file.mp4?token=3&quot;}}\"></div>"
            ),
        )
        server.start()
        try {
            val extractor = OtakudesuEmbedExtractor(
                acceptedHosts = setOf(server.hostName),
            )

            val result = extractor.extract(
                server.url("/embed").toString(),
                AniLabExtractionContext(),
            )

            assertTrue(result.any { it.url.contains("file.mp4?token=3") })
        } finally {
            server.shutdown()
        }
    }
}
