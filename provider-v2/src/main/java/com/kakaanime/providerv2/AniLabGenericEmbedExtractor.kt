package com.kakaanime.providerv2

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

class AniLabGenericEmbedExtractor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build(),
) : AniLabExtractor {
    override val id = "generic-embed"

    override fun canHandle(url: String): Boolean {
        val host = runCatching { URI(url).host?.lowercase() }.getOrNull() ?: return false
        return HOSTS.any { host == it || host.endsWith(".$it") }
    }

    override suspend fun extract(
        url: String,
        context: AniLabExtractionContext,
    ): List<AniLabStreamCandidate> {
        val result = linkedMapOf<String, AniLabStreamCandidate>()
        val visited = mutableSetOf<String>()
        var current = url

        for (depth in 0 until MAX_DEPTH) {
            if (!visited.add(current)) break

            val request = Request.Builder()
                .url(current)
                .header("Accept", "text/html,application/xhtml+xml,*/*")
                .apply {
                    context.userAgent?.let { header("User-Agent", it) }
                    context.referer?.let { header("Referer", it) }
                }
                .build()

            val next = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null

                val html = response.body?.string().orEmpty()

                MEDIA.findAll(html).forEach { match ->
                    val media = resolve(current, htmlDecode(match.groupValues[1])) ?: return@forEach
                    result.putIfAbsent(
                        media,
                        AniLabStreamCandidate(
                            providerId = "",
                            serverId = id,
                            url = media,
                            type = typeOf(media),
                            referer = current,
                            headers = context.headers,
                            cookies = context.cookies,
                        ),
                    )
                }

                EMBED.find(html)
                    ?.let { match ->
                        match.groupValues
                            .drop(1)
                            .firstOrNull { it.isNotBlank() }
                    }
                    ?.let { resolve(current, htmlDecode(it)) }
                    ?.takeIf { it != current && it !in visited }
            }

            if (next == null) break
            current = next
        }

        return result.values.toList()
    }

    private fun resolve(base: String, value: String): String? =
        runCatching { URI(base).resolve(value).toString() }
            .getOrNull()
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

    private fun typeOf(url: String) = when {
        url.contains(".m3u8", true) -> AniLabStreamType.HLS
        url.contains(".mpd", true) -> AniLabStreamType.DASH
        url.contains(".webm", true) -> AniLabStreamType.WEBM
        url.contains(".mp4", true) -> AniLabStreamType.MP4
        else -> AniLabStreamType.UNKNOWN
    }

    private fun htmlDecode(s: String) =
        s.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")

    companion object {
        private const val MAX_DEPTH = 3

        private val HOSTS = setOf(
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

        private val MEDIA = Regex(
            """(?:data-src|data-video|src|href)=["']([^"']+\.(?:m3u8|mpd|mp4|webm)(?:\?[^"']*)?)["']""",
            RegexOption.IGNORE_CASE,
        )

        private val EMBED = Regex(
            """<iframe[^>]+(?:data-src|src)=["']([^"']+)["']|(?:data-embed)=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        )
    }
}
