package com.kakaanime.provider

import com.kakaanime.provider.extractor.BrowserStreamResolver
import com.kakaanime.provider.extractor.ExtractorRegistry
import com.kakaanime.provider.extractor.StreamResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

/** AnimeDao-specific player discovery. */
class AnimeDaoProvider(browserResolver: BrowserStreamResolver? = null) : AnimeProvider {
    override val id = "animedao"
    override val name = "AnimeDao"
    override val priority = 35
    private val baseUrl = "https://animedao.in"
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).followRedirects(true).build()
    private val catalog = NativeHtmlProvider(id, name, priority, baseUrl, browserResolver)
    private val resolver = StreamResolver(ExtractorRegistry(browserResolver = browserResolver), browserResolver = browserResolver)

    override suspend fun search(query: String) = catalog.search(query)
    override suspend fun getAnime(animeId: String) = catalog.getAnime(animeId)
    override suspend fun getEpisodes(animeId: String) = catalog.getEpisodes(animeId)

    override suspend fun getStreams(animeId: String, episodeNumber: Int): List<ProviderStream> {
        val raw = animeId.removePrefix("$id:").trimEnd('/')
        val episodeUrl = getEpisodes(animeId).firstOrNull { it.number == episodeNumber }?.id?.removePrefix("$id:")?.takeIf { it.startsWith("http", true) }
            ?: if (raw.contains("/episodes/", true)) raw.replace(Regex("\\d+x\\d+"), "1x$episodeNumber")
            else "$baseUrl/episodes/${raw.substringAfterLast("/anime/").substringBefore("?").trim('/')}-1x$episodeNumber/"
        val doc = getDocument(episodeUrl) ?: return resolver.resolve(listOf(episodeUrl), referer = episodeUrl)
        val candidates = linkedSetOf<String>()
        doc.select("#videocontent iframe[src], #videocontent iframe[data-src], #videocontent video[src], #videocontent video source[src], #videocontent video source[data-src], iframe[src], iframe[data-src], embed[src], embed[data-src], video[src], video[data-src], video source[src], video source[data-src], source[src], source[data-src]").forEach { element ->
            val value = element.absUrl("src").ifBlank { element.attr("src") }.ifBlank { element.absUrl("data-src") }.ifBlank { element.attr("data-src") }
            if (value.isNotBlank()) candidates += value
        }
        // Some AnimeDao themes place the player in a data attribute or inline
        // script rather than an iframe node.
        doc.select("[data-video], [data-url], [data-embed], [data-src]").forEach { element ->
            listOf("data-video", "data-url", "data-embed", "data-src").map { element.attr(it) }.filter { it.startsWith("http", true) }.forEach { candidates += it }
        }
        Regex("https?://[^\\s\"'<>]+\\.(?:m3u8|mpd|mp4)(?:\\?[^\\s\"'<>]*)?", RegexOption.IGNORE_CASE)
            .findAll(doc.html()).forEach { candidates += it.value }
        if (candidates.isEmpty()) return resolver.resolve(listOf(episodeUrl), referer = episodeUrl)
        return resolver.resolve(candidates.toList(), referer = episodeUrl).map { it.copy(providerId = id) }
    }

    private suspend fun getDocument(url: String): Document? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).header("User-Agent", UA).header("Accept", "text/html,application/xhtml+xml").header("Referer", "$baseUrl/").build()
            client.newCall(request).execute().use { response -> if (!response.isSuccessful) null else response.body?.string()?.takeIf { it.isNotBlank() }?.let { Jsoup.parse(it, url) } }
        }.getOrNull()
    }

    companion object { private const val UA = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/140.0.0.0 Mobile Safari/537.36" }
}
