package com.kakaanime.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StreamNormalizerTest {

    @Test
    fun normalizesUrlTypeAndQualityMetadata() {
        val streams = StreamNormalizer.normalize(
            listOf(
                ProviderStream(
                    providerId = "p1",
                    url = " //cdn.example/video.m3u8?token=1 ",
                    quality = "Full HD",
                    type = StreamType.UNKNOWN,
                    headers = mapOf("Referer" to "https://episode.example/1")
                ),
                ProviderStream(
                    providerId = "p1",
                    url = "javascript:alert(1)",
                    quality = "720p",
                    type = StreamType.UNKNOWN
                )
            )
        )

        assertEquals(1, streams.size)
        assertEquals("https://cdn.example/video.m3u8?token=1", streams.first().url)
        assertEquals(StreamType.HLS, streams.first().type)
        assertEquals(StreamQuality.Q1080, streams.first().quality)
        assertEquals("https://episode.example/1", streams.first().headers["Referer"])
    }

    @Test
    fun deduplicatesEquivalentNormalizedUrls() {
        val streams = StreamNormalizer.normalize(
            listOf(
                ProviderStream(providerId = "p1", url = "https://cdn.example/video.mp4", quality = "480p"),
                ProviderStream(providerId = "p1", url = "  https://cdn.example/video.mp4  ", quality = "480p")
            )
        )

        assertEquals(1, streams.size)
        assertEquals(StreamType.MP4, streams.first().type)
        assertTrue(streams.first().url.startsWith("https://"))
    }
}
