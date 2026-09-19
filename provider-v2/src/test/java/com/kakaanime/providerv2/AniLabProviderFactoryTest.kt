package com.kakaanime.providerv2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AniLabProviderFactoryTest {

    @Test
    fun otakudesu_registersProviderAndExtractorsInPipelineOrder() {
        val stack = AniLabProviderFactory.otakudesu(
            provider = OtakudesuProvider("https://otakudesu.blog"),
            episodeExtractor = OtakudesuEpisodeExtractor(acceptedHosts = setOf("otakudesu.blog")),
            embedExtractor = OtakudesuEmbedExtractor(),
        )

        assertEquals(listOf("otakudesu"), stack.providers.map { it.id })

        val extractors = stack.extractorRegistry.find(
            "https://otakudesu.blog/episode/test-episode-1/",
        )
        assertTrue(extractors.any { it.id == "otakudesu-episode" })
        assertTrue(extractors.none { it.id == "otakudesu-embed" })
    }

    @Test
    fun samehadaku_registersProviderAndEpisodeExtractor() {
        val stack = AniLabProviderFactory.samehadaku(
            provider = SamehadakuProvider("https://v2.samehadaku.how"),
            episodeExtractor = SamehadakuEpisodeExtractor(
                acceptedHosts = setOf("v2.samehadaku.how"),
            ),
        )

        assertEquals(listOf("samehadaku"), stack.providers.map { it.id })

        val extractors = stack.extractorRegistry.find(
            "https://v2.samehadaku.how/episode/test-episode-1/",
        )
        assertEquals(listOf("samehadaku-episode"), extractors.map { it.id })
    }
}
