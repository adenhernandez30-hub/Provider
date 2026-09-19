package com.kakaanime.providerv2

enum class AniLabStreamType {
    HLS,
    DASH,
    MP4,
    WEBM,
    UNKNOWN
}

data class AniLabSearchResult(
    val id: String,
    val title: String,
    val url: String,
    val providerId: String,
)

data class AniLabAnime(
    val id: String,
    val title: String,
    val url: String,
    val providerId: String,
    val episodes: List<AniLabEpisode> = emptyList(),
)

data class AniLabEpisode(
    val number: Int,
    val title: String? = null,
    val url: String,
)

data class AniLabSubtitle(
    val url: String,
    val language: String,
)

data class AniLabStreamCandidate(
    val providerId: String,
    val serverId: String,
    val url: String,
    val type: AniLabStreamType = AniLabStreamType.UNKNOWN,
    val quality: Int? = null,
    val referer: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val cookies: Map<String, String> = emptyMap(),
    val subtitles: List<AniLabSubtitle> = emptyList(),
    val extractorId: String? = null,
)
