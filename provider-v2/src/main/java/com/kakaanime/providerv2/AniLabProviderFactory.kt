package com.kakaanime.providerv2

/**
 * Ready-to-wire Otakudesu provider stack for provider-v2.
 *
 * Keeps provider and extractor construction in one place so app integration
 * cannot accidentally omit the Otakudesu episode/embed extractors.
 */
data class AniLabProviderStack(
    val providers: List<AniLabProvider>,
    val extractorRegistry: AniLabExtractorRegistry,
    val candidatePipeline: AniLabCandidatePipeline,
    val verifiedPipeline: AniLabVerifiedCandidatePipeline,
)

object AniLabProviderFactory {
    fun otakudesu(
        provider: OtakudesuProvider = OtakudesuProvider(),
        episodeExtractor: OtakudesuEpisodeExtractor = OtakudesuEpisodeExtractor(),
        embedExtractor: OtakudesuEmbedExtractor = OtakudesuEmbedExtractor(),
        verifier: AniLabStreamVerifier = AniLabStreamVerifier(),
    ): AniLabProviderStack {
        val extractorRegistry = AniLabExtractorRegistry(
            listOf(
                episodeExtractor,
                embedExtractor,
            ),
        )
        val candidatePipeline = AniLabCandidatePipeline(extractorRegistry)
        val verifiedPipeline = AniLabVerifiedCandidatePipeline(
            candidatePipeline = candidatePipeline,
            verifier = verifier,
        )

        return AniLabProviderStack(
            providers = listOf(provider),
            extractorRegistry = extractorRegistry,
            candidatePipeline = candidatePipeline,
            verifiedPipeline = verifiedPipeline,
        )
    }
}
