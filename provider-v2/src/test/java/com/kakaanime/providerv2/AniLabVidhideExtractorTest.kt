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

class AniLabVidhideExtractorTest {
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
    fun extractsDirectMedia() = runBlocking {
        val mediaUrl = server.url("/video/master.m3u8").toString()
        server.enqueue(MockResponse().setBody("""<video data-src="$mediaUrl"></video>"""))

        val sourceUrl = server.url("/embed/test").toString()
        val extractor = AniLabVidhideExtractor(OkHttpClient.Builder().build())
        val candidates = extractor.extract(
            sourceUrl,
            AniLabExtractionContext(referer = "https://v2.samehadaku.how/episode/test"),
        )

        assertEquals(1, candidates.size)
        assertEquals(mediaUrl, candidates.single().url)
        assertEquals(AniLabStreamType.HLS, candidates.single().type)
        assertEquals(sourceUrl, candidates.single().referer)
        assertEquals("/embed/test", server.takeRequest().path)
    }

    @Test
    fun onlyHandlesVidhideFamilies() {
        val extractor = AniLabVidhideExtractor()
        assertTrue(extractor.canHandle("https://vidhide.com/embed/test"))
        assertTrue(extractor.canHandle("https://sub.vidhide.com/embed/test"))
        assertTrue(!extractor.canHandle("https://example.com/embed/test"))
    }
}
