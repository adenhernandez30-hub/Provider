package com.kakaanime.providerv2

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

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
    private val providers = providers.toList()
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

    private fun classify(t: Throwable): AniLabFailureType =
        if (t is TimeoutCancellationException) {
            AniLabFailureType.TIMEOUT
        } else {
            AniLabFailureType.PROVIDER_UNAVAILABLE
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
