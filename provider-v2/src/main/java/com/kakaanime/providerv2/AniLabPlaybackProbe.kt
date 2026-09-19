package com.kakaanime.providerv2

/**
 * Final playback boundary owned by the Android integration layer.
 *
 * provider-v2 deliberately does not depend on Media3. The app can implement this
 * interface with Media3 and report success only after actual playback reaches the
 * chosen readiness signal, e.g. onRenderedFirstFrame().
 */
fun interface AniLabPlaybackProbe {
    suspend fun probe(candidate: AniLabStreamCandidate): AniLabPlaybackProbeResult
}

sealed interface AniLabPlaybackProbeResult {
    data object Playable : AniLabPlaybackProbeResult

    data class Failed(
        val failure: AniLabFailure,
    ) : AniLabPlaybackProbeResult
}
