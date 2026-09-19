package com.kakaanime.providerv2

/** Provider -> extractor boundary. */
class AniLabCandidatePipeline(private val extractorRegistry: AniLabExtractorRegistry) {
    suspend fun resolve(providerId: String, candidates: List<AniLabStreamCandidate>, context: AniLabExtractionContext = AniLabExtractionContext()): AniLabPipelineResult {
        val resolved = mutableListOf<AniLabStreamCandidate>()
        val failures = mutableListOf<AniLabFailure>()
        for (candidate in candidates) {
            if (candidate.url.isBlank()) {
                failures += AniLabFailure(providerId, AniLabFailureType.INVALID_MEDIA, "Candidate URL is blank")
                continue
            }
            val result = extractorRegistry.extract(candidate.url, context.copy(
                referer = candidate.referer ?: context.referer,
                headers = context.headers + candidate.headers,
                cookies = context.cookies + candidate.cookies,
            ))
            when (result) {
                is AniLabExtractionResult.Candidates -> {
                    resolved += result.candidates.map { extracted -> extracted.copy(
                        providerId = providerId,
                        serverId = candidate.serverId,
                        subtitles = if (extracted.subtitles.isEmpty()) candidate.subtitles else extracted.subtitles,
                    ) }
                    failures += result.failures
                }
                is AniLabExtractionResult.Failure -> failures += result.failures
            }
        }
        return if (resolved.isNotEmpty()) AniLabPipelineResult.Candidates(resolved, failures)
        else AniLabPipelineResult.Failure(failures.ifEmpty { listOf(AniLabFailure(providerId, AniLabFailureType.EXTRACTOR_FAILED, "No stream candidate could be extracted")) })
    }
}

sealed interface AniLabPipelineResult {
    data class Candidates(val candidates: List<AniLabStreamCandidate>, val failures: List<AniLabFailure>) : AniLabPipelineResult
    data class Failure(val failures: List<AniLabFailure>) : AniLabPipelineResult
}