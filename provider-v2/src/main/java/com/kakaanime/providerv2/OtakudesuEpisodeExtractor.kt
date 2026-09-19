package com.kakaanime.providerv2

import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Resolves an Otakudesu episode page into direct media candidates and embed pages.
 *
 * Host-specific extractors can be added later without changing this page parser.
 */
class OtakudesuEpisodeExtractor(
    acceptedHosts: Set<String> = DEFAULT_HOSTS,
    private val client: OkHttpClient = defaultClient(),
) : AniLabExtractor {

    override val id: String = "otakudesu-episode"

    private val normalizedAcceptedHosts = acceptedHosts
        .map { it.lowercase().removePrefix("www.") }
        .toSet()

    override fun canHandle(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return false
        val path = uri.path.orEmpty()
        return host in normalizedAcceptedHosts &&
            (path.contains("/episode/", true) || path.contains("-episode-", true))
    }

    override suspend fun extract(
        url: String,
        context: AniLabExtractionContext,
    ): List<AniLabStreamCandidate> = withContext(Dispatchers.IO) {
        val html = get(url, context) ?: return@withContext emptyList()
        val results = linkedMapOf<String, AniLabStreamCandidate>()

        collectDirectMedia(html, url, "inline").forEach { results.putIfAbsent(it.url, it) }
        collectEmbeds(html, url, "embed").forEach { results.putIfAbsent(it.url, it) }
        resolveMirrorStream(url, html, context).forEach { results.putIfAbsent(it.url, it) }

        results.values.toList()
    }

    private suspend fun resolveMirrorStream(
        pageUrl: String,
        html: String,
        context: AniLabExtractionContext,
    ): List<AniLabStreamCandidate> {
        val origin = runCatching {
            URI(pageUrl).let { "${it.scheme}://${it.authority}" }
        }.getOrNull() ?: return emptyList()

        val script = SCRIPT_REGEX.findAll(html)
            .map { it.groupValues[1] }
            .firstOrNull { it.contains("window.__x__nonce", true) }
            ?: return emptyList()

        val nonceAction = NONCE_ACTION_REGEX.find(script)?.groupValues?.getOrNull(1)
            ?: return emptyList()

        val action = (
            ACTION_REGEX.find(script)?.groupValues?.getOrNull(1)
                ?: FALLBACK_ACTION_REGEX.find(script)?.groupValues?.getOrNull(1)
        ) ?: return emptyList()

        val ajaxReferer = "$origin/"
        val nonceRaw = postAjax(
            "$origin/wp-admin/admin-ajax.php",
            mapOf("action" to nonceAction),
            ajaxReferer,
            context,
        ) ?: return emptyList()

        val noncePayload = extractData(nonceRaw) ?: return emptyList()
        val nonce = decodeNonce(noncePayload) ?: return emptyList()

        val entries = DATA_CONTENT_REGEX.findAll(html)
            .map { decodeHtml(it.groupValues[1]) }
            .mapNotNull(::parseMirrorEntry)
            .distinctBy { "${it.id}|${it.mirror}|${it.quality}" }
            .toList()

        if (entries.isEmpty()) return emptyList()

        val results = linkedMapOf<String, AniLabStreamCandidate>()
        for (entry in entries) {
            val response = postAjax(
                "$origin/wp-admin/admin-ajax.php",
                mapOf(
                    "id" to entry.id,
                    "i" to entry.mirror,
                    "q" to entry.quality,
                    "nonce" to nonce,
                    "action" to action,
                ),
                ajaxReferer,
                context,
            ) ?: continue

            val payload = extractData(response)
            val decoded = payload?.let(::decodeBase64) ?: decodeBase64(response) ?: response

            collectDirectMedia(decoded, pageUrl, "mirror-${entry.quality}")
                .forEach { results.putIfAbsent(it.url, it) }
            collectEmbeds(decoded, pageUrl, "mirror-${entry.quality}")
                .forEach { results.putIfAbsent(it.url, it) }
        }

        return results.values.toList()
    }

    private suspend fun postAjax(
        endpoint: String,
        fields: Map<String, String>,
        referer: String,
        context: AniLabExtractionContext,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val form = FormBody.Builder()
                .apply { fields.forEach { (key, value) -> add(key, value) } }
                .build()

            val requestBuilder = Request.Builder()
                .url(endpoint)
                .post(form)
                .header("User-Agent", context.userAgent ?: USER_AGENT)
                .header("Referer", referer)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "*/*")

            if (context.cookies.isNotEmpty()) {
                requestBuilder.header(
                    "Cookie",
                    context.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" },
                )
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()?.trim()
            }
        } catch (t: CancellationException) {
            throw t
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun get(
        url: String,
        context: AniLabExtractionContext,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", context.userAgent ?: USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8")

            context.referer?.takeIf(String::isNotBlank)?.let {
                requestBuilder.header("Referer", it)
            }

            if (context.cookies.isNotEmpty()) {
                requestBuilder.header(
                    "Cookie",
                    context.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" },
                )
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()
            }
        } catch (t: CancellationException) {
            throw t
        } catch (_: Exception) {
            null
        }
    }

    private fun collectDirectMedia(
        html: String,
        baseUrl: String,
        serverId: String,
    ): List<AniLabStreamCandidate> {
        val urls = linkedSetOf<String>()

        DIRECT_URL_REGEX.findAll(html).forEach { urls += it.value }
        MEDIA_ATTRIBUTE_REGEX.findAll(html).forEach { match ->
            resolveUrl(baseUrl, decodeHtml(match.groupValues[1]))?.let { urls += it }
        }

        return urls.mapNotNull { mediaUrl ->
            val type = mediaType(mediaUrl)
            if (type == AniLabStreamType.UNKNOWN) return@mapNotNull null

            AniLabStreamCandidate(
                providerId = "otakudesu",
                serverId = serverId,
                url = mediaUrl,
                type = type,
                referer = baseUrl,
                headers = mapOf("User-Agent" to USER_AGENT, "Referer" to baseUrl),
            )
        }
    }

    private fun collectEmbeds(
        html: String,
        baseUrl: String,
        serverId: String,
    ): List<AniLabStreamCandidate> =
        EMBED_REGEX.findAll(html).mapNotNull { match ->
            resolveUrl(baseUrl, decodeHtml(match.groupValues[1]))?.let { embedUrl ->
                AniLabStreamCandidate(
                    providerId = "otakudesu",
                    serverId = serverId,
                    url = embedUrl,
                    type = AniLabStreamType.UNKNOWN,
                    referer = baseUrl,
                    headers = mapOf("User-Agent" to USER_AGENT, "Referer" to baseUrl),
                )
            }
        }.distinctBy { it.url }.toList()

    private fun parseMirrorEntry(raw: String): MirrorEntry? {
        val decoded = decodeBase64(raw)?.trim().orEmpty()
        if (decoded.isBlank()) return null

        val id = JSON_VALUE_REGEX("id").find(decoded)?.groupValues?.getOrNull(1) ?: return null
        val mirror = JSON_VALUE_REGEX("i").find(decoded)?.groupValues?.getOrNull(1) ?: return null
        val quality = JSON_VALUE_REGEX("q").find(decoded)?.groupValues?.getOrNull(1) ?: return null
        return MirrorEntry(id, mirror, quality)
    }

    private fun extractData(raw: String): String? =
        JSON_VALUE_REGEX("data").find(raw)?.groupValues?.getOrNull(1)

    private fun decodeBase64(value: String): String? =
        runCatching {
            String(Base64.getDecoder().decode(value.trim()), Charsets.UTF_8)
        }.getOrNull()

    private fun decodeNonce(value: String): String? {
        val raw = value.trim()
        if (raw.isBlank()) return null
        if (NONCE_REGEX.matches(raw)) return raw

        return decodeBase64(raw)
            ?.trim()
            ?.takeIf { NONCE_REGEX.matches(it) }
    }

    private fun resolveUrl(baseUrl: String, value: String): String? =
        runCatching { URI(baseUrl).resolve(value).toString() }.getOrNull()

    private fun decodeHtml(value: String): String =
        value.replace("&amp;", "&")
            .replace("&quot;", "\"")
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

    private data class MirrorEntry(
        val id: String,
        val mirror: String,
        val quality: String,
    )

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"

        val DEFAULT_HOSTS = setOf(
            "otakudesu.io",
            "otakudesu.fit",
            "otakudesu.blog",
            "otakudesu.id",
            "otakudesu.ro",
            "otakudesu.cloud",
            "otakudesu.my.id",
        )

        val SCRIPT_REGEX = Regex(
            "<script[^>]*>(.*?)</script>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        val NONCE_ACTION_REGEX = Regex(
            "data\\s*:\\s*\\{\\s*action\\s*:\\s*[\"']([a-f0-9]+)[\"']",
            RegexOption.IGNORE_CASE,
        )
        val ACTION_REGEX = Regex(
            "nonce\\s*:\\s*[^,]+,\\s*action\\s*:\\s*[\"']([a-f0-9]{16,64})[\"']",
            RegexOption.IGNORE_CASE,
        )
        val DATA_CONTENT_REGEX = Regex(
            "data-content\\s*=\\s*[\"']([^\"']+)[\"']",
            RegexOption.IGNORE_CASE,
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

        fun JSON_VALUE_REGEX(key: String) = Regex(
            "[\"']" + Regex.escape(key) + "[\"']\\s*:\\s*[\"']([^\"']+)[\"']",
            RegexOption.IGNORE_CASE,
        )

        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(35, TimeUnit.SECONDS)
                .build()
    }
}