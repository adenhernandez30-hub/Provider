package com.kakaanime.provider

interface ProviderMappingSource {
    suspend fun findMappings(identity: CanonicalAnimeIdentity): List<ProviderMapping>
}

/**
 * Resolves canonical identity to provider-specific anime references.
 *
 * Persistence, external mapping APIs, and cache policy remain outside this
 * class so they can evolve without changing ProviderEngine or providers.
 */
class ProviderMappingResolver(
    private val source: ProviderMappingSource,
) {
    suspend fun resolve(identity: CanonicalAnimeIdentity): List<ProviderAnimeRef> =
        source.findMappings(identity)
            .asSequence()
            .filter { it.anilistId == identity.anilistId }
            .filter { it.providerId.isNotBlank() && it.providerAnimeId.isNotBlank() }
            .sortedWith(
                compareByDescending<ProviderMapping> { it.confidence }
                    .thenBy { it.providerId }
                    .thenBy { it.providerAnimeId }
            )
            .distinctBy { "${it.providerId}:${it.providerAnimeId}" }
            .map {
                ProviderAnimeRef(
                    providerId = it.providerId,
                    animeId = it.providerAnimeId,
                    confidence = it.confidence,
                    seasonNumber = it.seasonNumber,
                )
            }
            .toList()
}
