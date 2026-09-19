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
}
