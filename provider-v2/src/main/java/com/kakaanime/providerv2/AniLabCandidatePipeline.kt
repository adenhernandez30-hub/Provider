package com.kakaanime.providerv2

/** Provider -> extractor boundary. */
class AniLabCandidatePipeline(private val extractorRegistry: AniLabExtractorRegistry) {
    suspend fun resolve(
        providerId: String,
        candidates: List<AniLabStreamCandidate>,
        context: AniLabExtractionContext = AniLabExtractionContext(),
    ): AniLabPipelineResult {
        val resolved = mutableListOf<AniLabStreamCandidate>()
        val failures = mutableListOf<AniLabFailure>()

        for (candidate in candidates) {
            if (candidate.url.isBlank()) {
                failures += AniLabFailure(providerId, AniLabFailureType.INVALID_MEDIA, "Candidate URL is blank")
                continue
            }

            val candidateContext = context.copy(
                referer = candidate.referer?.takeIf { it.isNotBlank() } ?: context.referer,
                headers = context.headers + candidate.headers,
                cookies = context.cookies + candidate.cookies,
            )

            when (val result = extractorRegistry.extract(candidate.url, candidateContext, providerId)) {
                is AniLabExtractionResult.Candidates -> {
                    resolved += result.candidates.map { extracted ->
                        extracted.copy(
                            providerId = providerId,
                            serverId = candidate.serverId,
                            referer = extracted.referer?.takeIf { it.isNotBlank() } ?: candidateContext.referer,
                            headers = candidateContext.headers + extracted.headers,
                            cookies = candidateContext.cookies + extracted.cookies,
                            subtitles = if (extracted.subtitles.isEmpty()) candidate.subtitles else extracted.subtitles,
                        )
                    }
                    failures += result.failures
                }

                is AniLabExtractionResult.Failure -> failures += result.failures
            }
        }

        return if (resolved.isNotEmpty()) {
            AniLabPipelineResult.Candidates(resolved, failures)
        } else {
            AniLabPipelineResult.Failure(
                failures.ifEmpty {
                    listOf(
                        AniLabFailure(
                            providerId,
                            AniLabFailureType.EXTRACTOR_FAILED,
                            "No stream candidate could be extracted",
                        ),
                    )
                },
            )
        }
    }
}

sealed interface AniLabPipelineResult {
    data class Candidates(val candidates: List<AniLabStreamCandidate>, val failures: List<AniLabFailure>) : AniLabPipelineResult
    data class Failure(val failures: List<AniLabFailure>) : AniLabPipelineResult
}
