package com.kakaanime.providerv2

/**
 * Native provider contract for AniLab Provider v2.
 *
 * Site-specific scraping stays behind this boundary. The router never needs
 * to know how a provider finds anime, episodes, servers, or media URLs.
 */
interface AniLabProvider {
    val id: String

    suspend fun search(query: String): List<AniLabSearchResult>

    suspend fun load(url: String): AniLabAnime?

    suspend fun loadLinks(episodeUrl: String): List<AniLabStreamCandidate>
}
