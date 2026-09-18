package com.kakaanime.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimeMatchScorerTest {

    @Test
    fun exactTitleWithSeasonAndYearGetsStrongConfidence() {
        val identity = CanonicalAnimeIdentity(
            anilistId = 1,
            title = "Attack on Titan",
            year = 2013,
            seasonNumber = 1,
        )
        val candidate = ProviderAnime(
            id = "aot-1",
            title = "Attack on Titan",
            providerId = "provider",
            year = 2013,
            seasonNumber = 1,
        )

        assertEquals(1.0, AnimeMatchScorer.score(identity, candidate))
    }

    @Test
    fun alternativeTitleCanMatch() {
        val identity = CanonicalAnimeIdentity(
            anilistId = 2,
            title = "Shingeki no Kyojin",
            alternativeTitles = listOf("Attack on Titan"),
        )
        val candidate = ProviderAnime(
            id = "aot",
            title = "Attack on Titan",
            providerId = "provider",
        )

        assertEquals(1.0, AnimeMatchScorer.score(identity, candidate))
    }

    @Test
    fun unrelatedTitleHasLowConfidence() {
        val identity = CanonicalAnimeIdentity(3, "One Piece")
        val candidate = ProviderAnime("naruto", "Naruto", providerId = "provider")

        assertTrue(AnimeMatchScorer.score(identity, candidate) < 0.5)
    }
}
