package com.kakaanime.providerv2

import java.net.URI

class AniLabBloggerExtractor : AniLabExtractor {
    override val id = "blogger"

    override fun canHandle(url: String): Boolean {
        val host = runCatching { URI(url).host?.lowercase() }.getOrNull() ?: return false
        return host == "blogspot.com" ||
            host.endsWith(".blogspot.com") ||
            host == "blogger.com" ||
            host.endsWith(".blogger.com")
    }

    override suspend fun extract(
        url: String,
        context: AniLabExtractionContext,
    ): List<AniLabStreamCandidate> {
        val candidates = MEDIA.findAll(url)
            .mapNotNull { match ->
                resolve(url, match.groupValues[1])
            }
            .distinct()
            .map { media ->
                AniLabStreamCandidate(
                    providerId = "",
                    serverId = id,
                    url = media,
                    type = typeOf(media),
                    referer = context.referer ?: url,
                    headers = context.headers,
                    cookies = context.cookies,
                )
            }

        return candidates
    }

    private fun resolve(base: String, value: String): String? =
        runCatching { URI(base).resolve(value).toString() }
            .getOrNull()
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

    private fun typeOf(url: String) = when {
        url.contains(".m3u8", true) -> AniLabStreamType.HLS
        url.contains(".mpd", true) -> AniLabStreamType.DASH
        url.contains(".webm", true) -> AniLabStreamType.WEBM
        url.contains(".mp4", true) -> AniLabStreamType.MP4
        else -> AniLabStreamType.UNKNOWN
    }

    companion object {
        private val MEDIA = Regex(
            """(?:src|data-src|data-video|href)=["']([^"']+\.(?:m3u8|mpd|mp4|webm)(?:\?[^"']*)?)["']""",
            RegexOption.IGNORE_CASE,
        )
    }
}
