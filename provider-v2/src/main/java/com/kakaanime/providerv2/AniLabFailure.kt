package com.kakaanime.providerv2

enum class AniLabFailureType {
    PROVIDER_UNAVAILABLE,
    SEARCH_FAILED,
    EPISODE_NOT_FOUND,
    SERVER_EMPTY,
    EXTRACTOR_FAILED,
    BAD_HEADERS,
    HTTP_BLOCKED,
    INVALID_MEDIA,
    MANIFEST_INVALID,
    TIMEOUT,
    PLAYBACK_FAILED,
}

data class AniLabFailure(
    val providerId: String,
    val type: AniLabFailureType,
    val message: String? = null,
    val cause: Throwable? = null,
)
