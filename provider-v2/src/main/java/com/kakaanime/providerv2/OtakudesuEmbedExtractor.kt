package com.kakaanime.providerv2

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Lightweight host extractor for the embeds currently returned by Otakudesu.
 *
 * It handles direct media, common video/source attributes, FileDon's data-page
 * payload, and one or two nested iframe levels. Dedicated host extractors can
 * replace individual branches later without changing the provider contract.
 */
class OtakudesuEmbedExtractor(
    acceptedHosts: Set<String> = DEFAULT_HOSTS,
    private val client: OkHttpClient = defaultClient(),
) : AniLabExtractor {

    override val id: String = "otakudesu-embed"

    private val normalizedAcceptedHosts = acceptedHosts
        .map { it.lowercase().removePrefix("www.") }
        .toSet()

    override fun canHandle(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return false
        return normalizedAcceptedHosts.any { host == it || host.contains(it) } && !isDirectMedia(url)
    }

    override suspend fun extract(
        url: String,
        context: AniLabExtractionContext,
    ): List<AniLabStreamCandidate> =
        withContext(Dispatchers.IO) {
            resolvePage(
                url = url,
                context = context,
                depth = 0,
                visited = linkedSetOf(),
            )
        }

    private suspend fun resolvePage(
        url: String,
        context: AniLabExtractionContext,
        depth: Int,
        visited: MutableSet<String>,
    ): List<AniLabStreamCandidate> {
        if (depth > MAX_DEPTH || !visited.add(url)) return emptyList()

        if (isDirectMedia(url)) {
            return listOf(candidate(url, "direct", context.referer ?: url))
        }

        val html = get(url, context) ?: return emptyList()
        val results = linkedMapOf<String, AniLabStreamCandidate>()

        collectMedia(html, url).forEach { results.putIfAbsent(it.url, it) }

        if (depth < MAX_DEPTH) {
            for (embed in collectEmbeds(html, url)) {
                val host = runCatching { URI(embed).host?.lowercase()?.removePrefix("www.") }.getOrNull()
                if (host == null || normalizedAcceptedHosts.none { host == it || host.contains(it) }) continue

                resolvePage(
                    url = embed,
                    context = context.copy(referer = url),
                    depth = depth + 1,
                    visited = visited,
                ).forEach { results.putIfAbsent(it.url, it) }
            }
        }

        return results.values.toList()
    }

    private suspend fun get(
        url: String,
        context: AniLabExtractionContext,
    ): String? = try {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", context.userAgent ?: USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8")

        context.referer?.takeIf(String::isNotBlank)?.let {
            requestBuilder.header("Referer", it)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    } catch (t: CancellationException) {
        throw t
    } catch (_: Exception) {
        null
    }

    private fun collectMedia(
        html: String,
        baseUrl: String,
    ): List<AniLabStreamCandidate> {
        val urls = linkedSetOf<String>()

        DIRECT_URL_REGEX.findAll(html).forEach { urls += it.value }
        MEDIA_ATTRIBUTE_REGEX.findAll(html).forEach { match ->
            resolveUrl(baseUrl, decodeHtml(match.groupValues[1]))?.let { urls += it }
        }

        DATA_PAGE_REGEX.findAll(html).forEach { match ->
            val raw = decodeHtml(match.groupValues[1])
            JSON_URL_REGEX.find(raw)?.groupValues?.getOrNull(1)?.let {
                resolveUrl(baseUrl, decodeHtml(it))?.let(urls::add)
            }
        }

        return urls.mapNotNull { mediaUrl ->
            val type = mediaType(mediaUrl)
            if (type == AniLabStreamType.UNKNOWN) return@mapNotNull null
            candidate(mediaUrl, "embed", baseUrl).copy(type = type)
        }
    }

    private fun collectEmbeds(
        html: String,
        baseUrl: String,
    ): List<String> =
        EMBED_REGEX.findAll(html)
            .mapNotNull { resolveUrl(baseUrl, decodeHtml(it.groupValues[1])) }
            .filterNot(::isDirectMedia)
            .distinct()
            .toList()

    private fun candidate(
        url: String,
        serverId: String,
        referer: String,
    ): AniLabStreamCandidate =
        AniLabStreamCandidate(
            providerId = "otakudesu",
            serverId = serverId,
            url = url,
            type = mediaType(url),
            referer = referer,
            headers = mapOf("User-Agent" to USER_AGENT, "Referer" to referer),
        )

    private fun resolveUrl(baseUrl: String, value: String): String? =
        runCatching { URI(baseUrl).resolve(value) }.getOrNull()?.toString()

    private fun decodeHtml(value: String): String =
        value.replace("&amp;", "&")
            .replace("&quot;", """)
            .replace("&#039;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

    private fun mediaType(url: String): AniLabStreamType {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            path.endsWith(".m3u8") -> AniLabStreamType.HLS
            path.endsWith(".mpd") -> AniLabStreamType.DASH
            path.endsWith(".webm") -> AniLabStreamType.WEBM
            path.endsWith(".mp4") || path.endsWith(".mkv") -> AniLabStreamType.MP4
            else -> AniLabStreamType.UNKNOWN
        }
    }

    private fun isDirectMedia(url: String): Boolean =
        mediaType(url) != AniLabStreamType.UNKNOWN

    private companion object {
        const val MAX_DEPTH = 2
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"

        val DEFAULT_HOSTS = setOf(
            "filedon",
            "uservideo",
            "userdrive",
            "samevideo",
            "vidhide",
            "blogger",
            "blogspot",
            "mp4upload",
            "yourupload",
            "yuplod",
            "streamwish",
            "filelions",
            "desustream",
            "desudrive",
            "odstream",
            "odcdn",
            "otakuwatch",
        )

        val EMBED_REGEX = Regex(
            "<(?:iframe|embed)[^>]+(?:src|data-src)\\s*=\\s*[\"']([^\"']+)[\"']",
            RegexOption.IGNORE_CASE,
        )
        val MEDIA_ATTRIBUTE_REGEX = Regex(
            "(?:file|src|source|hls|m3u8|mpd|videoUrl|video_url|playlist)\\s*[=:]\\s*[\"']([^\"']+)[\"']",
            RegexOption.IGNORE_CASE,
        )
        val DIRECT_URL_REGEX = Regex(
            "https?://[^\\s\"'<>]+\\.(?:m3u8|mpd|mp4|mkv|webm)(?:\\?[^\\s\"'<>]*)?",
            RegexOption.IGNORE_CASE,
        )
        val DATA_PAGE_REGEX = Regex(
            "<div[^>]+id=[\"']app[\"'][^>]+data-page=[\"']([^\"']+)[\"']",
            RegexOption.IGNORE_CASE,
        )
        val JSON_URL_REGEX = Regex(
            "[\"']url[\"']\\s*:\\s*[\"']([^\"']+)[\"']",
            RegexOption.IGNORE_CASE,
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
