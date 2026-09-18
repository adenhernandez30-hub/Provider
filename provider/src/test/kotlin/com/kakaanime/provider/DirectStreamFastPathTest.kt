package com.kakaanime.provider

import com.kakaanime.provider.extractor.DirectStreamFastPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectStreamFastPathTest {

    @Test
    fun recognizesCommonDirectMediaUrls() {
        assertTrue(DirectStreamFastPath.isDirectMediaUrl("https://cdn.example/video.m3u8"))
        assertTrue(DirectStreamFastPath.isDirectMediaUrl("https://cdn.example/video.mp4?token=abc"))
        assertTrue(DirectStreamFastPath.isDirectMediaUrl("https://cdn.example/video.mpd"))
    }

    @Test
    fun ignoresPageAndEmbedUrls() {
        assertFalse(DirectStreamFastPath.isDirectMediaUrl("https://example.com/watch/episode-1"))
        assertFalse(DirectStreamFastPath.isDirectMediaUrl("https://example.com/embed/player?id=1"))
    }

    @Test
    fun returnsOnlyDirectCandidatesInStableOrder() {
        assertEquals(
            listOf("https://cdn.example/a.m3u8", "https://cdn.example/b.mp4"),
            DirectStreamFastPath.candidates(
                listOf(
                    " https://cdn.example/a.m3u8 ",
                    "https://example.com/watch",
                    "https://cdn.example/a.m3u8",
                    "https://cdn.example/b.mp4",
                )
            )
        )
    }
}
