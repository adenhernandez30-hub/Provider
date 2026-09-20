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

class AniLabLegacyHostFamilyExtractorTest {
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
    fun extractsMp4UploadStyleMedia() = runBlocking {
        val mediaUrl = server.url("/video/file.mp4").toString()
        server.enqueue(MockResponse().setBody("""<video src="$mediaUrl"></video>"""))

        val sourceUrl = server.url("/embed/test").toString()
        val extractor = AniLabLegacyHostFamilyExtractor(OkHttpClient.Builder().build())
        val candidates = extractor.extract(sourceUrl, AniLabExtractionContext())

        assertEquals(1, candidates.size)
        assertEquals(mediaUrl, candidates.single().url)
        assertEquals(AniLabStreamType.MP4, candidates.single().type)
    }

    @Test
    fun handlesAllLegacyHostFamilies() {
        val extractor = AniLabLegacyHostFamilyExtractor()
        assertTrue(extractor.canHandle("https://mp4upload.com/embed/test"))
        assertTrue(extractor.canHandle("https://yourupload.com/embed/test"))
        assertTrue(extractor.canHandle("https://yuplod.net/embed/test"))
        assertTrue(extractor.canHandle("https://desustream.info/embed/test"))
        assertTrue(extractor.canHandle("https://desudrive.com/embed/test"))
        assertTrue(extractor.canHandle("https://odstream.xyz/embed/test"))
        assertTrue(extractor.canHandle("https://odcdn.com/embed/test"))
        assertTrue(extractor.canHandle("https://otakuwatch.com/embed/test"))
        assertTrue(!extractor.canHandle("https://example.com/embed/test"))
    }
}
