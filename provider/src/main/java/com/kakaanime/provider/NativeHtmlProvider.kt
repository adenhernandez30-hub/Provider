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
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * HTML-first native adapter. Unlike RemoteSourceProviderV2 this talks directly to the
 * configured site's pages, then hands episode pages to the same shared StreamResolver
 * used by Samehadaku.
 */
class NativeHtmlProvider(
    override val id: String,
    override val name: String,
    override val priority: Int,
    private val baseUrl: String,
    browserResolver: BrowserStreamResolver? = null
) : AnimeProvider {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    private val resolver = StreamResolver(ExtractorRegistry(browserResolver = browserResolver), browserResolver = browserResolver)

    override suspend fun search(query: String): List<ProviderAnime> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val urls = listOf(
            "$baseUrl/?s=${encode(q)}",
            "$baseUrl/search/${slug(q)}/",
            "$baseUrl/?post_type=post&s=${encode(q)}",
            "$baseUrl/search/?q=${encode(q)}"
        ).distinct()
        for (url in urls) requestDocument(url)?.extractSearch()?.takeIf { it.isNotEmpty() }?.let { return it }
        return emptyList()
    }

    override suspend fun getAnime(animeId: String): ProviderAnime? {
        val url = normalizeUrl(animeId)
        return requestDocument(url)?.extractDetail(url)
    }

    override suspend fun getEpisodes(animeId: String): List<ProviderEpisode> {
        val animeUrl = normalizeUrl(animeId)
        val document = requestDocument(animeUrl) ?: return emptyList()
        return document.extractEpisodes(animeUrl)
    }

    override suspend fun getStreams(animeId: String, episodeNumber: Int): List<ProviderStream> {
        val episode = getEpisodes(animeId).firstOrNull { it.number == episodeNumber } ?: return emptyList()
        val episodeUrl = episode.id.removePrefix("$id:").trim()
        if (!episodeUrl.startsWith("http", true)) return emptyList()
        // The episode page is the source boundary. Preserve it as the referer so
        // hosts that require the normal page-to-embed flow receive the correct
        // origin instead of the catalog root.
        return resolver.resolve(listOf(episodeUrl), referer = episodeUrl)
            .map { it.copy(providerId = id) }
    }

    private fun normalizeUrl(value: String): String {
        val raw = value.removePrefix("$id:").trim()
        return when {
            raw.startsWith("http", true) -> raw
            raw.startsWith("/") -> baseUrl.trimEnd('/') + raw
            else -> baseUrl.trimEnd('/') + "/" + raw.trim('/') + "/"
        }
    }

    private suspend fun requestDocument(url: String): Document? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url)
                .header("User-Agent", UA)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Referer", "$baseUrl/")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()?.takeIf { it.isNotBlank() }
                    ?.let { Jsoup.parse(it, response.request.url.toString()) }
            }
        }.getOrNull()
    }

    private fun Document.extractSearch(): List<ProviderAnime> {
        val selectors = listOf("article, div.bs, div.bsx, div.animepost, div.listupd .bs, div.listupd .bsx", "div.item, div.items, div.post, div.postbody")
        for (selector in selectors) {
            val result = select(selector).mapNotNull { it.toSearchAnime() }.distinctBy { it.id }
            if (result.isNotEmpty()) return result
        }
        return select("a[href]").mapNotNull { anchor ->
            val href = anchor.absUrl("href")
            val title = anchor.attr("title").trim().ifBlank { anchor.text().trim() }
            if (href.isBlank() || title.isBlank() || !looksLikeAnimeUrl(href)) null
            else ProviderAnime("$id:$href", title, id, posterUrl = anchor.selectFirst("img")?.let { imageUrl(it) })
        }.distinctBy { it.id }.take(30)
    }

    private fun Element.toSearchAnime(): ProviderAnime? {
        val anchor = selectFirst("a[href*='/anime/'], a[href*='/series/'], a[href*='/title/'], .title a, .tt a, h2 a, h3 a, a[href]") ?: return null
        val href = anchor.absUrl("href")
        val title = selectFirst(".title, .tt, h2, h3, h4")?.text()?.trim() ?: anchor.attr("title").trim().ifBlank { anchor.text().trim() }
        if (href.isBlank() || title.isBlank() || !looksLikeAnimeUrl(href)) return null
        return ProviderAnime("$id:$href", title, id, posterUrl = selectFirst("img")?.let { imageUrl(it) })
    }

    private fun Document.extractDetail(url: String): ProviderAnime? {
        val title = selectFirst("h1.entry-title, h1.title, h1, .entry-title")?.text()?.trim() ?: return null
        val canonical = selectFirst("link[rel=canonical]")?.absUrl("href")?.ifBlank { url } ?: url
        val description = select(".desc p, .synopsis p, .entry-content p, .description p, .sinopsis p").text().trim()
        val genres = select("a[href*='/genre/'], .genre a, .genres a").map { it.text().trim() }.filter { it.isNotBlank() }.distinct()
        val poster = selectFirst(".thumb img, .fotoanime img, .poster img, img")?.let { imageUrl(it) }
        val year = Regex("\\b(19\\d{2}|20\\d{2})\\b").find(select(".spe, .info, .infozingle, .metadata").text())?.value?.toIntOrNull()
        return ProviderAnime("$id:$canonical", title, id, posterUrl = poster, description = description, genres = genres, year = year, status = "UNKNOWN", latestEpisode = extractEpisodes(canonical).maxOfOrNull { it.number })
    }

    private fun Document.extractEpisodes(animeUrl: String): List<ProviderEpisode> {
        val result = linkedMapOf<Int, ProviderEpisode>()
        val selectors = listOf(
            "div.lstepsiode.listeps li, div.listeps li, div.episodelist li",
            ".episodelist a, .listeps a, .eplister a, .episodes a",
            "a[href*='episode']"
        )
        // Merge all known layouts. Returning on the first non-empty selector
        // silently dropped episodes when a page exposed multiple partial lists.
        selectors.asSequence().flatMap { select(it).asSequence() }.forEach { element ->
            val anchor = if (element.tagName() == "a") element else element.selectFirst("a") ?: return@forEach
            val href = anchor.absUrl("href").ifBlank { anchor.absUrl("data-href") }.ifBlank { anchor.attr("href") }.ifBlank { anchor.attr("data-href") }
            if (href.isBlank()) return@forEach
            val text = anchor.text().trim().ifBlank { anchor.attr("title").trim() }
            val number = episodeNumber(text, href) ?: return@forEach
            result.putIfAbsent(number, ProviderEpisode("$id:$href", "$id:$animeUrl", number, id, text.ifBlank { "Episode $number" }))
        }
        return result.values.sortedBy { it.number }
    }

    private fun episodeNumber(text: String, href: String): Int? =
        Regex("(?:episode|eps|ep|e)[^0-9]*(\\d+)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Regex("(?:episode|eps|ep)[^0-9]*(\\d+)", RegexOption.IGNORE_CASE).find(href)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Regex("(?:^|[^0-9])\\d+\\s*[xX]\\s*(\\d+)(?:[^0-9]|$)").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Regex("(?:^|[^0-9])\\d+\\s*[xX]\\s*(\\d+)(?:[^0-9]|$)").find(href)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Regex("(?:-|/)(\\d+)(?:/|$)").find(href)?.groupValues?.getOrNull(1)?.toIntOrNull()

    private fun imageUrl(image: Element): String? = image.absUrl("src").ifBlank { image.absUrl("data-src") }.ifBlank { null }
    private fun looksLikeAnimeUrl(url: String): Boolean {
        if (!url.trim().startsWith("http", true)) return false
        val hostMatches = runCatching { URI(url).host.equals(URI(baseUrl).host, ignoreCase = true) }.getOrDefault(false)
        if (!hostMatches) return false
        val path = runCatching { URI(url).path.lowercase() }.getOrDefault("/")
        if (path == "/" || path.isBlank()) return false
        val blocked = listOf("/wp-admin", "/wp-login", "/feed", "/category/", "/categories/", "/tag/", "/genre/", "/genres/", "/page/", "/author/", "/search/", "/contact", "/privacy", "/disclaimer", "/login", "/register", "/support", "/faq")
        if (blocked.any(path::contains)) return false
        return path.contains("/anime/") || path.contains("/series/") || path.contains("/title/") || path.contains("/episode") || path.contains("-anime") || path.split('/').lastOrNull()?.isNotBlank() == true
    }
    private fun slug(value: String): String = value.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    private companion object { const val UA = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/140.0.0.0 Mobile Safari/537.36" }
}
