package com.kakaanime.providerv2

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Provider-level routing only.
 *
 * AUTO may move from one provider to the next after a provider-level failure.
 * MANUAL is intentionally pinned to the selected provider.
 *
 * Actual media playability is verified by the integration layer (Media3 E2E);
 * this router does not claim that an HTTP URL is playable.
 */
class AniLabRouter(
    providers: List<AniLabProvider>,
    private val circuitBreaker: AniLabCircuitBreaker = AniLabCircuitBreaker(),
) {
    private val providers = providers.toList().also { list ->
        require(list.all { it.id.isNotBlank() }) { "Provider ids must not be blank" }
        require(list.map { it.id }.distinct().size == list.size) {
            "Provider ids must be unique"
        }
    }
    private val providersById = this.providers.associateBy { it.id }

    suspend fun loadLinks(
        episodeUrl: String,
        mode: AniLabRoutingMode,
        selectedProviderId: String? = null,
    ): AniLabRouteResult {
        val targets = when (mode) {
            AniLabRoutingMode.AUTO -> providers
            AniLabRoutingMode.MANUAL -> {
                val selected = selectedProviderId?.let(providersById::get)
                    ?: return AniLabRouteResult.Failure(
                        AniLabFailure(
                            providerId = selectedProviderId.orEmpty(),
                            type = AniLabFailureType.PROVIDER_UNAVAILABLE,
                            message = "Manual mode requires a registered provider",
                        ),
                    )
                listOf(selected)
            }
        }

        val failures = mutableListOf<AniLabFailure>()

        for (provider in targets) {
            if (!circuitBreaker.allow(provider.id)) {
                val failure = AniLabFailure(
                    providerId = provider.id,
                    type = AniLabFailureType.PROVIDER_UNAVAILABLE,
                    message = "Provider circuit is open",
                )
                failures += failure
                continue
            }

            try {
                val candidates = provider.loadLinks(episodeUrl)
                if (candidates.isNotEmpty()) {
                    circuitBreaker.recordSuccess(provider.id)
                    return AniLabRouteResult.Candidates(
                        providerId = provider.id,
                        candidates = candidates,
                        failures = failures.toList(),
                    )
                }

                val failure = AniLabFailure(
                    providerId = provider.id,
                    type = AniLabFailureType.SERVER_EMPTY,
                    message = "Provider returned no stream candidates",
                )
                failures += failure
                circuitBreaker.recordFailure(provider.id)
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                val failure = AniLabFailure(
                    providerId = provider.id,
                    type = classify(t),
                    message = t.message,
                    cause = t,
                )
                failures += failure
                circuitBreaker.recordFailure(provider.id)
            }
        }

        return AniLabRouteResult.Failure(
            primary = failures.lastOrNull()
                ?: AniLabFailure(
                    providerId = selectedProviderId.orEmpty(),
                    type = AniLabFailureType.PROVIDER_UNAVAILABLE,
                    message = "No provider produced candidates",
                ),
            failures = failures,
        )
    }

    /**
     * Provider routing with extraction and HTTP verification.
     *
     * AUTO advances when a provider returns candidates but none survive verification.
     * MANUAL remains pinned to the selected provider. Media3 first-frame playback is
     * still the final playability check outside provider-v2.
     */
    suspend fun loadVerifiedLinks(
        episodeUrl: String,
        mode: AniLabRoutingMode,
        verifiedPipeline: AniLabVerifiedCandidatePipeline,
        selectedProviderId: String? = null,
        context: AniLabExtractionContext = AniLabExtractionContext(),
    ): AniLabVerifiedRouteResult {
        val targets = when (mode) {
            AniLabRoutingMode.AUTO -> providers
            AniLabRoutingMode.MANUAL -> {
                val selected = selectedProviderId?.let(providersById::get)
                    ?: return AniLabVerifiedRouteResult.Failure(
                        failures = listOf(
                            AniLabFailure(
                                providerId = selectedProviderId.orEmpty(),
                                type = AniLabFailureType.PROVIDER_UNAVAILABLE,
                                message = "Manual mode requires a registered provider",
                            ),
                        ),
                    )
                listOf(selected)
            }
        }

        val failures = mutableListOf<AniLabFailure>()
        for (provider in targets) {
            if (!circuitBreaker.allow(provider.id)) {
                failures += AniLabFailure(
                    providerId = provider.id,
                    type = AniLabFailureType.PROVIDER_UNAVAILABLE,
                    message = "Provider circuit is open",
                )
                continue
            }

            try {
                val candidates = provider.loadLinks(episodeUrl)
                if (candidates.isEmpty()) {
                    failures += AniLabFailure(
                        provider.id,
                        AniLabFailureType.SERVER_EMPTY,
                        "Provider returned no stream candidates",
                    )
                    circuitBreaker.recordFailure(provider.id)
                    continue
                }

                when (val verified = verifiedPipeline.resolve(provider.id, candidates, context)) {
                    is AniLabVerifiedPipelineResult.Candidates -> {
                        circuitBreaker.recordSuccess(provider.id)
                        return AniLabVerifiedRouteResult.Candidates(
                            providerId = provider.id,
                            candidates = verified.candidates,
                            failures = failures + verified.failures,
                        )
                    }
                    is AniLabVerifiedPipelineResult.Failure -> {
                        failures += verified.failures
                        circuitBreaker.recordFailure(provider.id)
                    }
                }
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                failures += AniLabFailure(
                    providerId = provider.id,
                    type = classify(t),
                    message = t.message,
                    cause = t,
                )
                circuitBreaker.recordFailure(provider.id)
            }
        }

        return AniLabVerifiedRouteResult.Failure(
            failures = failures.ifEmpty {
                listOf(
                    AniLabFailure(
                        providerId = selectedProviderId.orEmpty(),
                        type = AniLabFailureType.PROVIDER_UNAVAILABLE,
                        message = "No provider produced a verified stream",
                    ),
                )
            },
        )
    }

    private fun classify(t: Throwable): AniLabFailureType =
        when (t) {
            is TimeoutCancellationException, is SocketTimeoutException -> AniLabFailureType.TIMEOUT
            is IOException -> AniLabFailureType.PROVIDER_UNAVAILABLE
            else -> AniLabFailureType.PROVIDER_UNAVAILABLE
        }
}

sealed interface AniLabRouteResult {
    data class Candidates(
        val providerId: String,
        val candidates: List<AniLabStreamCandidate>,
        val failures: List<AniLabFailure>,
    ) : AniLabRouteResult

    data class Failure(
        val primary: AniLabFailure,
        val failures: List<AniLabFailure> = emptyList(),
    ) : AniLabRouteResult
}


sealed interface AniLabVerifiedRouteResult {
    data class Candidates(
        val providerId: String,
        val candidates: List<AniLabStreamCandidate>,
        val failures: List<AniLabFailure>,
    ) : AniLabVerifiedRouteResult

    data class Failure(
        val failures: List<AniLabFailure>,
    ) : AniLabVerifiedRouteResult
}
