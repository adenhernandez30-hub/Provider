package com.kakaanime.providerv2

import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Reusable HTML player extractor for common external hosts used by Indonesian
 * anime sites. It intentionally only extracts media/embed URLs; host-specific
 * token/signature flows belong in dedicated extractors.
 */
private fun defaultGenericClient(): OkHttpClient = OkHttpClient.Builder()\n    .followRedirects(true)\n    .followSslRedirects(true)\n    .connectTimeout(8, TimeUnit.SECONDS)\n    .readTimeout(15, TimeUnit.SECONDS)\n    .callTimeout(20, TimeUnit.SECONDS)\n    .build()\n\nclass AniLabGenericEmbedExtractor(
    private val client: OkHttpClient = defaultGenericClient(),
) : AniLabExtractor {

    override val id: String = "generic-embed"

    override fun canHandle(url: String): Boolean {
        val host = hostOf(url) ?: return false
        return HOST_TOKENS.any { token -> host == token || host.endsWith(".$token") || host.contains(token) }
    }

    override suspend fun extract(
        url: String,
        context: AniLabExtractionContext,
    ): List<AniLabStreamCandidate> {
        val visited = linkedSetOf<String>()
        val results = linkedMapOf<String, AniLabStreamCandidate>()
        var current = url
        var depth = 0

        while (depth <= MAX_DEPTH && visited.add(current)) {
            val request = Request.Builder()
                .url(current)
                .header("Accept", "text/html,application/xhtml+xml,application/json,*/*")
                .apply {
                    context.userAgent?.takeIf { it.isNotBlank() }?.let { header("User-Agent", it) }
                    context.referer?.takeIf { it.isNotBlank() }?.let { header("Referer", it) }
                    context.headers.forEach { (key, value) ->
                        if (!key.equals("Referer", true) && !key.equals("Cookie", true)) {
                            header(key, value)
                        }
                    }
                    if (context.cookies.isNotEmpty()) {
                        header("Cookie", context.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
                    }
                }
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                throw t
            }

            response.use {
                if (!it.isSuccessful) return@use
                val html = it.body?.string().orEmpty()
                collectMedia(html, current, context, results)

                val next = findEmbed(html, current)
                if (next != null && next != current) {
                    current = next
                    depth++
                } else {
                    depth = MAX_DEPTH + 1
                }
            }
        }

        return results.values.toList()
    }

    private fun collectMedia(
        html: String,
        pageUrl: String,
        context: AniLabExtractionContext,
        results: MutableMap<String, AniLabStreamCandidate>,
    ) {
        MEDIA_REGEX.findAll(html).forEach { match ->
            val raw = match.groupValues[1]
            val mediaUrl = absolutize(pageUrl, decodeHtml(raw)) ?: return@forEach
            val type = when {
                mediaUrl.contains(".m3u8", true) -> AniLabStreamType.HLS
                mediaUrl.contains(".mpd", true) -> AniLabStreamType.DASH
                mediaUrl.contains(".webm", true) -> AniLabStreamType.WEBM
                mediaUrl.contains(".mp4", true) -> AniLabStreamType.MP4
                else -> AniLabStreamType.UNKNOWN
            }
            results.putIfAbsent(
                mediaUrl,
                AniLabStreamCandidate(
                    providerId = "",
                    serverId = id,
                    url = mediaUrl,
                    type = type,
                    referer = pageUrl.ifBlank { context.referer },
                    headers = context.headers,
                    cookies = context.cookies,
                ),
            )
        }
    }

    private fun findEmbed(html: String, pageUrl: String): String? {
        val match = EMBED_REGEX.find(html) ?: return null
        return absolutize(pageUrl, decodeHtml(match.groupValues[1]))
    }

    private fun hostOf(url: String): String? =
        runCatching { java.net.URI(url).host?.lowercase() }.getOrNull()

    private fun absolutize(base: String, value: String): String? =
        runCatching { java.net.URI(base).resolve(value).toString() }.getOrNull()
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

    private fun decodeHtml(value: String): String =
        value.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

    companion object {
        private const val MAX_DEPTH = 2

        private val HOST_TOKENS = setOf(
            "blogspot.com",
            "blogger.com",
            "filedon.co",
            "filedon.to",
            "uservideo.net",
            "userdrive.co",
            "samevideo.to",
            "vidhide.com",
            "mp4upload.com",
            "yourupload.com",
            "yuplod.net",
            "streamwish.to",
            "filelions.to",
            "desustream.info",
            "desudrive.com",
            "odstream.xyz",
            "odcdn.com",
            "otakuwatch.com",
        )

        private val MEDIA_REGEX = Regex(
            """(?:data-src|data-video|src|href)=["']([^"']+.(?:m3u8|mpd|mp4|webm)(?:\?[^"']*)?)["']""",
            RegexOption.IGNORE_CASE,
        )

        private val EMBED_REGEX = Regex(
            """(?:data-src|data-embed|src)=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        )

        private val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .build()
        }
    }
}
