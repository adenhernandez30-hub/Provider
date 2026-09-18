package com.kakaanime.provider

/**
 * Canonical identity used to resolve an anime independently of any provider.
 */
data class CanonicalAnimeIdentity(
    val anilistId: Int,
    val title: String,
    val alternativeTitles: List<String> = emptyList(),
    val year: Int? = null,
    val format: String? = null,
    val seasonNumber: Int? = null,
    val seasonTitle: String? = null,
) {
    init {
        require(anilistId > 0) { "anilistId must be positive" }
        require(title.isNotBlank()) { "title must not be blank" }
    }
}
