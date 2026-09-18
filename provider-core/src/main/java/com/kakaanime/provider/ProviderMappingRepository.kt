package com.kakaanime.provider

import kotlinx.coroutines.CoroutineScope

/**
 * Mapping repository combining optional persistence with an in-memory cache.
 *
 * Cache hits avoid both persistence and source calls. Concurrent misses for
 * the same AniList ID share one in-flight source load.
 */
class ProviderMappingRepository(
    private val source: ProviderMappingSource,
    private val cache: ProviderMappingCache,
    private val store: ProviderMappingStore? = null,
) : ProviderMappingSource {

    override suspend fun findMappings(identity: CanonicalAnimeIdentity): List<ProviderMapping> {
        cache.get(identity.anilistId)?.let { return it }

        val persisted = store?.get(identity.anilistId)
            ?.takeIf { it.isNotEmpty() }
        if (persisted != null) {
            cache.getOrLoad(identity.anilistId) { persisted }
            return persisted
        }

        return cache.getOrLoad(identity.anilistId) {
            source.findMappings(identity)
                .filter { it.anilistId == identity.anilistId }
                .also { mappings ->
                    store?.put(identity.anilistId, mappings)
                }
        }
    }
}
