package com.kakaanime.providerv2

interface AniLabExtractor {
    val id: String

    fun canHandle(url: String): Boolean

    suspend fun extract(
        url: String,
        context: AniLabExtractionContext,
    ): List<AniLabStreamCandidate>
}

data class AniLabExtractionContext(
    val referer: String? = null,
    val origin: String? = null,
    val userAgent: String? = null,
    val cookies: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
)
