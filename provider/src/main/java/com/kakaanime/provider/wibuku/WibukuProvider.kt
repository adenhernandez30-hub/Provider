package com.kakaanime.provider.wibuku

import com.kakaanime.provider.AnimeProvider
import com.kakaanime.provider.ProviderAnime
import com.kakaanime.provider.ProviderEpisode
import com.kakaanime.provider.ProviderStream
import com.kakaanime.provider.StreamType

/**
 * Experimental Wibuku adapter.
 *
 * Wibuku exposes episode metadata/stream sources rather than a complete
 * anime catalog through the endpoint currently known to this project. The
 * catalog and link-resolution steps are therefore injected instead of being
 * guessed or coupled to the provider core.
 */
class WibukuProvider(
    private val api: WibukuApi,
    private val episodeIdResolver: (suspend (animeId: String, episodeNumber: Int) -> String?)? = null,
    private val streamLinkResolver: (suspend (source: WibukuStreamSource) -> String?)? = null
) : AnimeProvider {
    override val id: String = "wibuku"
    override val name: String = "Wibuku"
    override val priority: Int = 310

    override suspend fun search(query: String): List<ProviderAnime> = emptyList()

    override suspend fun getAnime(animeId: String): ProviderAnime? = null

    override suspend fun getEpisodes(animeId: String): List<ProviderEpisode> = emptyList()

    override suspend fun getStreams(animeId: String, episodeNumber: Int): List<ProviderStream> {
        if (animeId.isBlank() || episodeNumber < 1) return emptyList()
        val resolveEpisodeId = episodeIdResolver ?: return emptyList()
        val episodeId = resolveEpisodeId(animeId, episodeNumber)?.trim().orEmpty()
        if (episodeId.isBlank()) return emptyList()

        val response = api.getEpisodeMeta(episodeId) ?: return emptyList()
        if (response.httpCode !in 200..299 || response.status.equals("failed", ignoreCase = true)) return emptyList()

        return response.meta?.streamSources.orEmpty().mapNotNull { source ->
            val resolved = streamLinkResolver?.invoke(source) ?: source.link
            resolved.trim().takeIf { it.startsWith("http", ignoreCase = true) }?.let { url ->
                ProviderStream(
                    providerId = id,
                    url = url,
                    quality = source.quality,
                    type = detectType(source.type, url)
                )
            }
        }.distinctBy { Triple(it.url, it.quality, it.type) }
    }

    private fun detectType(rawType: String?, url: String): StreamType {
        val type = rawType.orEmpty().lowercase()
        return when {
            type.contains("hls") || type.contains("m3u8") || url.contains(".m3u8", true) -> StreamType.HLS
            type.contains("dash") || type.contains("mpd") || url.contains(".mpd", true) -> StreamType.DASH
            type.contains("mp4") || url.contains(".mp4", true) -> StreamType.MP4
            else -> StreamType.UNKNOWN
        }
    }
}
