package com.kakaanime.providerv2

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Native Samehadaku site adapter. Media resolution stays in SamehadakuEpisodeExtractor. */
class SamehadakuProvider(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val client: OkHttpClient = defaultClient(),
) : AniLabProvider {
    override val id: String = "samehadaku"
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    override suspend fun search(query: String): List<AniLabSearchResult> {
        val normalized = query.trim()
        if (normalized.isBlank()) return emptyList()
        val encoded = URLEncoder.encode(normalized, Charsets.UTF_8.name())
        val html = get("$normalizedBaseUrl/?s=$encoded") ?: return emptyList()
        return ANCHOR_REGEX.findAll(html).mapNotNull { m ->
            val href = decodeHtml(m.groupValues[1])
            val title = stripHtml(m.groupValues[2])
            val url = resolve(normalizedBaseUrl, href) ?: return@mapNotNull null
            if (!url.contains("/anime/", true) || title.isBlank()) return@mapNotNull null
            AniLabSearchResult(url, title, url, id)
        }.distinctBy { it.url }.toList()
    }

    override suspend fun load(url: String): AniLabAnime? {
        val target = normalizeUrl(url) ?: return null
        val html = get(target) ?: return null
        val title = firstMatch(html, "<h1[^>]*>(.*?)</h1>", "<title[^>]*>(.*?)</title>")?.let(::stripHtml).orEmpty()
        val episodes = ANCHOR_REGEX.findAll(html).mapNotNull { m ->
            val href = decodeHtml(m.groupValues[1])
            val anchorTitle = stripHtml(m.groupValues[2])
            val episodeUrl = resolve(target, href) ?: return@mapNotNull null
            if (!episodeUrl.contains("episode", true)) return@mapNotNull null
            val number = EPISODE_REGEX.find("$anchorTitle $episodeUrl")?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: return@mapNotNull null
            AniLabEpisode(number, anchorTitle.ifBlank { "Episode $number" }, episodeUrl)
        }.distinctBy { it.number }.sortedBy { it.number }.toList()
        if (title.isBlank() && episodes.isEmpty()) return null
        return AniLabAnime(target, title.ifBlank { "Unknown Anime" }, target, id, episodes)
    }

    override suspend fun loadLinks(episodeUrl: String): List<AniLabStreamCandidate> {
        val target = normalizeUrl(episodeUrl) ?: return emptyList()
        return listOf(AniLabStreamCandidate(id, "samehadaku-episode-page", target, referer = target))
    }

    private suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8").build()
            client.newCall(request).execute().use { response -> if (!response.isSuccessful) null else response.body?.string() }
        } catch (t: CancellationException) { throw t } catch (_: Exception) { null }
    }

    private fun normalizeUrl(value: String): String? = runCatching {
        val raw = value.trim()
        if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) URI(raw).toString()
        else URI(normalizedBaseUrl).resolve(raw.trimStart('/')).toString()
    }.getOrNull()

    private fun resolve(base: String, value: String): String? = runCatching { URI(base).resolve(value).toString() }.getOrNull()

    private companion object {
        const val DEFAULT_BASE_URL = "https://v2.samehadaku.how"
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"
        val ANCHOR_REGEX = Regex("<a\\b[^>]*\\bhref\\s*=\\s*[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a\\s*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val EPISODE_REGEX = Regex("(?:episode|eps|ep)[^0-9]*(\\d+)", RegexOption.IGNORE_CASE)
        fun firstMatch(html: String, vararg patterns: String): String? = patterns.firstNotNullOfOrNull { Regex(it, setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)?.groupValues?.getOrNull(1) }
        fun stripHtml(value: String): String = value.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
        fun decodeHtml(value: String): String = value.replace("&amp;", "&").replace("&quot;", "\"").replace("&#039;", "'").replace("&#39;", "'")
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
    }
}