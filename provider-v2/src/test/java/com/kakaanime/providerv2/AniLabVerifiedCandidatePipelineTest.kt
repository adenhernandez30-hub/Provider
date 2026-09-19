package com.kakaanime.providerv2

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AniLabVerifiedCandidatePipelineTest {

    @Test
    fun validExtractedCandidate_isReturnedWithDetectedType() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/vnd.apple.mpegurl")
                .setBody("#EXTM3U\n#EXT-X-VERSION:3\n"),
        )
        server.start()

        try {
            val url = server.url("/stream.m3u8").toString()
            val extractor = StaticExtractor(url)
            val pipeline = AniLabVerifiedCandidatePipeline(
                AniLabCandidatePipeline(AniLabExtractorRegistry(listOf(extractor))),
                AniLabStreamVerifier(),
            )

            val result = pipeline.resolve(
                "provider-1",
                listOf(
                    AniLabStreamCandidate(
                        providerId = "provider-1",
                        serverId = "server-1",
                        url = url,
                        type = AniLabStreamType.HLS,
                    ),
                ),
            )

            val success = assertIs<AniLabVerifiedPipelineResult.Candidates>(result)
            assertEquals(1, success.candidates.size)
            assertEquals(AniLabStreamType.HLS, success.candidates.single().type)
            assertEquals("static", success.candidates.single().extractorId)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun blockedCandidate_isRejectedAndClassified() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(403))
        server.start()

        try {
            val url = server.url("/blocked.m3u8").toString()
            val pipeline = AniLabVerifiedCandidatePipeline(
                AniLabCandidatePipeline(AniLabExtractorRegistry(listOf(StaticExtractor(url)))),
                AniLabStreamVerifier(),
            )

            val result = pipeline.resolve(
                "provider-1",
                listOf(
                    AniLabStreamCandidate(
                        providerId = "provider-1",
                        serverId = "server-1",
                        url = url,
                        type = AniLabStreamType.HLS,
                    ),
                ),
            )

            val failure = assertIs<AniLabVerifiedPipelineResult.Failure>(result)
            assertEquals(AniLabFailureType.HTTP_BLOCKED, failure.failures.single().type)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun oneInvalidCandidate_doesNotPreventLaterValidCandidate() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/vnd.apple.mpegurl")
                .setBody("#EXTM3U\n"),
        )
        server.start()

        try {
            val badUrl = server.url("/bad.m3u8").toString()
            val goodUrl = server.url("/good.m3u8").toString()
            val extractor = SequenceExtractor(listOf(badUrl, goodUrl))
            val pipeline = AniLabVerifiedCandidatePipeline(
                AniLabCandidatePipeline(AniLabExtractorRegistry(listOf(extractor))),
                AniLabStreamVerifier(),
            )

            val result = pipeline.resolve(
                "provider-1",
                listOf(
                    AniLabStreamCandidate("provider-1", "bad", badUrl, AniLabStreamType.HLS),
                    AniLabStreamCandidate("provider-1", "good", goodUrl, AniLabStreamType.HLS),
                ),
            )

            val success = assertIs<AniLabVerifiedPipelineResult.Candidates>(result)
            assertEquals("good", success.candidates.single().serverId)
            assertTrue(success.failures.any { it.type == AniLabFailureType.HTTP_BLOCKED })
        } finally {
            server.shutdown()
        }
    }

    private class StaticExtractor(private val url: String) : AniLabExtractor {
        override val id = "static"
        override fun canHandle(url: String) = url == this.url
        override suspend fun extract(url: String, context: AniLabExtractionContext) =
            listOf(AniLabStreamCandidate("", "", url, AniLabStreamType.HLS))
    }

    private class SequenceExtractor(private val urls: List<String>) : AniLabExtractor {
        override val id = "sequence"
        override fun canHandle(url: String) = url in urls
        override suspend fun extract(url: String, context: AniLabExtractionContext) =
            listOf(AniLabStreamCandidate("", "", url, AniLabStreamType.HLS))
    }
}
