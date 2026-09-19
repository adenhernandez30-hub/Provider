package com.kakaanime.provider

import com.kakaanime.provider.extractor.extractors.OtakudesuServerExtractor
import kotlin.test.Test
import kotlin.test.assertEquals

class OtakudesuServerExtractorTest {

    @Test
    fun limitedPlaybackCandidatesCapsAndKeepsHttpCandidates() {
        val extractor = OtakudesuServerExtractor()
        val candidates = listOf(
            OtakudesuServerExtractor.PlaybackCandidate("https://a.example/1", "360"),
            OtakudesuServerExtractor.PlaybackCandidate("https://a.example/1", "360"),
            OtakudesuServerExtractor.PlaybackCandidate("javascript:void(0)", "360"),
            OtakudesuServerExtractor.PlaybackCandidate("https://b.example/2", "480"),
            OtakudesuServerExtractor.PlaybackCandidate("https://c.example/3", "720"),
            OtakudesuServerExtractor.PlaybackCandidate("https://d.example/4", "1080"),
            OtakudesuServerExtractor.PlaybackCandidate("https://e.example/5", "1080"),
            OtakudesuServerExtractor.PlaybackCandidate("https://f.example/6", "1080"),
        )

        val limited = extractor.limitedPlaybackCandidates(candidates)

        assertEquals(5, limited.size)
        assertEquals(
            listOf(
                "https://a.example/1",
                "https://b.example/2",
                "https://c.example/3",
                "https://d.example/4",
                "https://e.example/5"
            ),
            limited.map { it.url }
        )
    }
}
