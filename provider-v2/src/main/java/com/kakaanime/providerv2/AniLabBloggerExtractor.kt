package com.kakaanime.providerv2

import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

class AniLabBloggerExtractor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build(),
) : AniLabExtractor {
    override val id = "blogger"

    override fun canHandle(url: String): Boolean {
        val host = runCatching { URI(url).host?.lowercase() }.getOrNull() ?: return false
        return host == "blogspot.com" ||
            host.endsWith(".blogspot.com") ||
            host == "blogger.com" ||
            host.endsWith(".blogger.com")
    }

    override suspend fun extract(
        url: String,
        context: AniLabExtractionContext,
    ): List<AniLabStreamCandidate> {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/html,application/xhtml+xml,*/*")
            .apply {
                context.userAgent?.let { header("User-Agent", it) }
                context.referer?.let { header("Referer", it) }
            }
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val html = response.body?.string().orEmpty()
                MEDIA.findAll(html)
                    .mapNotNull { resolve(response.request.url.toString(), htmlDecode(it.groupValues[1])) }
                    .distinct()
                    .map { media ->
                        AniLabStreamCandidate(
                            providerId = "",
                            serverId = id,
                            url = media,
                            type = typeOf(media),
                            referer = response.request.url.toString(),
                            headers = context.headers,
                            cookies = context.cookies,
                        )
                    }
                    .toList()
            }
        } catch (e: CancellationException) {
            throw e
        }
    }

    private fun resolve(base: String, value: String): String? =
        runCatching { URI(base).resolve(value).toString() }
            .getOrNull()
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

    private fun htmlDecode(value: String) =
        value.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")

    private fun typeOf(url: String) = when {
        url.contains(".m3u8", true) -> AniLabStreamType.HLS
        url.contains(".mpd", true) -> AniLabStreamType.DASH
        url.contains(".webm", true) -> AniLabStreamType.WEBM
        url.contains(".mp4", true) -> AniLabStreamType.MP4
        else -> AniLabStreamType.UNKNOWN
    }

    companion object {
        private val MEDIA = Regex(
            """(?:src|data-src|data-video|href)=["']([^"']+\.(?:m3u8|mpd|mp4|webm)(?:\?[^"']*)?)["']""",
            RegexOption.IGNORE_CASE,
        )
    }
}
