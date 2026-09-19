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

    /**
     * Full routing boundary including the app's real playback probe.
     *
     * A provider is successful only when at least one verified candidate also
     * passes the playback probe. AUTO can therefore move to the next provider
     * after Media3 rejects every candidate from the current provider.
     */
    suspend fun loadPlayableLinks(
        episodeUrl: String,
        mode: AniLabRoutingMode,
        verifiedPipeline: AniLabVerifiedCandidatePipeline,
        playbackProbe: AniLabPlaybackProbe,
        selectedProviderId: String? = null,
        context: AniLabExtractionContext = AniLabExtractionContext(),
    ): AniLabPlayableRouteResult {
        val targets = when (mode) {
            AniLabRoutingMode.AUTO -> providers
            AniLabRoutingMode.MANUAL -> {
                val selected = selectedProviderId?.let(providersById::get)
                    ?: return AniLabPlayableRouteResult.Failure(
                        listOf(
                            AniLabFailure(
                                selectedProviderId.orEmpty(),
                                AniLabFailureType.PROVIDER_UNAVAILABLE,
                                "Manual mode requires a registered provider",
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
                    provider.id,
                    AniLabFailureType.PROVIDER_UNAVAILABLE,
                    "Provider circuit is open",
                )
                continue
            }

            try {
                val rawCandidates = provider.loadLinks(episodeUrl)
                if (rawCandidates.isEmpty()) {
                    failures += AniLabFailure(
                        provider.id,
                        AniLabFailureType.SERVER_EMPTY,
                        "Provider returned no stream candidates",
                    )
                    circuitBreaker.recordFailure(provider.id)
                    continue
                }

                when (val verified = verifiedPipeline.resolve(provider.id, rawCandidates, context)) {
                    is AniLabVerifiedPipelineResult.Failure -> {
                        failures += verified.failures
                        circuitBreaker.recordFailure(provider.id)
                    }
                    is AniLabVerifiedPipelineResult.Candidates -> {
                        var playable: AniLabStreamCandidate? = null
                        for (candidate in verified.candidates) {
                            when (val probe = playbackProbe.probe(candidate)) {
                                AniLabPlaybackProbeResult.Playable -> {
                                    playable = candidate
                                    break
                                }
                                is AniLabPlaybackProbeResult.Failed -> failures += probe.failure.copy(
                                    providerId = provider.id,
                                )
                            }
                        }

                        if (playable != null) {
                            circuitBreaker.recordSuccess(provider.id)
                            return AniLabPlayableRouteResult.Candidates(
                                providerId = provider.id,
                                candidate = playable,
                                failures = failures + verified.failures,
                            )
                        }

                        failures += verified.failures
                        if (failures.none { it.providerId == provider.id && it.type == AniLabFailureType.PLAYBACK_FAILED }) {
                            failures += AniLabFailure(
                                provider.id,
                                AniLabFailureType.PLAYBACK_FAILED,
                                "No verified candidate passed the playback probe",
                            )
                        }
                        circuitBreaker.recordFailure(provider.id)
                    }
                }
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                failures += AniLabFailure(provider.id, classify(t), t.message, t)
                circuitBreaker.recordFailure(provider.id)
            }
        }

        return AniLabPlayableRouteResult.Failure(
            failures.ifEmpty {
                listOf(
                    AniLabFailure(
                        selectedProviderId.orEmpty(),
                        AniLabFailureType.PROVIDER_UNAVAILABLE,
                        "No provider produced a playable stream",
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


sealed interface AniLabPlayableRouteResult {
    data class Candidates(
        val providerId: String,
        val candidate: AniLabStreamCandidate,
        val failures: List<AniLabFailure>,
    ) : AniLabPlayableRouteResult

    data class Failure(
        val failures: List<AniLabFailure>,
    ) : AniLabPlayableRouteResult
}
