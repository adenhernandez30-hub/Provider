package com.kakaanime.provider

import com.kakaanime.provider.extractor.StreamResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.util.Base64

/** Site-specific discovery; StreamResolver owns final extraction and validation. */
object SiteSpecificStreamResolver {
    private const val UA = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/140.0.0.0 Mobile Safari/537.36"

    suspend fun otakudesu(client: OkHttpClient, resolver: StreamResolver, episodeUrl: String): List<ProviderStream> {
        val doc = getDocument(client, episodeUrl, episodeUrl) ?: return emptyList()
        val script = doc.selectFirst("script:containsData({action:)")?.data().orEmpty()
        if (script.isBlank()) return resolvePage(client, resolver, episodeUrl, episodeUrl)
        val nonceAction = script.substringAfter("{action:\"", "").substringBefore("\"").ifBlank { script.substringAfter("action:\"", "").substringBefore("\"") }
        val action = script.substringAfter("action:\"", "").substringBefore("\"")
        if (nonceAction.isBlank() || action.isBlank()) return resolvePage(client, resolver, episodeUrl, episodeUrl)
        val nonce = post(client, URI(episodeUrl).let { "${it.scheme}://${it.host}/wp-admin/admin-ajax.php" }, FormBody.Builder().add("action", nonceAction).build(), episodeUrl)?.let(::extractAjaxValue).orEmpty()
        val mirrors = doc.select("div.mirrorstream ul li > a")
        for (mirror in mirrors) {
            val encoded = mirror.attr("data-content").trim()
            if (encoded.isBlank() || nonce.isBlank()) continue
            val decoded = decodeBase64(encoded).removeSurrounding("[")
            val fields = decoded.removeSuffix("]").split(",").associate { it.substringBefore(":").replace("\"", "").trim() to it.substringAfter(":", "").replace("\"", "").trim() }
            val id = fields["id"].orEmpty(); val index = fields["i"].orEmpty(); val quality = fields["q"].orEmpty()
            if (id.isBlank() || index.isBlank()) continue
            val origin = URI(episodeUrl)
            val ajaxUrl = "${origin.scheme}://${origin.host}/wp-admin/admin-ajax.php"
            val body = FormBody.Builder().add("id", id).add("i", index).add("q", quality).add("nonce", nonce).add("action", action).build()
            val iframe = post(client, ajaxUrl, body, episodeUrl)?.let(::extractAjaxValue)?.let(::decodeBase64)?.let { Jsoup.parse(it, episodeUrl).selectFirst("iframe[src], iframe[data-src]")?.let { frame -> frame.absUrl("src").ifBlank { frame.attr("src") }.ifBlank { frame.absUrl("data-src") }.ifBlank { frame.attr("data-src") } } }.orEmpty()
            if (iframe.isBlank()) continue
            val resolved = resolvePage(client, resolver, iframe, episodeUrl)
            if (resolved.isNotEmpty()) return resolved.map { it.copy(quality = it.quality ?: quality.ifBlank { null }) }
        }
        return emptyList()
    }

    suspend fun samehadaku(client: OkHttpClient, resolver: StreamResolver, episodeUrl: String): List<ProviderStream> {
        val doc = getDocument(client, episodeUrl, episodeUrl) ?: return emptyList()
        val origin = URI(episodeUrl)
        val ajaxUrl = "${origin.scheme}://${origin.host}/wp-admin/admin-ajax.php"
        val servers = doc.select("#server > ul > li > div, #server .server_option, #server [data-post][data-nume], .server_option[data-post], .server_option [data-post]")
        for (server in servers) {
            val body = FormBody.Builder().add("action", "player_ajax").add("post", server.attr("data-post")).add("nume", server.attr("data-nume")).add("type", server.attr("data-type")).build()
            val iframe = post(client, ajaxUrl, body, episodeUrl)?.let { raw ->
                val data = runCatching { org.json.JSONObject(raw).optString("data") }.getOrNull().orEmpty()
                val payload = data.ifBlank { raw }
                Regex("""(?:src|url)\s*=\s*[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE).find(payload)?.groupValues?.getOrNull(1)
                    ?: Jsoup.parse(payload, episodeUrl).selectFirst("iframe[src], iframe[data-src]")?.let { frame -> frame.absUrl("src").ifBlank { frame.attr("src") }.ifBlank { frame.absUrl("data-src") }.ifBlank { frame.attr("data-src") } }
            }.orEmpty()
            if (iframe.isBlank()) continue
            val resolved = resolvePage(client, resolver, iframe, episodeUrl)
            if (resolved.isNotEmpty()) return resolved
        }
        return resolvePage(client, resolver, episodeUrl, episodeUrl)
    }

    private suspend fun resolvePage(client: OkHttpClient, resolver: StreamResolver, url: String, referer: String): List<ProviderStream> {
        // Never expose media discovered from an embed page before validation.
        val direct = resolver.resolve(listOf(url), referer)
        if (direct.isNotEmpty()) return direct
        val doc = getDocument(client, url, referer) ?: return emptyList()
        val candidates = buildList {
            doc.select("video source[src], video source[data-src], video[src], video[data-src], source[src], source[data-src]").forEach { element ->
                add(element.absUrl("src").ifBlank { element.attr("src") }.ifBlank { element.absUrl("data-src") }.ifBlank { element.attr("data-src") })
            }
            doc.select("iframe[src], iframe[data-src], embed[src], embed[data-src]").forEach { element ->
                add(element.absUrl("src").ifBlank { element.attr("src") }.ifBlank { element.absUrl("data-src") }.ifBlank { element.attr("data-src") })
            }
        }.filter { it.startsWith("http", true) }.distinct()
        if (candidates.isEmpty()) return emptyList()
        return resolver.resolve(candidates, referer = url)
    }

    private suspend fun getDocument(client: OkHttpClient, url: String, referer: String): Document? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).header("User-Agent", UA).header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8").header("Referer", referer).build()
            client.newCall(request).execute().use { response -> if (!response.isSuccessful) null else response.body?.string()?.takeIf { it.isNotBlank() }?.let { Jsoup.parse(it, url) } }
        }.getOrNull()
    }

    private suspend fun post(client: OkHttpClient, url: String, body: FormBody, referer: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).post(body).header("User-Agent", UA).header("Accept", "*/*").header("X-Requested-With", "XMLHttpRequest").header("Origin", URI(referer).let { "${it.scheme}://${it.host}" }).header("Referer", referer).build()
            client.newCall(request).execute().use { response -> if (!response.isSuccessful) null else response.body?.string() }
        }.getOrNull()
    }

    private fun extractAjaxValue(raw: String): String = runCatching { org.json.JSONObject(raw).optString("data").takeIf { it.isNotBlank() } }.getOrNull() ?: raw.trim().removeSurrounding("\"")
    private fun decodeBase64(value: String): String = runCatching { String(Base64.getDecoder().decode(value), Charsets.UTF_8) }.getOrElse { value }
}
