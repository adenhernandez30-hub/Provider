package com.kakaanime.providerv2

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AniLabFileDonExtractorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun extractsDirectAndDataPageMedia() = runBlocking {
        val direct = server.url("/video/direct.mp4").toString()
        val dataPage = server.url("/video/page.m3u8").toString()

        server.enqueue(
            MockResponse().setBody(
                """<video src="$direct"></video>
                    <div data-page="{&quot;url&quot;:&quot;$dataPage&quot;}"></div>"""
            )
        )

        val sourceUrl = server.url("/d/test").toString()
        val extractor = AniLabFileDonExtractor(OkHttpClient.Builder().build())
        val candidates = extractor.extract(
            sourceUrl,
            AniLabExtractionContext(referer = "https://v2.samehadaku.how/episode/test"),
        )

        assertEquals(2, candidates.size)
        assertTrue(candidates.any { it.url == direct && it.type == AniLabStreamType.MP4 })
        assertTrue(candidates.any { it.url == dataPage && it.type == AniLabStreamType.HLS })
        assertEquals(sourceUrl, candidates.first().referer)
        assertEquals("/d/test", server.takeRequest().path)
    }

    @Test
    fun onlyHandlesFileDonFamilies() {
        val extractor = AniLabFileDonExtractor()
        assertTrue(extractor.canHandle("https://filedon.co/d/test"))
        assertTrue(extractor.canHandle("https://sub.filedon.to/d/test"))
        assertTrue(!extractor.canHandle("https://example.com/d/test"))
    }
}
