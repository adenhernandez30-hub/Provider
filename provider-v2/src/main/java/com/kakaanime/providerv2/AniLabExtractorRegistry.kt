package com.kakaanime.providerv2

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import java.net.SocketTimeoutException

/** Ordered extractor registry with fallback across compatible extractors. */
class AniLabExtractorRegistry(
    extractors: List<AniLabExtractor>,
) {
    private val extractors = extractors.toList()

    fun find(url: String): List<AniLabExtractor> =
        extractors.filter { extractor ->
            try {
                extractor.canHandle(url)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                false
            }
        }

    suspend fun extract(
        url: String,
        context: AniLabExtractionContext = AniLabExtractionContext(),
        providerId: String = "",
    ): AniLabExtractionResult {
        val failures = mutableListOf<AniLabFailure>()

        for (extractor in find(url)) {
            try {
                val candidates = extractor.extract(url, context)
                if (candidates.isNotEmpty()) {
                    return AniLabExtractionResult.Candidates(
                        extractorId = extractor.id,
                        candidates = candidates.map {
                            if (it.extractorId == null) it.copy(extractorId = extractor.id) else it
                        },
                        failures = failures.toList(),
                    )
                }
                failures += AniLabFailure(
                    providerId,
                    AniLabFailureType.EXTRACTOR_FAILED,
                    "Extractor ${extractor.id} returned no candidates",
                )
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                failures += AniLabFailure(
                    providerId,
                    if (t is TimeoutCancellationException || t is SocketTimeoutException) AniLabFailureType.TIMEOUT
                    else AniLabFailureType.EXTRACTOR_FAILED,
                    t.message,
                    t,
                )
            }
        }

        return AniLabExtractionResult.Failure(
            failures.lastOrNull() ?: AniLabFailure(
                providerId,
                AniLabFailureType.EXTRACTOR_FAILED,
                "No compatible extractor found for URL",
            ),
            failures,
        )
    }
}

sealed interface AniLabExtractionResult {
    data class Candidates(
        val extractorId: String,
        val candidates: List<AniLabStreamCandidate>,
        val failures: List<AniLabFailure>,
    ) : AniLabExtractionResult

    data class Failure(
        val failure: AniLabFailure,
        val failures: List<AniLabFailure>,
    ) : AniLabExtractionResult
}
