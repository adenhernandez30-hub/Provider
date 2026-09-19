package com.kakaanime.provider

import kotlin.test.Test
import kotlin.test.assertEquals

class OtakudesuProviderTest {

    @Test
    fun selectPlaybackRefPrefersWebEpisodeUrlForNonHttpApiSlug() {
        val provider = OtakudesuProvider()
        val selected = provider.selectPlaybackRef(
            episodeRef = "one-piece-episode-1086",
            webEpisodeUrl = "https://otakudesu.blog/episode/one-piece-episode-1086/"
        )

        assertEquals("https://otakudesu.blog/episode/one-piece-episode-1086/", selected)
    }

    @Test
    fun selectPlaybackRefKeepsCommunityReference() {
        val provider = OtakudesuProvider()
        val selected = provider.selectPlaybackRef(
            episodeRef = "community:one-piece-episode-1086",
            webEpisodeUrl = "https://otakudesu.blog/episode/one-piece-episode-1086/"
        )

        assertEquals("community:one-piece-episode-1086", selected)
    }
}
