package com.kakaanime.provider.wibuku

/**
 * Resolves the provider-specific Wibuku episode id from the app's stable
 * anime id and episode number. Kept injectable because the currently known
 * Wibuku endpoint does not expose a catalog endpoint in this project.
 */
fun interface WibukuEpisodeIdResolver {
    suspend fun resolve(animeId: String, episodeNumber: Int): String?
}
