package com.kakaanime.providerv2

/** Candidate pipeline + HTTP/media verification. Media3 playback remains the final playability check. */
class AniLabVerifiedCandidatePipeline(
    private val candidatePipeline: AniLabCandidatePipeline,
    private val verifier: AniLabStreamVerifier,
) {
    suspend fun resolve(
        providerId: String,
        candidates: List<AniLabStreamCandidate>,
        context: AniLabExtractionContext = AniLabExtractionContext(),
    ): AniLabVerifiedPipelineResult {
        return when (val extracted = candidatePipeline.resolve(providerId, candidates, context)) {
            is AniLabPipelineResult.Failure -> AniLabVerifiedPipelineResult.Failure(extracted.failures)
            is AniLabPipelineResult.Candidates -> {
                val valid = mutableListOf<AniLabStreamCandidate>()
                val failures = extracted.failures.toMutableList()

                for (candidate in extracted.candidates) {
                    when (val result = verifier.verify(candidate)) {
                        is AniLabVerificationResult.Valid -> valid += result.candidate
                        is AniLabVerificationResult.Failure -> failures += result.failure
                    }
                }

                if (valid.isNotEmpty()) {
                    AniLabVerifiedPipelineResult.Candidates(valid, failures)
                } else {
                    AniLabVerifiedPipelineResult.Failure(
                        failures.ifEmpty {
                            listOf(
                                AniLabFailure(
                                    providerId,
                                    AniLabFailureType.INVALID_MEDIA,
                                    "No extracted candidate passed stream verification",
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

sealed interface AniLabVerifiedPipelineResult {
    data class Candidates(
        val candidates: List<AniLabStreamCandidate>,
        val failures: List<AniLabFailure>,
    ) : AniLabVerifiedPipelineResult

    data class Failure(val failures: List<AniLabFailure>) : AniLabVerifiedPipelineResult
}
