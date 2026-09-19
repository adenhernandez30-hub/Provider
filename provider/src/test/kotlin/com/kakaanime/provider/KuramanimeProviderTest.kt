package com.kakaanime.provider

import org.jsoup.Jsoup
import kotlin.test.Test
import kotlin.test.assertEquals

class KuramanimeProviderTest {

    private val provider = KuramanimeProvider()

    @Test
    fun buildAuthPathNormalizesMissingSlashes() {
        val authPath = provider.buildAuthPath(
            mapOf(
                "MIX_PREFIX_AUTH_ROUTE_PARAM" to "/api/",
                "MIX_AUTH_ROUTE_PARAM" to "/auth/token/"
            )
        )

        assertEquals("api/auth/token", authPath)
    }

    @Test
    fun extractEpisodeCandidatesCollectsIframeAndVideoSources() {
        val html = """
            <html>
              <body>
                <div class="video-content">
                  <iframe src="/embed/player"></iframe>
                </div>
                <video id="player">
                  <source data-src="https://cdn.example/video-1086.m3u8" />
                </video>
              </body>
            </html>
        """.trimIndent()
        val doc = Jsoup.parse(html, "https://v20.kuramanime.ing/anime/one-piece/episode/1086")

        val candidates = provider.extractEpisodeCandidates(doc)

        assertEquals(
            listOf(
                "https://v20.kuramanime.ing/embed/player",
                "https://cdn.example/video-1086.m3u8"
            ),
            candidates
        )
    }
}
