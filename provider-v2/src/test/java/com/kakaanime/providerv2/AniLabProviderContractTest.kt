package com.kakaanime.providerv2

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.net.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AniLabProviderContractTest {

    @Test
    fun auto_usesFirstProviderWithCandidates() = runBlocking {
        val first = FakeProvider("first", emptyList())
        val second = FakeProvider("second", listOf(candidate("second", "server-b")))

        val result = AniLabRouter(listOf(first, second)).loadLinks(
            episodeUrl = "episode",
            mode = AniLabRoutingMode.AUTO,
        )

        val routed = when (result) {
            is AniLabRouteResult.Candidates -> result
            is AniLabRouteResult.Failure -> error("Expected candidates, got failure: ${result.primary.type} provider=${result.primary.providerId} message=${result.primary.message}")
        }
        check(routed.providerId == "second") { "providerId=${routed.providerId}" }
        check(routed.candidates.single().serverId == "server-b") { "serverId=${routed.candidates.single().serverId}" }
        check(routed.failures.single().type == AniLabFailureType.SERVER_EMPTY) { "failures=${routed.failures}" }
        check(listOf(first.lastEpisodeUrl, second.lastEpisodeUrl) == listOf("episode", "episode")) {
            "calls=${listOf(first.lastEpisodeUrl, second.lastEpisodeUrl)}"
        }
    }

    @Test
    fun manual_doesNotFallThroughToAnotherProvider() = runBlocking {
        val selected = FakeProvider("selected", emptyList())
        val other = FakeProvider("other", listOf(candidate("other", "server-a")))

        val result = AniLabRouter(listOf(selected, other)).loadLinks(
            episodeUrl = "episode",
            mode = AniLabRoutingMode.MANUAL,
            selectedProviderId = "selected",
        )

        val failure = assertIs<AniLabRouteResult.Failure>(result)
        assertEquals("selected", failure.primary.providerId)
        assertEquals(AniLabFailureType.SERVER_EMPTY, failure.primary.type)
        assertEquals("episode", selected.lastEpisodeUrl)
        assertEquals(null, other.lastEpisodeUrl)
    }

    @Test
    fun manual_unknownProviderFailsBeforeCallingProviders() = runBlocking {
        val provider = FakeProvider("known", listOf(candidate("known", "server-a")))

        val result = AniLabRouter(listOf(provider)).loadLinks(
            episodeUrl = "episode",
            mode = AniLabRoutingMode.MANUAL,
            selectedProviderId = "missing",
        )

        val failure = assertIs<AniLabRouteResult.Failure>(result)
        assertEquals(AniLabFailureType.PROVIDER_UNAVAILABLE, failure.primary.type)
        assertEquals("missing", failure.primary.providerId)
        assertEquals(null, provider.lastEpisodeUrl)
    }

    @Test
    fun providerException_isClassifiedAndAutoContinues() = runBlocking {
        val broken = FakeProvider("broken", failure = IllegalStateException("site down"))
        val healthy = FakeProvider("healthy", listOf(candidate("healthy", "server-a")))

        val result = AniLabRouter(listOf(broken, healthy)).loadLinks(
            episodeUrl = "episode",
            mode = AniLabRoutingMode.AUTO,
        )

        val routed = assertIs<AniLabRouteResult.Candidates>(result)
        assertEquals("healthy", routed.providerId)
        assertEquals(AniLabFailureType.PROVIDER_UNAVAILABLE, routed.failures.single().type)
        assertTrue(routed.failures.single().message.orEmpty().contains("site down"))
    }



    @Test
    fun router_socketTimeout_isClassifiedAsTimeout() = runBlocking {
        val broken = FakeProvider("broken", failure = SocketTimeoutException("provider timed out"))

        val result = AniLabRouter(listOf(broken)).loadLinks(
            episodeUrl = "episode",
            mode = AniLabRoutingMode.AUTO,
        )

        val failure = assertIs<AniLabRouteResult.Failure>(result)
        assertEquals(AniLabFailureType.TIMEOUT, failure.primary.type)
    }

    @Test
    fun router_rejectsDuplicateProviderIds() {
        val first = FakeProvider("same")
        val second = FakeProvider("same")

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            AniLabRouter(listOf(first, second))
        }
    }

    @Test
    fun router_rejectsBlankProviderId() {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            AniLabRouter(listOf(FakeProvider(" ")))
        }
    }

    @Test
    fun auto_skipsOpenProviderAndContinues() = runBlocking {
        val now = longArrayOf(0L)
        val breaker = AniLabCircuitBreaker(
            failureThreshold = 1,
            cooldownMillis = 1_000L,
            nowMillis = { now[0] },
        )
        breaker.recordFailure("broken")

        val broken = FakeProvider("broken", listOf(candidate("broken", "server-a")))
        val healthy = FakeProvider("healthy", listOf(candidate("healthy", "server-b")))

        val result = AniLabRouter(listOf(broken, healthy), breaker).loadLinks(
            episodeUrl = "episode",
            mode = AniLabRoutingMode.AUTO,
        )

        val routed = assertIs<AniLabRouteResult.Candidates>(result)
        assertEquals("healthy", routed.providerId)
        assertEquals(null, broken.lastEpisodeUrl)
        assertEquals("episode", healthy.lastEpisodeUrl)
        assertEquals(AniLabFailureType.PROVIDER_UNAVAILABLE, routed.failures.single().type)
    }

    @Test
    fun manual_openProviderDoesNotCallProvider() = runBlocking {
        val breaker = AniLabCircuitBreaker(failureThreshold = 1, cooldownMillis = 1_000L)
        breaker.recordFailure("selected")
        val selected = FakeProvider("selected", listOf(candidate("selected", "server-a")))

        val result = AniLabRouter(listOf(selected), breaker).loadLinks(
            episodeUrl = "episode",
            mode = AniLabRoutingMode.MANUAL,
            selectedProviderId = "selected",
        )

        val failure = assertIs<AniLabRouteResult.Failure>(result)
        assertEquals(AniLabFailureType.PROVIDER_UNAVAILABLE, failure.primary.type)
        assertEquals(null, selected.lastEpisodeUrl)
    }

    @Test
    fun router_rethrowsCancellation() = runBlocking {
        val provider = object : FakeProvider("cancel") {
            open override suspend fun loadLinks(episodeUrl: String): List<AniLabStreamCandidate> {
                throw kotlinx.coroutines.CancellationException("cancelled")
            }
        }

        var rethrown = false
        try {
            AniLabRouter(listOf(provider)).loadLinks(
                episodeUrl = "episode",
                mode = AniLabRoutingMode.AUTO,
            )
        } catch (t: kotlinx.coroutines.CancellationException) {
            rethrown = true
        }
        assertTrue(rethrown)
    }

    @Test
    fun auto_verifiedRouting_skipsProviderWhoseCandidateFailsVerification() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/vnd.apple.mpegurl")
            .setBody("#EXTM3U\n"))
        server.start()

        try {
            val badUrl = server.url("/bad.m3u8").toString()
            val goodUrl = server.url("/good.m3u8").toString()
            val broken = FakeProvider("broken", listOf(candidate("broken", "bad").copy(url = badUrl)))
            val healthy = FakeProvider("healthy", listOf(candidate("healthy", "good").copy(url = goodUrl)))
            val verified = AniLabVerifiedCandidatePipeline(
                AniLabCandidatePipeline(AniLabExtractorRegistry(listOf(UrlExtractor(badUrl, goodUrl)))),
                AniLabStreamVerifier(),
            )

            val result = AniLabRouter(listOf(broken, healthy)).loadVerifiedLinks(
                episodeUrl = "episode",
                mode = AniLabRoutingMode.AUTO,
                verifiedPipeline = verified,
            )

            val routed = assertIs<AniLabVerifiedRouteResult.Candidates>(result)
            assertEquals("healthy", routed.providerId)
            assertEquals("good", routed.candidates.single().serverId)
            assertTrue(routed.failures.any { it.type == AniLabFailureType.HTTP_BLOCKED })
            assertEquals("episode", broken.lastEpisodeUrl)
            assertEquals("episode", healthy.lastEpisodeUrl)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun manual_verifiedRouting_doesNotFallThroughWhenVerificationFails() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(403))
        server.start()

        try {
            val badUrl = server.url("/bad.m3u8").toString()
            val selected = FakeProvider("selected", listOf(candidate("selected", "bad").copy(url = badUrl)))
            val other = FakeProvider("other", listOf(candidate("other", "good")))
            val verified = AniLabVerifiedCandidatePipeline(
                AniLabCandidatePipeline(AniLabExtractorRegistry(listOf(UrlExtractor(badUrl)))),
                AniLabStreamVerifier(),
            )

            val result = AniLabRouter(listOf(selected, other)).loadVerifiedLinks(
                episodeUrl = "episode",
                mode = AniLabRoutingMode.MANUAL,
                selectedProviderId = "selected",
                verifiedPipeline = verified,
            )

            val failure = assertIs<AniLabVerifiedRouteResult.Failure>(result)
            assertEquals("selected", failure.failures.last().providerId)
            assertEquals(AniLabFailureType.HTTP_BLOCKED, failure.failures.last().type)
            assertEquals("episode", selected.lastEpisodeUrl)
            assertEquals(null, other.lastEpisodeUrl)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun auto_playbackFailure_movesToNextProvider() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "application/vnd.apple.mpegurl").setBody("#EXTM3U\\n"))
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "application/vnd.apple.mpegurl").setBody("#EXTM3U\\n"))
        server.start()
        try {
            val firstUrl = server.url("/first.m3u8").toString()
            val secondUrl = server.url("/second.m3u8").toString()
            val first = FakeProvider("first", listOf(candidate("first", "server-a").copy(url = firstUrl)))
            val second = FakeProvider("second", listOf(candidate("second", "server-b").copy(url = secondUrl)))
            val pipeline = AniLabVerifiedCandidatePipeline(
                AniLabCandidatePipeline(AniLabExtractorRegistry(listOf(UrlExtractor(firstUrl, secondUrl)))),
                AniLabStreamVerifier(),
            )
            val probe = AniLabPlaybackProbe { candidate ->
                if (candidate.providerId == "first") {
                    AniLabPlaybackProbeResult.Failed(
                        AniLabFailure("first", AniLabFailureType.PLAYBACK_FAILED, "first frame not reached"),
                    )
                } else AniLabPlaybackProbeResult.Playable
            }

            val result = AniLabRouter(listOf(first, second)).loadPlayableLinks(
                episodeUrl = "episode",
                mode = AniLabRoutingMode.AUTO,
                verifiedPipeline = pipeline,
                playbackProbe = probe,
            )

            val routed = assertIs<AniLabPlayableRouteResult.Candidates>(result)
            assertEquals("second", routed.providerId)
            assertEquals("server-b", routed.candidate.serverId)
            assertTrue(routed.failures.any { it.providerId == "first" && it.type == AniLabFailureType.PLAYBACK_FAILED })
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun playbackFailure_isAttributedToCurrentProvider() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/vnd.apple.mpegurl")
                .setBody("#EXTM3U\\n"),
        )
        server.start()
        try {
            val selectedUrl = server.url("/selected.m3u8").toString()
            val selected = FakeProvider(
                "selected",
                listOf(candidate("selected", "server-a").copy(url = selectedUrl)),
            )
            val pipeline = AniLabVerifiedCandidatePipeline(
                AniLabCandidatePipeline(AniLabExtractorRegistry(listOf(UrlExtractor(selectedUrl)))),
                AniLabStreamVerifier(),
            )
            val probe = AniLabPlaybackProbe {
                AniLabPlaybackProbeResult.Failed(
                    AniLabFailure("wrong-provider", AniLabFailureType.PLAYBACK_FAILED, "first frame not reached"),
                )
            }

            val result = AniLabRouter(listOf(selected)).loadPlayableLinks(
                episodeUrl = "episode",
                mode = AniLabRoutingMode.MANUAL,
                selectedProviderId = "selected",
                verifiedPipeline = pipeline,
                playbackProbe = probe,
            )

            val failure = assertIs<AniLabPlayableRouteResult.Failure>(result)
            assertTrue(failure.failures.any {
                it.providerId == "selected" && it.type == AniLabFailureType.PLAYBACK_FAILED
            })
            assertTrue(failure.failures.none {
                it.providerId == "wrong-provider" && it.type == AniLabFailureType.PLAYBACK_FAILED
            })
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun manual_playbackFailure_doesNotFallThroughToAnotherProvider() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "application/vnd.apple.mpegurl").setBody("#EXTM3U\\n"))
        server.start()
        try {
            val selectedUrl = server.url("/selected.m3u8").toString()
            val selected = FakeProvider("selected", listOf(candidate("selected", "server-a").copy(url = selectedUrl)))
            val other = FakeProvider("other", listOf(candidate("other", "server-b")))
            val pipeline = AniLabVerifiedCandidatePipeline(
                AniLabCandidatePipeline(AniLabExtractorRegistry(listOf(UrlExtractor(selectedUrl)))),
                AniLabStreamVerifier(),
            )
            val probe = AniLabPlaybackProbe {
                AniLabPlaybackProbeResult.Failed(
                    AniLabFailure("selected", AniLabFailureType.PLAYBACK_FAILED, "first frame not reached"),
                )
            }

            val result = AniLabRouter(listOf(selected, other)).loadPlayableLinks(
                episodeUrl = "episode",
                mode = AniLabRoutingMode.MANUAL,
                selectedProviderId = "selected",
                verifiedPipeline = pipeline,
                playbackProbe = probe,
            )

            val failure = assertIs<AniLabPlayableRouteResult.Failure>(result)
            assertTrue(failure.failures.any { it.providerId == "selected" && it.type == AniLabFailureType.PLAYBACK_FAILED })
            assertEquals("episode", selected.lastEpisodeUrl)
            assertEquals(null, other.lastEpisodeUrl)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun verifiedFailure_opensCircuitInsteadOfCountingUnverifiedCandidateAsSuccess() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(403))
        server.start()

        try {
            val badUrl = server.url("/bad.m3u8").toString()
            val breaker = AniLabCircuitBreaker(failureThreshold = 1, cooldownMillis = 60_000L)
            val provider = FakeProvider("broken", listOf(candidate("broken", "bad").copy(url = badUrl)))
            val verified = AniLabVerifiedCandidatePipeline(
                AniLabCandidatePipeline(AniLabExtractorRegistry(listOf(UrlExtractor(badUrl)))),
                AniLabStreamVerifier(),
            )
            val router = AniLabRouter(listOf(provider), breaker)

            val first = router.loadVerifiedLinks("episode", AniLabRoutingMode.AUTO, verified)
            assertIs<AniLabVerifiedRouteResult.Failure>(first)
            assertEquals(1, provider.callCount)

            val second = router.loadVerifiedLinks("episode", AniLabRoutingMode.AUTO, verified)
            assertIs<AniLabVerifiedRouteResult.Failure>(second)
            assertEquals(1, provider.callCount)
            assertEquals(AniLabFailureType.PROVIDER_UNAVAILABLE, (second as AniLabVerifiedRouteResult.Failure).failures.last().type)
        } finally {
            server.shutdown()
        }
    }

    private fun candidate(providerId: String, serverId: String) = AniLabStreamCandidate(
        providerId = providerId,
        serverId = serverId,
        url = "https://example.test/$serverId.m3u8",
        type = AniLabStreamType.HLS,
    )

    private open class FakeProvider(
        override val id: String,
        private val candidates: List<AniLabStreamCandidate> = emptyList(),
        private val failure: Throwable? = null,
    ) : AniLabProvider {
        var lastEpisodeUrl: String? = null
            private set
        var callCount: Int = 0
            private set

        override suspend fun search(query: String): List<AniLabSearchResult> = emptyList()

        override suspend fun load(url: String): AniLabAnime? = null

        override suspend fun loadLinks(episodeUrl: String): List<AniLabStreamCandidate> {
            callCount++
            lastEpisodeUrl = episodeUrl
            failure?.let { throw it }
            return candidates
        }
    }
    private class UrlExtractor(private vararg val urls: String) : AniLabExtractor {
        override val id = "url"
        override fun canHandle(url: String) = url in urls
        override suspend fun extract(url: String, context: AniLabExtractionContext) =
            listOf(AniLabStreamCandidate("", "", url, AniLabStreamType.HLS))
    }

}
