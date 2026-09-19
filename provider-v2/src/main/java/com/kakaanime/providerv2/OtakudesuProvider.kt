package com.kakaanime.providerv2

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Native Otakudesu site adapter.
 *
 * Search/detail/episode discovery stays here; media extraction lives in the
 * dedicated OtakudesuEpisodeExtractor.
 */
class OtakudesuProvider(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val client: OkHttpClient = defaultClient(),
) : AniLabProvider {

    override val id: String = "otakudesu"

    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    override suspend fun search(query: String): List<AniLabSearchResult> {
        val normalized = query.trim()
        if (normalized.isBlank()) return emptyList()

        val encoded = URLEncoder.encode(normalized, Charsets.UTF_8.name())
        val html = get("$normalizedBaseUrl/?s=$encoded&post_type=anime") ?: return emptyList()

        return parseAnchors(html)
            .filter { it.url.contains("/anime/", true) || it.url.contains("/series/", true) }
            .distinctBy { it.url }
            .map {
                AniLabSearchResult(
                    id = it.url,
                    title = it.title.ifBlank { "Unknown Anime" },
                    url = it.url,
                    providerId = id,
                )
            }
    }

    override suspend fun load(url: String): AniLabAnime? {
        val target = normalizeUrl(url) ?: return null
        val html = get(target) ?: return null

        val title = firstMatch(
            html,
            "<h1[^>]*>(.*?)</h1>",
            "<title[^>]*>(.*?)</title>",
        )?.stripHtml()?.removeSuffix(" - Otaku Desu")?.trim().orEmpty()

        val episodes = parseAnchors(html)
            .asSequence()
            .filter { it.url.contains("/episode/", true) }
            .mapNotNull {
                val number = extractEpisodeNumber(it.title, it.url) ?: return@mapNotNull null
                AniLabEpisode(
                    number = number,
                    title = it.title.ifBlank { "Episode $number" },
                    url = it.url,
                )
            }
            .distinctBy { it.number }
            .sortedBy { it.number }
            .toList()

        if (title.isBlank() && episodes.isEmpty()) return null

        return AniLabAnime(
            id = target,
            title = title.ifBlank { target.substringAfterLast('/').ifBlank { "Unknown Anime" } },
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
                serverId = "otakudesu-episode-page",
                url = target,
                type = AniLabStreamType.UNKNOWN,
                referer = target,
            ),
        )
    }

    private suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()
            }
        } catch (t: CancellationException) {
            throw t
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeUrl(value: String): String? =
        runCatching {
            val raw = value.trim()
            if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) {
                URI(raw).toString()
            } else {
                URI(normalizedBaseUrl).resolve(raw.trimStart('/')).toString()
            }
        }.getOrNull()

    private fun parseAnchors(html: String): List<Anchor> =
        ANCHOR_REGEX.findAll(html).mapNotNull { match ->
            val href = decodeHtml(match.groupValues[1].trim())
            val title = match.groupValues[2].stripHtml()
            val url = runCatching { URI(normalizedBaseUrl).resolve(href).toString() }.getOrNull()
                ?: return@mapNotNull null
            if (!url.startsWith("http", true)) return@mapNotNull null
            Anchor(url, title)
        }.toList()

    private fun firstMatch(html: String, vararg patterns: String): String? =
        patterns.firstNotNullOfOrNull { pattern ->
            Regex(pattern, setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(html)?.groupValues?.getOrNull(1)
        }

    private fun extractEpisodeNumber(title: String, url: String): Int? =
        Regex("(?:episode|eps|ep)[^0-9]*(\\d+)", RegexOption.IGNORE_CASE)
            .find("$title $url")
            ?.groupValues?.getOrNull(1)
            ?.toIntOrNull()

    private fun decodeHtml(value: String): String =
        value.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

    private fun String.stripHtml(): String =
        replace(Regex("<[^>]+>"), " ")
            .let(::decodeHtml)
            .replace(Regex("\\s+"), " ")
            .trim()

    private data class Anchor(val url: String, val title: String)

    private companion object {
        const val DEFAULT_BASE_URL = "https://otakudesu.blog"
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"
        val ANCHOR_REGEX = Regex(
            "<a\\b[^>]*\\bhref\\s*=\\s*[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a\\s*>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )

        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build()
    }
}