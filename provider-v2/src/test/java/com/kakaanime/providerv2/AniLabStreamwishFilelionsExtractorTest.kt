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

class AniLabStreamwishFilelionsExtractorTest {
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
    fun extractsStreamwishMedia() = runBlocking {
        val mediaUrl = server.url("/video/master.m3u8").toString()
        server.enqueue(MockResponse().setBody("""<video src="$mediaUrl"></video>"""))

        val sourceUrl = server.url("/sw/embed").toString()
        val extractor = AniLabStreamwishFilelionsExtractor(OkHttpClient.Builder().build())
        val candidates = extractor.extract(sourceUrl, AniLabExtractionContext())

        assertEquals(1, candidates.size)
        assertEquals(mediaUrl, candidates.single().url)
        assertEquals(AniLabStreamType.HLS, candidates.single().type)
        assertEquals(sourceUrl, candidates.single().referer)
    }

    @Test
    fun extractsFilelionsMedia() = runBlocking {
        val mediaUrl = server.url("/video/file.mp4").toString()
        server.enqueue(MockResponse().setBody("""<source data-src="$mediaUrl">"""))

        val sourceUrl = server.url("/fl/embed").toString()
        val extractor = AniLabStreamwishFilelionsExtractor(OkHttpClient.Builder().build())
        val candidates = extractor.extract(sourceUrl, AniLabExtractionContext())

        assertEquals(1, candidates.size)
        assertEquals(mediaUrl, candidates.single().url)
        assertEquals(AniLabStreamType.MP4, candidates.single().type)
    }

    @Test
    fun handlesOnlySupportedFamilies() {
        val extractor = AniLabStreamwishFilelionsExtractor()
        assertTrue(extractor.canHandle("https://streamwish.to/e/test"))
        assertTrue(extractor.canHandle("https://sub.streamwish.to/e/test"))
        assertTrue(extractor.canHandle("https://filelions.to/v/test"))
        assertTrue(!extractor.canHandle("https://example.com/e/test"))
    }
}
