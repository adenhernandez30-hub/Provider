package com.kakaanime.providerv2

import kotlinx.coroutines.runBlocking
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

        val routed = assertIs<AniLabRouteResult.Candidates>(result)
        assertEquals("second", routed.providerId)
        assertEquals("server-b", routed.candidates.single().serverId)
        assertEquals(AniLabFailureType.SERVER_EMPTY, routed.failures.single().type)
        assertEquals(listOf("first", "second"), listOf(first.lastEpisodeUrl, second.lastEpisodeUrl))
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

    private fun candidate(providerId: String, serverId: String) = AniLabStreamCandidate(
        providerId = providerId,
        serverId = serverId,
        url = "https://example.test/$serverId.m3u8",
        type = AniLabStreamType.HLS,
    )

    private class FakeProvider(
        override val id: String,
        private val candidates: List<AniLabStreamCandidate> = emptyList(),
        private val failure: Throwable? = null,
    ) : AniLabProvider {
        var lastEpisodeUrl: String? = null
            private set

        override suspend fun search(query: String): List<AniLabSearchResult> = emptyList()

        override suspend fun load(url: String): AniLabAnime? = null

        override suspend fun loadLinks(episodeUrl: String): List<AniLabStreamCandidate> {
            lastEpisodeUrl = episodeUrl
            failure?.let { throw it }
            return candidates
        }
    }
}
