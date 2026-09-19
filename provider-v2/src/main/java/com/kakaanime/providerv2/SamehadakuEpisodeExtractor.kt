package com.kakaanime.providerv2

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

/** Extracts Samehadaku episode pages into player/embed and direct media candidates. */
class SamehadakuEpisodeExtractor(
    acceptedHosts: Set<String> = DEFAULT_HOSTS,
    private val client: OkHttpClient = defaultClient(),
) : AniLabExtractor {
    override val id: String = "samehadaku-episode"

    private val hosts = acceptedHosts.map { it.lowercase().removePrefix("www.") }.toSet()

    override fun canHandle(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return false
        return host in hosts && uri.path.orEmpty().contains("episode", true)
    }

    override suspend fun extract(url: String, context: AniLabExtractionContext): List<AniLabStreamCandidate> =
        withContext(Dispatchers.IO) {
            val html = get(url, context) ?: return@withContext emptyList()
            val results = linkedMapOf<String, AniLabStreamCandidate>()
            collectDirect(html, url, "inline").forEach { results.putIfAbsent(it.url, it) }
            collectEmbeds(html, url, "player").forEach { results.putIfAbsent(it.url, it) }
            collectAjaxPlayers(html, url, context).forEach { results.putIfAbsent(it.url, it) }

            for (embed in results.values.filter { it.type == AniLabStreamType.UNKNOWN }.toList()) {
                val playerHtml = get(embed.url, context.copy(referer = url)) ?: continue
                collectDirect(playerHtml, embed.url, embed.serverId)
                    .forEach { results.putIfAbsent(it.url, it) }
                collectEmbeds(playerHtml, embed.url, embed.serverId)
                    .forEach { results.putIfAbsent(it.url, it) }
            }
            results.values.toList()
        }

    private fun collectDirect(html: String, base: String, server: String): List<AniLabStreamCandidate> =
        DIRECT_REGEX.findAll(html).mapNotNull { m ->
            val media = resolve(base, m.value) ?: return@mapNotNull null
            val type = mediaType(media)
            if (type == AniLabStreamType.UNKNOWN) return@mapNotNull null
            AniLabStreamCandidate("samehadaku", server, media, type, referer = base)
        }.distinctBy { it.url }.toList()

    private fun collectEmbeds(html: String, base: String, server: String): List<AniLabStreamCandidate> =
        EMBED_REGEX.findAll(html).mapNotNull { m ->
            val embed = resolve(base, decodeHtml(m.groupValues[1])) ?: return@mapNotNull null
            AniLabStreamCandidate("samehadaku", server, embed, referer = base)
        }.distinctBy { it.url }.toList()

    private suspend fun collectAjaxPlayers(
        html: String,
        pageUrl: String,
        context: AniLabExtractionContext,
    ): List<AniLabStreamCandidate> {
        val options = PLAYER_OPTION_TAG_REGEX.findAll(html).mapNotNull { match ->
            val tag = match.value
            val nume = attr(tag, "data-nume") ?: return@mapNotNull null
            val post = attr(tag, "data-post")
                ?: PLAYER_POST_REGEX.find(html)?.groupValues?.getOrNull(1)
                ?: return@mapNotNull null
            val type = attr(tag, "data-type").orEmpty().ifBlank { "schtml" }
            val label = attr(tag, "data-name").orEmpty()
            AjaxOption(post, nume, type, label)
        }.distinctBy { "\${it.post}|\${it.nume}|\${it.type}" }.toList()

        if (options.isEmpty()) return emptyList()

        val results = linkedMapOf<String, AniLabStreamCandidate>()
        for (option in options) {
            val responseHtml = postAjaxPlayer(option, pageUrl, context) ?: continue
            collectDirect(responseHtml, pageUrl, option.label.ifBlank { "player" })
                .forEach { results.putIfAbsent(it.url, it) }
            collectEmbeds(responseHtml, pageUrl, option.label.ifBlank { "player" })
                .forEach { results.putIfAbsent(it.url, it) }
        }
        return results.values.toList()
    }

    private suspend fun postAjaxPlayer(
        option: AjaxOption,
        referer: String,
        context: AniLabExtractionContext,
    ): String? = try {
        val form = FormBody.Builder()
            .add("action", "player_ajax")
            .add("post", option.post)
            .add("nume", option.nume)
            .add("type", option.type)
            .build()
        val request = Request.Builder()
            .url(AJAX_URL)
            .post(form)
            .header("User-Agent", context.userAgent ?: USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Referer", referer)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    } catch (t: CancellationException) {
        throw t
    } catch (_: Exception) {
        null
    }

    private suspend fun get(url: String, context: AniLabExtractionContext): String? = try {
        val b = Request.Builder()
            .url(url)
            .header("User-Agent", context.userAgent ?: USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
        context.referer?.takeIf(String::isNotBlank)?.let { b.header("Referer", it) }
        client.newCall(b.build()).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    } catch (t: CancellationException) {
        throw t
    } catch (_: Exception) {
        null
    }

    private fun attr(tag: String, name: String): String? =
        ATTR_REGEX(name).find(tag)?.groupValues?.getOrNull(1)?.let(::decodeHtml)

    private fun resolve(base: String, value: String): String? =
        runCatching { URI(base).resolve(value).toString() }.getOrNull()

    private fun mediaType(url: String): AniLabStreamType {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            path.endsWith(".m3u8") -> AniLabStreamType.HLS
            path.endsWith(".mpd") -> AniLabStreamType.DASH
            path.endsWith(".mp4") -> AniLabStreamType.MP4
            path.endsWith(".webm") -> AniLabStreamType.WEBM
            else -> AniLabStreamType.UNKNOWN
        }
    }

    private fun decodeHtml(value: String): String =
        value.replace("&amp;", "&").replace("&quot;", "\"").replace("&#039;", "'").replace("&#39;", "'")

    private data class AjaxOption(val post: String, val nume: String, val type: String, val label: String)

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"
        const val AJAX_URL = "https://v2.samehadaku.how/wp-admin/admin-ajax.php"
        val DEFAULT_HOSTS = setOf("v2.samehadaku.how", "samehadaku.how", "samehadaku.email")
        val PLAYER_OPTION_TAG_REGEX = Regex(
            "<[^>]*class=[\\\"'][^\\\"']*east_player_option[^\\\"']*[\\\"'][^>]*>",
            RegexOption.IGNORE_CASE,
        )
        val PLAYER_POST_REGEX = Regex(
            "data-post\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']",
            RegexOption.IGNORE_CASE,
        )
        val EMBED_REGEX = Regex(
            "<(?:iframe|embed)[^>]+(?:src|data-src)\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']",
            RegexOption.IGNORE_CASE,
        )
        val DIRECT_REGEX = Regex(
            "https?://[^\\s\\\"'<>]+\\.(?:m3u8|mpd|mp4|webm)(?:\\?[^\\s\\\"'<>]*)?",
            RegexOption.IGNORE_CASE,
        )
        fun ATTR_REGEX(name: String): Regex = Regex(
            "$name\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']",
            RegexOption.IGNORE_CASE,
        )
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .build()
    }
}
