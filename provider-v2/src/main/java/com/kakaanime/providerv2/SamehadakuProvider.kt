package com.kakaanime.providerv2

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
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
        val slug = slugify(normalized)
        val searchUrls = listOf(
            "$normalizedBaseUrl/?s=$encoded",
            "$normalizedBaseUrl/search/$slug/",
            "$normalizedBaseUrl/search/?q=$encoded",
        ).distinct()

        for (url in searchUrls) {
            val document = getDocument(url) ?: continue
            val results = document.extractSearchResults()
            if (results.isNotEmpty()) return results
        }
        return emptyList()
    }

    override suspend fun load(url: String): AniLabAnime? {
        val target = normalizeUrl(url) ?: return null
        val document = getDocument(target) ?: return null

        val title = document.selectFirst(
            "h1.entry-title, h1.title, h1",
        )?.text()?.trim().orEmpty().ifBlank {
            document.title().substringBefore(" - ").trim()
        }

        val episodes = document.select(
            "div.lstepsiode.listeps ul li, " +
                "div.listeps ul li, " +
                "div.episodelist ul li, " +
                "div.listing-chapters_wrap ul li",
        ).mapNotNull { item ->
            val anchor = item.selectFirst("span.lchx > a, a[href]") ?: return@mapNotNull null
            val href = anchor.absUrl("href").ifBlank { anchor.attr("href") }.trim()
            if (href.isBlank()) return@mapNotNull null

            val anchorTitle = anchor.text().trim().ifBlank { anchor.attr("title").trim() }
            val number = episodeNumber(anchorTitle, href)
                ?: item.selectFirst("span")?.text()?.let(::episodeNumber)
                ?: return@mapNotNull null

            AniLabEpisode(
                number = number,
                title = anchorTitle.ifBlank { "Episode $number" },
                url = href,
            )
        }.distinctBy { it.number }.sortedBy { it.number }

        if (title.isBlank() && episodes.isEmpty()) return null
        return AniLabAnime(
            id = target,
            title = title.ifBlank { "Unknown Anime" },
            url = target,
            providerId = id,
            episodes = episodes,
        )
    }

    override suspend fun loadLinks(episodeUrl: String): List<AniLabStreamCandidate> {
        val target = normalizeUrl(episodeUrl) ?: return emptyList()
        return listOf(
            AniLabStreamCandidate(
                providerId = id,
                serverId = "samehadaku-episode-page",
                url = target,
                referer = target,
            ),
        )
    }

    private suspend fun getDocument(url: String) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Referer", "$normalizedBaseUrl/")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    null
                } else {
                    response.body?.string()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { Jsoup.parse(it, response.request.url.toString()) }
                }
            }
        } catch (t: CancellationException) {
            throw t
        } catch (_: Exception) {
            null
        }
    }

    private fun org.jsoup.nodes.Document.extractSearchResults(): List<AniLabSearchResult> {
        val cards = select(
            "div.animepost, article.animpost, div.bs, div.bsx, " +
                "div.listupd .bs, div.listupd .bsx, article.bs, article.bsx",
        ).mapNotNull { card ->
            val link = card.selectFirst("div.title a, div.tt a, a[href*='/anime/'], a[href]")
                ?: return@mapNotNull null
            val href = link.absUrl("href").ifBlank { link.attr("href") }.trim()
            val title = card.selectFirst(
                "div.title h2, div.tt h4, .tt, .title, h2, h3, h4",
            )?.text()?.trim()
                ?: link.attr("title").trim().ifBlank { link.text().trim() }

            if (href.isBlank() || title.isBlank() || !href.contains("/anime/", true)) {
                null
            } else {
                AniLabSearchResult(
                    id = href,
                    title = title,
                    url = href,
                    providerId = id,
                )
            }
        }

        if (cards.isNotEmpty()) return cards.distinctBy { it.url }

        return select("a[href*='/anime/']").mapNotNull { anchor ->
            val href = anchor.absUrl("href").ifBlank { anchor.attr("href") }.trim()
            val title = anchor.selectFirst(".tt, .title, h2, h3, h4")?.text()?.trim()
                ?: anchor.attr("title").trim().ifBlank { anchor.text().trim() }

            if (href.isBlank() || title.isBlank()) {
                null
            } else {
                AniLabSearchResult(
                    id = href,
                    title = title,
                    url = href,
                    providerId = id,
                )
            }
        }.distinctBy { it.url }
    }

    private fun normalizeUrl(value: String): String? = runCatching {
        val raw = value.trim()
        if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) {
            URI(raw).toString()
        } else {
            URI(normalizedBaseUrl).resolve(raw.trimStart('/')).toString()
        }
    }.getOrNull()

    private fun episodeNumber(value: String, fallbackUrl: String = ""): Int? =
        EPISODE_REGEX.find("$value $fallbackUrl")
            ?.groupValues?.getOrNull(1)?.toIntOrNull()

    private fun slugify(value: String): String =
        value.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    private companion object {
        const val DEFAULT_BASE_URL = "https://v2.samehadaku.how"
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36"

        val EPISODE_REGEX = Regex(
            "(?:episode|eps|ep)[^0-9]*(\\d+)",
            RegexOption.IGNORE_CASE,
        )

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
