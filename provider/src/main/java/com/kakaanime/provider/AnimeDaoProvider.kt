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
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
    private val catalog = NativeHtmlProvider(id, name, priority, baseUrl, browserResolver)
    private val resolver = StreamResolver(ExtractorRegistry(browserResolver = browserResolver), browserResolver = browserResolver)
    override suspend fun search(query: String) = catalog.search(query)
    override suspend fun getAnime(animeId: String) = catalog.getAnime(animeId)
    override suspend fun getEpisodes(animeId: String) = catalog.getEpisodes(animeId)
    override suspend fun getStreams(animeId: String, episodeNumber: Int): List<ProviderStream> {
        val raw = animeId.removePrefix("$id:").trimEnd('/')
        val episodeUrl = if (raw.contains("/episodes/", true)) raw.replace(Regex("1x\\d+"), "1x" + episodeNumber)
            else baseUrl + "/episodes/" + raw.substringAfterLast("/anime/").substringBefore("?").trim('/') + "-1x" + episodeNumber + "/"
        val doc = getDocument(episodeUrl) ?: return emptyList()
        val candidates = buildList {
            known(doc, "gstore", "source").forEach(::add)
            known(doc, "vid", "video").forEach(::add)
            known(doc, "fembed", "iframe").forEach(::add)
            known(doc, "sb", "iframe").forEach(::add)
            known(doc, "streamtape", "iframe").forEach(::add)
            known(doc, "mixdrop", "iframe").forEach(::add)

            // AnimeDao can move the active provider outside #videocontent
            // after a player refresh. Collect page-level candidates too.
            doc.select("iframe[src], iframe[data-src], embed[src], video[src], video[data-src], video source[src], video source[data-src], source[src], source[data-src]")
                .map {
                    it.absUrl("src").ifBlank { it.attr("src") }
                        .ifBlank { it.absUrl("data-src") }
                        .ifBlank { it.attr("data-src") }
                }
                .filter { it.isNotBlank() }
                .forEach(::add)
        }.filter { it.startsWith("http", true) }.distinct()
        if (candidates.isEmpty()) return resolver.resolve(listOf(episodeUrl), referer = baseUrl)
        return resolver.resolve(candidates, referer = episodeUrl).map { it.copy(providerId = id) }
    }
    private fun known(doc: Document, id: String, kind: String): List<String> {
        val wrapper = doc.selectFirst("#videowrapper_$id") ?: return emptyList()
        return if (kind == "source") wrapper.select("video source[src]").map { it.absUrl("src") }
        else if (kind == "video") wrapper.select("video[src], video source[src]").map { it.absUrl("src").ifBlank { it.absUrl("data-src") } }
        else wrapper.select("iframe[src]").map { it.absUrl("src").ifBlank { it.absUrl("data-src") } }
    }
    private suspend fun getDocument(url: String): Document? = withContext(Dispatchers.IO) {
        runCatching { val request = Request.Builder().url(url).header("User-Agent", UA).header("Accept", "text/html,application/xhtml+xml").header("Referer", "$baseUrl/").build()
            client.newCall(request).execute().use { response -> if (!response.isSuccessful) null else response.body?.string()?.let { Jsoup.parse(it, url) } } }.getOrNull()
    }
    companion object { private const val UA = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/140.0.0.0 Mobile Safari/537.36" }
}
