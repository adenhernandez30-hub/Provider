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

class AniLabBloggerExtractorTest {
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
    fun extractsMediaFromBloggerLikePage() = runBlocking {
        val mediaUrl = server.url("/video/master.m3u8").toString()
        server.enqueue(
            MockResponse().setBody(
                """<html><video src="$mediaUrl"></video></html>"""
            )
        )

        val sourceUrl = server.url("/player").toString()
        val extractor = AniLabBloggerExtractor(OkHttpClient.Builder().build())
        val candidates = extractor.extract(
            sourceUrl,
            AniLabExtractionContext(referer = "https://v2.samehadaku.how/episode/test"),
        )

        assertEquals(1, candidates.size)
        assertEquals(mediaUrl, candidates.single().url)
        assertEquals(AniLabStreamType.HLS, candidates.single().type)
        assertEquals(sourceUrl, candidates.single().referer)
        assertEquals("/player", server.takeRequest().path)
    }

    @Test
    fun onlyHandlesBloggerFamilies() {
        val extractor = AniLabBloggerExtractor()
        assertTrue(extractor.canHandle("https://example.blogspot.com/p/player.html"))
        assertTrue(extractor.canHandle("https://www.blogger.com/video"))
        assertTrue(!extractor.canHandle("https://example.com/embed/test"))
    }
}
