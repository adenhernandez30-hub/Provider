package com.kakaanime.provider

import com.kakaanime.provider.extractor.extractors.SamehadakuEpisodeExtractor
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SamehadakuEpisodeExtractorTest {

    private val extractor = SamehadakuEpisodeExtractor()

    @Test
    fun handlesNumericEpisodeSlugOnMainDomain() {
        assertTrue(extractor.canHandle("https://samehadaku.li/one-piece-1086/"))
    }

    @Test
    fun rejectsAnimeDetailPath() {
        assertFalse(extractor.canHandle("https://samehadaku.li/anime/one-piece/"))
    }

    @Test
    fun rejectsOtherDomains() {
        assertFalse(extractor.canHandle("https://example.com/one-piece-1086/"))
    }

    @Test
    fun normalizeCandidateUrlFixesEscapedProtocol() {
        val normalized = extractor.normalizeCandidateUrl(
            baseUrl = "https://samehadaku.li/one-piece-1086/",
            value = "https:\\/\\/cdn.example\\/master.m3u8?token=abc"
        )

        assertTrue(normalized == "https://cdn.example/master.m3u8?token=abc")
    }
}
