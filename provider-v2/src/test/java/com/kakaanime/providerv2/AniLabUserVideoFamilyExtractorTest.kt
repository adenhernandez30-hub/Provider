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

class AniLabUserVideoFamilyExtractorTest {
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
    fun extractsMediaFromUserVideoFamily() = runBlocking {
        val mediaUrl = server.url("/video/master.m3u8").toString()
        server.enqueue(MockResponse().setBody("""<video data-video="$mediaUrl"></video>"""))

        val sourceUrl = server.url("/embed/test").toString()
        val extractor = AniLabUserVideoFamilyExtractor(OkHttpClient.Builder().build())
        val candidates = extractor.extract(sourceUrl, AniLabExtractionContext())

        assertEquals(1, candidates.size)
        assertEquals(mediaUrl, candidates.single().url)
        assertEquals(AniLabStreamType.HLS, candidates.single().type)
        assertEquals(sourceUrl, candidates.single().referer)
    }

    @Test
    fun handlesAllSupportedHosts() {
        val extractor = AniLabUserVideoFamilyExtractor()
        assertTrue(extractor.canHandle("https://uservideo.net/embed/test"))
        assertTrue(extractor.canHandle("https://userdrive.co/embed/test"))
        assertTrue(extractor.canHandle("https://samevideo.to/embed/test"))
        assertTrue(!extractor.canHandle("https://example.com/embed/test"))
    }
}
