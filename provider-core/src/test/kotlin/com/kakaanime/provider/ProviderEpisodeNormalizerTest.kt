package com.kakaanime.provider

import kotlin.test.Test
import kotlin.test.assertEquals

class ProviderEpisodeNormalizerTest {

    @Test
    fun dropsInvalidAndDeduplicatesEpisodes() {
        val episodes = listOf(
            ProviderEpisode("zero", "anime", 0),
            ProviderEpisode("two", "anime", 2),
            ProviderEpisode(
                id = "two-rich",
                animeId = "anime",
                number = 2,
                title = "Episode 2",
                thumbnailUrl = "https://img.example/2.jpg",
                availability = EpisodeAvailability.AVAILABLE,
            ),
            ProviderEpisode("one", "anime", 1),
        )

        val result = ProviderEpisodeNormalizer.normalize(episodes)

        assertEquals(listOf(1, 2), result.map { it.number })
        assertEquals("Episode 2", result[1].title)
        assertEquals("https://img.example/2.jpg", result[1].thumbnailUrl)
    }

    @Test
    fun seasonFilterKeepsUnspecifiedAndRequestedSeason() {
        val episodes = listOf(
            ProviderEpisode("s1", "anime", 1, seasonNumber = 1),
            ProviderEpisode("s2", "anime", 1, seasonNumber = 2),
            ProviderEpisode("unknown", "anime", 2),
        )

        val result = ProviderEpisodeNormalizer.normalize(episodes, seasonNumber = 1)

        assertEquals(listOf("s1", "unknown"), result.map { it.id })
    }
}
