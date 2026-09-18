package com.kakaanime.provider

/**
 * A resolved link between a canonical anime identity and one provider's anime ID.
 *
 * confidence is a normalized 0.0..1.0 signal supplied by the mapping source.
 */
data class ProviderMapping(
    val anilistId: Int,
    val providerId: String,
    val providerAnimeId: String,
    val confidence: Double = 1.0,
    val seasonNumber: Int? = null,
) {
    init {
        require(anilistId > 0) { "anilistId must be positive" }
        require(providerId.isNotBlank()) { "providerId must not be blank" }
        require(providerAnimeId.isNotBlank()) { "providerAnimeId must not be blank" }
        require(confidence in 0.0..1.0) { "confidence must be between 0.0 and 1.0" }
    }
}

data class ProviderAnimeRef(
    val providerId: String,
    val animeId: String,
    val confidence: Double = 1.0,
    val seasonNumber: Int? = null,
)
