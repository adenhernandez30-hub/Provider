package com.kakaanime.provider

import kotlin.test.Test
import kotlin.test.assertEquals

class AnimeDaoProviderTest {

    @Test
    fun normalizeCandidateUrlSupportsProtocolRelativeAndEscapedValues() {
        val provider = AnimeDaoProvider()

        assertEquals(
            "https://cdn.example/video.m3u8",
            provider.normalizeCandidateUrl("https://animedao.in/episodes/one-piece-1x1174/", "//cdn.example/video.m3u8")
        )
        assertEquals(
            "https://cdn.example/video.mp4",
            provider.normalizeCandidateUrl("https://animedao.in/episodes/one-piece-1x1174/", "https:\\/\\/cdn.example\\/video.mp4")
        )
        assertEquals(
            "https://animedao.in/embed/player?id=123",
            provider.normalizeCandidateUrl("https://animedao.in/episodes/one-piece-1x1174/", "/embed/player?id=123")
        )
    }
}
