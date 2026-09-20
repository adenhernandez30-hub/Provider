package com.kakaanime.providerv2

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AniLabVerifiedPipelineIntegrationTest {
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
    fun extractedCandidatePassesRealHttpVerifier() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/vnd.apple.mpegurl")
                .setBody("#EXTM3U
#EXT-X-VERSION:3
#EXT-X-TARGETDURATION:6
#EXTINF:6,
segment.ts
"),
        )

        val mediaUrl = server.url("/master.m3u8").toString()
        val sourceUrl = "https://vidhide.com/embed/test"

        val extractor = object : AniLabExtractor {
            override val id = "test-source"

            override fun canHandle(url: String) = url == sourceUrl

            override suspend fun extract(
                url: String,
                context: AniLabExtractionContext,
            ) = listOf(
                AniLabStreamCandidate(
                    providerId = "samehadaku",
                    serverId = "test-source",
                    url = mediaUrl,
                    type = AniLabStreamType.HLS,
                    referer = context.referer ?: sourceUrl,
                ),
            )
        }

        val registry = AniLabExtractorRegistry(listOf(extractor))
        val candidatePipeline = AniLabCandidatePipeline(registry)
        val verifiedPipeline = AniLabVerifiedCandidatePipeline(
            candidatePipeline = candidatePipeline,
            verifier = AniLabStreamVerifier(),
        )

        val result = verifiedPipeline.resolve(
            providerId = "samehadaku",
            candidates = listOf(
                AniLabStreamCandidate(
                    providerId = "samehadaku",
                    serverId = "embed",
                    url = sourceUrl,
                ),
            ),
        )

        assertTrue(result is AniLabVerifiedPipelineResult.Candidates)
        val verified = result as AniLabVerifiedPipelineResult.Candidates
        assertEquals(1, verified.candidates.size)
        assertEquals(mediaUrl, verified.candidates.single().url)
        assertEquals(AniLabStreamType.HLS, verified.candidates.single().type)
    }
}
