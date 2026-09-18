package com.kakaanime.provider

/**
 * Optional persistence boundary for provider mappings.
 *
 * Implementations may use a database, file, remote service, or no persistence
 * at all. The provider core only depends on this contract.
 */
interface ProviderMappingStore {
    suspend fun get(anilistId: Int): List<ProviderMapping>?
    suspend fun put(anilistId: Int, mappings: List<ProviderMapping>)
}
