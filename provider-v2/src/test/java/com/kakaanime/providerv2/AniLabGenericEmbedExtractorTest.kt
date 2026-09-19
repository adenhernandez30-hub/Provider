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

class AniLabGenericEmbedExtractorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() { server = MockWebServer(); server.start() }

    @After
    fun tearDown() { server.shutdown() }

    @Test
    fun extractMediaAndFollowEmbedHop() = runBlocking {
        val playerUrl = server.url("/player").toString()
        val mediaUrl = server.url("/video/master.m3u8").toString()
        server.enqueue(MockResponse().setBody("<html><iframe src=\"$playerUrl\"></iframe></html>"))
        server.enqueue(MockResponse().setBody("<video src=\"$mediaUrl\"></video>"))
        val extractor = AniLabGenericEmbedExtractor(OkHttpClient.Builder().build())
        val sourceUrl = "https://vidhide.com/embed/test"
        val candidates = extractor.extract(sourceUrl, AniLabExtractionContext(referer = sourceUrl))
        assertEquals(1, candidates.size)
        assertEquals(mediaUrl, candidates.single().url)
        assertEquals(AniLabStreamType.HLS, candidates.single().type)
        assertEquals(playerUrl, candidates.single().referer)
        assertEquals("/player", server.takeRequest().path)
        assertEquals("/player", server.takeRequest().path)
    }

    @Test
    fun handlesSupportedHostFamiliesOnly() {
        val extractor = AniLabGenericEmbedExtractor()
        assertTrue(extractor.canHandle("https://vidhide.com/embed/test"))
        assertTrue(extractor.canHandle("https://sub.streamwish.to/e/test"))
        assertTrue(extractor.canHandle("https://filelions.to/v/test"))
        assertTrue(!extractor.canHandle("https://example.com/embed/test"))
    }
}