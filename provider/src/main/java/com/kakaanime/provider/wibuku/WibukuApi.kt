package com.kakaanime.provider.wibuku

/**
 * Minimal Wibuku API boundary so the provider can be tested without a live
 * network or real credentials.
 */
interface WibukuApi {
    suspend fun getEpisodeMeta(id: String, mode: Int = 0): EpisodeMetaResponse?
}
