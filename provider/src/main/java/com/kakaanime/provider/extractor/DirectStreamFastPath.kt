package com.kakaanime.provider.extractor

/**
 * Identifies URLs that can be attempted directly before running the extractor
 * chain. This is only a fast-path hint; StreamValidator remains authoritative.
 */
object DirectStreamFastPath {
    private val mediaPattern = Regex(
        """(?i)(?:^|[/?&])[^/?&]+\.(?:m3u8|mpd|mp4)(?:$|[?#&])"""
    )

    fun candidates(urls: List<String>): List<String> =
        urls.map(String::trim)
            .filter(String::isNotBlank)
            .filter(::isDirectMediaUrl)
            .distinct()

    fun isDirectMediaUrl(url: String): Boolean =
        mediaPattern.containsMatchIn(url.trim())
}
