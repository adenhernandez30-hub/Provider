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

/** Kuramanime-specific adapter. Its episode page uses a tokenized server switcher. */
class KuramanimeProvider(browserResolver: BrowserStreamResolver? = null) : AnimeProvider {
    override val id = "kuramanime"
    override val name = "Kuramanime"
    override val priority = 210
    private val baseUrl = "https://v20.kuramanime.ing"
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).followRedirects(true).build()
    private val resolver = StreamResolver(ExtractorRegistry(browserResolver = browserResolver), browserResolver = browserResolver)

    override suspend fun search(query: String): List<ProviderAnime> {
        val encoded = encode(query)
        val searchUrls = listOf(
            "$baseUrl/anime?search=$encoded&page=1",
            "$baseUrl/anime?keyword=$encoded&page=1",
            "$baseUrl/search?keyword=$encoded",
            "$baseUrl/search?q=$encoded"
        ).distinct()

        for (url in searchUrls) {
            val doc = getDocument(url, baseUrl) ?: continue
            val results = doc.select(
                "div.filter__gallery > a, a[href*='/anime/'], " +
                    "a[href*='/anime/'][title], article a[href*='/anime/']"
            ).mapNotNull { a ->
                val href = a.absUrl("href").ifBlank { a.attr("href") }
                val title = a.selectFirst("h5, .title, .name")?.text()?.trim()
                    ?: a.attr("title").trim().ifBlank { a.text().trim() }
                if (href.isBlank() || title.isBlank() || !href.contains("/anime/", true)) null
                else ProviderAnime(
                    "$id:$href",
                    title,
                    id,
                    posterUrl = a.selectFirst("img")?.let {
                        it.absUrl("src").ifBlank { it.absUrl("data-src") }
                    }?.ifBlank { null }
                )
            }.distinctBy { it.id }.take(30)
            if (results.isNotEmpty()) return results
        }
        return emptyList()
    }

    override suspend fun getAnime(animeId: String): ProviderAnime? {
        val url = normalizeAnimeUrl(animeId); val doc = getDocument(url, baseUrl) ?: return null
        val details = doc.selectFirst("div.anime__details__text") ?: return null
        val title = details.selectFirst("div > h3")?.text()?.removePrefix("Judul: ")?.trim().orEmpty()
        if (title.isBlank()) return null
        val info = details.selectFirst("div.anime__details__widget")
        return ProviderAnime("$id:$url", title, id, posterUrl = doc.selectFirst("div.anime__details__pic")?.attr("data-setbg"), description = details.selectFirst("p#synopsisField")?.text().orEmpty(), genres = info?.select("li:contains(Genre:) > a, li:contains(Tema:) > a, li:contains(Demografis:) > a")?.eachText().orEmpty(), status = info?.selectFirst("li:contains(Status:) > a")?.text().orEmpty().ifBlank { "UNKNOWN" })
    }

    override suspend fun getEpisodes(animeId: String): List<ProviderEpisode> {
        val animeUrl = normalizeAnimeUrl(animeId); val doc = getDocument(animeUrl, baseUrl) ?: return emptyList()
        val html = doc.selectFirst("a#episodeLists")?.attr("data-content").orEmpty(); if (html.isBlank()) return emptyList()
        val episodeDoc = Jsoup.parse(html); val limits = episodeDoc.select("a.btn-secondary")
        return if (limits.isEmpty()) {
            episodeDoc.select("a").filterNot { it.attr("href").contains("batch", true) }.mapNotNull { element ->
                val href = element.absUrl("href").ifBlank { java.net.URI(animeUrl).resolve(element.attr("href")).toString() }
                val number = element.text().filter(Char::isDigit).toIntOrNull() ?: return@mapNotNull null
                ProviderEpisode("$id:$href", "$id:$animeUrl", number, id, element.text())
            }.reversed().distinctBy { it.number }
        } else {
            val values = limits.eachText().take(2).mapNotNull { Regex("\\d+").find(it)?.value?.toIntOrNull() }
            if (values.size < 2) return emptyList()
            (values[1] downTo values[0]).map { number ->
                val href = animeUrl + "/episode/" + number
                ProviderEpisode("$id:$href", "$id:$animeUrl", number, id, "Episode $number")
            }
        }
    }

    override suspend fun getStreams(animeId: String, episodeNumber: Int): List<ProviderStream> {
        if (episodeNumber < 1) return emptyList()
        val animeUrl = normalizeAnimeUrl(animeId); val episodeUrl = animeUrl + "/episode/" + episodeNumber
        val doc = getDocument(episodeUrl, animeUrl) ?: return emptyList()
        val pageCandidates = extractEpisodeCandidates(doc)
        suspend fun fallbackStreams(): List<ProviderStream> {
            val candidates = (listOf(episodeUrl) + pageCandidates).distinct()
            return resolver.resolve(candidates, referer = episodeUrl).map { it.copy(providerId = id) }
        }
        val csrf = doc.selectFirst("meta[name=csrf-token]")?.attr("content").orEmpty().ifBlank { doc.selectFirst("meta[name=csrf-token]")?.attr("csrf-token").orEmpty() }
        val scriptName = doc.selectFirst("[data-kps]")?.attr("data-kps").orEmpty()
        if (csrf.isBlank() || scriptName.isBlank()) return fallbackStreams()
        val env = getScriptEnv(scriptName) ?: return fallbackStreams()
        val authPath = buildAuthPath(env)
        val tokenId = env["MIX_AUTH_KEY"].orEmpty() + ":" + env["MIX_AUTH_TOKEN"].orEmpty()
        val tokenParam = env["MIX_PAGE_TOKEN_KEY"].orEmpty(); val serverParam = env["MIX_STREAM_SERVER_KEY"].orEmpty()
        if (authPath.isBlank() || tokenId == ":" || tokenParam.isBlank() || serverParam.isBlank()) return fallbackStreams()
        val headers = mapOf("User-Agent" to UA, "Referer" to episodeUrl, "X-Requested-With" to "XMLHttpRequest", "X-CSRF-TOKEN" to csrf, "X-Fuck-ID" to tokenId, "X-Request-ID" to randomToken(), "X-Request-Index" to "0")
        val hash = requestText(baseUrl + "/" + authPath, episodeUrl, headers)?.trim('"').orEmpty(); if (hash.isBlank()) return fallbackStreams()
        val supported = setOf("kuramadrive", "kuramadrive-v2", "filelions", "filemoon", "mega", "streamwish", "streamtape", "vidguard")
        val servers = doc.select("select#changeServer > option")
            .map { it.attr("value") to it.text().substringBefore(" (") }
            .filter { it.first in supported }
            .sortedBy { if (it.first == "kuramadrive" || it.first == "kuramadrive-v2") 0 else 1 }
        for ((server, _) in servers) {
            val playerUrl = episodeUrl + "?" + tokenParam + "=" + encode(hash) + "&" + serverParam + "=" + encode(server)
            val playerDoc = getDocument(playerUrl, episodeUrl) ?: continue
            val iframe = playerDoc.selectFirst("div.video-content iframe")?.absUrl("src").orEmpty()
            val direct = extractEpisodeCandidates(playerDoc)
            val candidates = (listOf(iframe) + direct).filter { it.startsWith("http", true) }.distinct(); if (candidates.isEmpty()) continue
            val streams = resolver.resolve(candidates, referer = episodeUrl); if (streams.isNotEmpty()) return streams.map { it.copy(providerId = id) }
        }
        return fallbackStreams()
    }

    private suspend fun getScriptEnv(scriptName: String): Map<String, String>? {
        val normalized = normalizeScriptAsset(scriptName) ?: return null
        val js = requestText(normalized, baseUrl) ?: return null
        val block = Regex("""window\.process\s*=\s*\{[\s\S]*?env:\s*\{([\s\S]*?)\}[\s\S]*?\}""").find(js)?.groupValues?.getOrNull(1) ?: return null
        return Regex("""(\w+):\s*['"]([^'"]+)['"]""").findAll(block).associate { it.groupValues[1] to it.groupValues[2] }
    }
    internal fun buildAuthPath(env: Map<String, String>): String = listOf(
        env["MIX_PREFIX_AUTH_ROUTE_PARAM"].orEmpty().trim('/'),
        env["MIX_AUTH_ROUTE_PARAM"].orEmpty().trim('/')
    ).filter { it.isNotBlank() }.joinToString("/")

    internal fun extractEpisodeCandidates(doc: Document): List<String> = buildList {
        doc.select("div.video-content iframe[src], iframe[src], iframe[data-src]").forEach {
            add(it.absUrl("src").ifBlank { it.attr("src") }.ifBlank { it.absUrl("data-src") }.ifBlank { it.attr("data-src") })
        }
        doc.select("video#player > source[src], video source[src], video source[data-src], video[src], video[data-src], source[src], source[data-src]").forEach {
            add(it.absUrl("src").ifBlank { it.attr("src") }.ifBlank { it.absUrl("data-src") }.ifBlank { it.attr("data-src") })
        }
    }.filter { it.startsWith("http", true) }.distinct()

    private fun normalizeScriptAsset(scriptName: String): String? {
        val raw = scriptName.trim()
        if (raw.isBlank()) return null
        if (raw.startsWith("http", true)) return raw
        val withExt = if (raw.contains(".js")) raw else "$raw.js"
        return if (withExt.startsWith("/")) "$baseUrl$withExt" else "$baseUrl/assets/js/$withExt"
    }
    private suspend fun getDocument(url: String, referer: String): Document? = withContext(Dispatchers.IO) {
        runCatching { val request = Request.Builder().url(url).header("User-Agent", UA).header("Accept", "text/html,application/xhtml+xml").header("Referer", referer).build()
            client.newCall(request).execute().use { response -> if (!response.isSuccessful) null else response.body?.string()?.takeIf { it.isNotBlank() }?.let { Jsoup.parse(it, url) } } }.getOrNull()
    }
    private suspend fun requestText(url: String, referer: String, extra: Map<String, String> = emptyMap()): String? = withContext(Dispatchers.IO) {
        runCatching { val builder = Request.Builder().url(url).header("User-Agent", UA).header("Referer", referer); extra.forEach { (k,v) -> builder.header(k,v) }
            client.newCall(builder.build()).execute().use { response -> if (!response.isSuccessful) null else response.body?.string() } }.getOrNull()
    }
    private fun normalizeAnimeUrl(value: String): String { val raw = value.removePrefix("$id:").trim(); return if (raw.startsWith("http", true)) raw.trimEnd('/') else baseUrl + "/" + raw.trim('/') }
    private fun encode(value: String) = java.net.URLEncoder.encode(value, "UTF-8")
    private fun randomToken(length: Int = 8) = (1..length).map { ("abcdefghijklmnopqrstuvwxyz0123456789").random() }.joinToString("")
    companion object { private const val UA = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/140.0.0.0 Mobile Safari/537.36" }
}
