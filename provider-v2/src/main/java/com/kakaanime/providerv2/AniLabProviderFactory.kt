package com.kakaanime.providerv2

/**
 * Ready-to-wire provider stacks for provider-v2.
 *
 * Keeps provider and extractor construction in one place so app integration
 * cannot accidentally omit the site's extractors.
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
    ): AniLabProviderStack = singleProvider(
        provider = provider,
        extractors = listOf(episodeExtractor, embedExtractor),
        verifier = verifier,
    )

    fun samehadaku(
        provider: SamehadakuProvider = SamehadakuProvider(),
        episodeExtractor: SamehadakuEpisodeExtractor = SamehadakuEpisodeExtractor(),
        verifier: AniLabStreamVerifier = AniLabStreamVerifier(),
    ): AniLabProviderStack = singleProvider(
        provider = provider,
        extractors = listOf(episodeExtractor),
        verifier = verifier,
    )

    private fun singleProvider(
        provider: AniLabProvider,
        extractors: List<AniLabExtractor>,
        verifier: AniLabStreamVerifier,
    ): AniLabProviderStack {
        val extractorRegistry = AniLabExtractorRegistry(extractors)
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
