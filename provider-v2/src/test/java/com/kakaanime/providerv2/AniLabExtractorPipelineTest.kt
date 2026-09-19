package com.kakaanime.providerv2

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AniLabExtractorPipelineTest {

    @Test
    fun registry_fallsBackToNextExtractor_andTagsExtractorId() = runBlocking {
        val first = FakeExtractor("first", canHandle = true, candidates = emptyList())
        val second = FakeExtractor("second", canHandle = true, candidates = listOf(candidate("raw")))

        val result = AniLabExtractorRegistry(listOf(first, second)).extract("https://example.test/video")

        val success = assertIs<AniLabExtractionResult.Candidates>(result)
        assertEquals("second", success.extractorId)
        assertEquals("second", success.candidates.single().extractorId)
        assertEquals(AniLabFailureType.EXTRACTOR_FAILED, success.failures.single().type)
        assertEquals(listOf("https://example.test/video"), first.calls)
        assertEquals(listOf("https://example.test/video"), second.calls)
    }

    @Test
    fun registry_noCompatibleExtractor_returnsFailure() = runBlocking {
        val extractor = FakeExtractor("ignored", canHandle = false)

        val result = AniLabExtractorRegistry(listOf(extractor)).extract("https://example.test/video")

        val failure = assertIs<AniLabExtractionResult.Failure>(result)
        assertEquals(AniLabFailureType.EXTRACTOR_FAILED, failure.failure.type)
        assertTrue(failure.failure.message.orEmpty().contains("No compatible extractor"))
        assertTrue(extractor.calls.isEmpty())
    }

    @Test
    fun candidatePipeline_preservesCandidateContextAndMetadata() = runBlocking {
        val extractor = CapturingExtractor()
        val pipeline = AniLabCandidatePipeline(AniLabExtractorRegistry(listOf(extractor)))
        val source = candidate("source").copy(
            serverId = "server-1",
            referer = "https://site.test/episode",
            headers = mapOf("X-Test" to "yes"),
            cookies = mapOf("sid" to "abc"),
            subtitles = listOf(AniLabSubtitle(url = "https://sub.test/a.vtt", language = "id")),
        )

        val result = pipeline.resolve("provider-1", listOf(source))

        val success = assertIs<AniLabPipelineResult.Candidates>(result)
        val resolved = success.candidates.single()
        assertEquals("provider-1", resolved.providerId)
        assertEquals("server-1", resolved.serverId)
        assertEquals("capturing", resolved.extractorId)
        assertEquals(source.subtitles, resolved.subtitles)
        assertEquals("https://site.test/episode", extractor.context.referer)
        assertEquals("yes", extractor.context.headers["X-Test"])
        assertEquals("abc", extractor.context.cookies["sid"])
    }

    @Test
    fun candidatePipeline_blankUrl_isInvalidMedia_withoutExtractorCall() = runBlocking {
        val extractor = CapturingExtractor()
        val pipeline = AniLabCandidatePipeline(AniLabExtractorRegistry(listOf(extractor)))

        val result = pipeline.resolve(
            "provider-1",
            listOf(candidate("source").copy(url = "   ")),
        )

        val failure = assertIs<AniLabPipelineResult.Failure>(result)
        assertEquals(AniLabFailureType.INVALID_MEDIA, failure.failures.single().type)
        assertTrue(extractor.calls.isEmpty())
    }

    private fun candidate(id: String) = AniLabStreamCandidate(
        providerId = "source-provider",
        serverId = id,
        url = "https://example.test/$id.m3u8",
        type = AniLabStreamType.HLS,
    )

    private class FakeExtractor(
        override val id: String,
        private val canHandle: Boolean,
        private val candidates: List<AniLabStreamCandidate> = emptyList(),
    ) : AniLabExtractor {
        val calls = mutableListOf<String>()

        override fun canHandle(url: String) = canHandle

        override suspend fun extract(url: String, context: AniLabExtractionContext): List<AniLabStreamCandidate> {
            calls += url
            return candidates
        }
    }

    private class CapturingExtractor : AniLabExtractor {
        override val id = "capturing"
        lateinit var context: AniLabExtractionContext
        val calls = mutableListOf<String>()

        override fun canHandle(url: String) = true

        override suspend fun extract(url: String, context: AniLabExtractionContext): List<AniLabStreamCandidate> {
            calls += url
            this.context = context
            return listOf(AniLabStreamCandidate(providerId = "source-provider", serverId = "extracted", url = "https://example.test/extracted.m3u8", type = AniLabStreamType.HLS))
        }
    }
}
