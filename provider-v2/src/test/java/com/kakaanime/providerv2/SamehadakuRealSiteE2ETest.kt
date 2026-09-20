package com.kakaanime.providerv2

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Opt-in real-site smoke test.
 *
 * Run with:
 *   SAMEHADAKU_E2E_EPISODE_URL="https://..." \
 *   ./gradlew :provider-v2:testDebugUnitTest --tests '*SamehadakuRealSiteE2ETest'
 */
class SamehadakuRealSiteE2ETest {
    @Test
    fun search_detail_episode_extract_and_verify_real_site() = runBlocking {
        val episodeUrl = System.getenv("SAMEHADAKU_E2E_EPISODE_URL")
            ?.trim()
            .orEmpty()

        check(episodeUrl.isNotBlank()) {
            "SAMEHADAKU_E2E_EPISODE_URL must be set for the real-site E2E test"
        }

        val stack = AniLabProviderFactory.samehadaku()
        val provider = stack.providers.single { it.id == "samehadaku" }

        val search = provider.search("One Piece")
        assertTrue(search.isNotEmpty(), "Samehadaku search returned no results")

        val detail = provider.load(
            search.firstOrNull { it.url.contains("/anime/", ignoreCase = true) }?.url
                ?: error("Samehadaku search returned no anime detail URL"),
        )
        assertTrue(detail != null, "Samehadaku detail could not be loaded")
        assertTrue(detail!!.episodes.isNotEmpty(), "Samehadaku detail returned no episodes")

        val episode = provider.loadLinks(episodeUrl)
        assertFalse(episode.isEmpty(), "Samehadaku loadLinks returned no episode-page candidate")

        val extracted = stack.candidatePipeline.resolve(
            providerId = provider.id,
            candidates = episode,
            context = AniLabExtractionContext(referer = episodeUrl),
        )
        assertTrue(
            extracted is AniLabPipelineResult.Candidates &&
                extracted.candidates.isNotEmpty(),
            "Samehadaku real-site extraction produced no stream candidates: $extracted",
        )

        val verified = stack.verifiedPipeline.resolve(
            providerId = provider.id,
            candidates = episode,
            context = AniLabExtractionContext(referer = episodeUrl),
        )
        assertTrue(
            verified is AniLabVerifiedPipelineResult.Candidates &&
                verified.candidates.isNotEmpty(),
            "Samehadaku real-site verification produced no playable media candidates: $verified",
        )
    }
}
