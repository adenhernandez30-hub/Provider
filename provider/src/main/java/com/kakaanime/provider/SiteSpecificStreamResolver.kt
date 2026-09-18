package com.kakaanime.provider

import com.kakaanime.provider.extractor.StreamResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.Base64

/**
 * Site-specific stream discovery for sources whose player links are not exposed
 * as a plain iframe on the episode page.
 *
 * The provider-specific adapter owns the site's handshake; StreamResolver still
 * owns final media extraction/validation.
 */
object SiteSpecificStreamResolver {
    private const val UA = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/140.0.0.0 Mobile Safari/537.36"

    suspend fun otakudesu(
        client: OkHttpClient,
        resolver: StreamResolver,
        episodeUrl: String,
    ): List<ProviderStream> {
        val doc = getDocument(client, episodeUrl, episodeUrl) ?: return emptyList()
        val script = doc.selectFirst("script:containsData({action:)")?.data().orEmpty()
        if (script.isBlank()) return resolvePage(client, resolver, episodeUrl, episodeUrl)

        val nonceAction = script.substringAfter("{action:\"", "").substringBefore("\"")
            .ifBlank { script.substringAfter("action:\"", "").substringBefore("\"") }
        val action = script.substringAfter("action:\"", "").substringBefore("\"")
        if (nonceAction.isBlank() || action.isBlank()) return resolvePage(client, resolver, episodeUrl, episodeUrl)

        val nonce = post(client, "$episodeUrl".let { URI(it).let { u -> "${u.scheme}://${u.host}/wp-admin/admin-ajax.php" } },
            FormBody.Builder().add("action", nonceAction).build(), episodeUrl)
            ?.let(::extractAjaxValue).orEmpty()

        val mirrors = doc.select("div.mirrorstream ul li > a")
        for (mirror in mirrors) {
            val encoded = mirror.attr("data-content").trim()
            if (encoded.isBlank() || nonce.isBlank()) continue
            val decoded = decodeBase64(encoded).drop(1).dropLast(1)
            val fields = decoded.split(",").associate {
                val key = it.substringBefore(":").replace("\"", "").trim()
                val value = it.substringAfter(":", "").replace("\"", "").trim()
                key to value
            }
            val id = fields["id"].orEmpty()
            val index = fields["i"].orEmpty()
            val quality = fields["q"].orEmpty()
            if (id.isBlank() || index.isBlank()) continue
            val origin = URI(episodeUrl)
            val ajaxUrl = "${origin.scheme}://${origin.host}/wp-admin/admin-ajax.php"
            val body = FormBody.Builder()
                .add("id", id)
                .add("i", index)
                .add("q", quality)
                .add("nonce", nonce)
                .add("action", action)
                .build()
            val iframe = post(client, ajaxUrl, body, episodeUrl)
                ?.let(::extractAjaxValue)
                ?.let(::decodeBase64)
                ?.let { Jsoup.parse(it).selectFirst("iframe")?.absUrl("src").orEmpty() }
                .orEmpty()
            if (iframe.isBlank()) continue
            val resolved = resolvePage(client, resolver, iframe, episodeUrl)
            if (resolved.isNotEmpty()) return resolved
        }
        return emptyList()
    }

    suspend fun samehadaku(
        client: OkHttpClient,
        resolver: StreamResolver,
        episodeUrl: String,
    ): List<ProviderStream> {
        val doc = getDocument(client, episodeUrl, episodeUrl) ?: return emptyList()
        val origin = URI(episodeUrl)
        val ajaxUrl = "${origin.scheme}://${origin.host}/wp-admin/admin-ajax.php"
        val servers = doc.select("#server > ul > li > div")
        for (server in servers) {
            val body = FormBody.Builder()
                .add("action", "player_ajax")
                .add("post", server.attr("data-post"))
                .add("nume", server.attr("data-nume"))
                .add("type", server.attr("data-type"))
                .build()
            val iframe = post(client, ajaxUrl, body, episodeUrl)
                ?.let { Regex("""src\\s*=\\s*[\"']([^\"']+)[\"']""").find(it)?.groupValues?.getOrNull(1) }
                ?.let { Jsoup.parse("<iframe src=\"$it\"></iframe>", episodeUrl).selectFirst("iframe")?.absUrl("src").orEmpty() }
                .orEmpty()
            if (iframe.isBlank()) continue
            val resolved = resolvePage(client, resolver, iframe, episodeUrl)
            if (resolved.isNotEmpty()) return resolved
        }
        return resolvePage(client, resolver, episodeUrl, episodeUrl)
    }

    private suspend fun resolvePage(
        client: OkHttpClient,
        resolver: StreamResolver,
        url: String,
        referer: String,
    ): List<ProviderStream> {
        val direct = resolver.resolve(listOf(url), referer)
        if (direct.isNotEmpty()) return direct

        val doc = getDocument(client, url, referer) ?: return emptyList()
        val candidates = buildList {
            doc.select("video source[src], video[src], source[src]").forEach { add(it.absUrl("src")) }
            doc.select("iframe[src]").forEach { add(it.absUrl("src")) }
        }.filter { it.startsWith("http", true) }.distinct()
        if (candidates.isEmpty()) return emptyList()
        return resolver.resolve(candidates, referer = url)
    }

    private suspend fun getDocument(client: OkHttpClient, url: String, referer: String): Document? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", UA)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Referer", referer)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) null
                    else response.body?.string()?.takeIf { it.isNotBlank() }?.let { Jsoup.parse(it, url) }
                }
            }.getOrNull()
        }

    private suspend fun post(client: OkHttpClient, url: String, body: FormBody, referer: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .header("User-Agent", UA)
                    .header("Accept", "*/*")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Referer", referer)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) null else response.body?.string()
                }
            }.getOrNull()
        }

    private fun extractAjaxValue(raw: String): String {
        val trimmed = raw.trim()
        return Regex("""[:=]\s*[\"']([^\"']+)[\"']""").find(trimmed)?.groupValues?.getOrNull(1)
            ?: trimmed.removeSurrounding("\"").trim()
    }

    private fun decodeBase64(value: String): String =
        runCatching { String(Base64.getDecoder().decode(value)) }
            .getOrElse { value }
}
