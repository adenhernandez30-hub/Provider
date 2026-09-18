package com.kakaanime.provider

import com.kakaanime.provider.extractor.BrowserStreamResolver
import com.kakaanime.provider.extractor.ExtractorRegistry
import com.kakaanime.provider.extractor.StreamResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Remote catalog adapter. Stream candidates always cross the shared resolver boundary. */
class RemoteSourceProviderV2(
    override val id: String,
    override val name: String,
    override val priority: Int,
    private val sourceSlug: String,
    browserResolver: BrowserStreamResolver? = null,
) : AnimeProvider {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()
    private val resolver = StreamResolver(ExtractorRegistry(browserResolver = browserResolver), browserResolver = browserResolver)
    private val sanka = "https://www.sankavollerei.web.id/anime"
    private val wajik = "https://wajik-anime-api.vercel.app"
    private val kumanime = "https://kumanime.vercel.app/api"
    private val vharasc = "https://www.vharasc.my.id/api/v1"

    override suspend fun search(query: String): List<ProviderAnime> {
        for (url in searchUrls(query)) requestJson(url)?.let { root -> extractItems(root).mapNotNull { it.toAnime() }.takeIf { it.isNotEmpty() }?.let { return it } }
        return emptyList()
    }
    override suspend fun getAnime(animeId: String): ProviderAnime? {
        val slug = animeId.removePrefix("$id:").trim('/')
        for (url in detailUrls(slug)) requestJson(url)?.toAnime(slug)?.let { return it }
        return null
    }
    override suspend fun getEpisodes(animeId: String): List<ProviderEpisode> {
        val slug = animeId.removePrefix("$id:").trim('/')
        for (url in detailUrls(slug)) {
            val root = requestJson(url) ?: continue
            val episodes = extractEpisodeItems(root).mapNotNull { item ->
                val number = item.episodeNumber() ?: return@mapNotNull null
                val endpoint = item.firstString("slug", "endpoint", "id", "url") ?: number.toString()
                ProviderEpisode("$id:$endpoint", "$id:$slug", number, id, item.firstString("title", "name") ?: "Episode $number", isNew = item.optBoolean("isNew", false))
            }.distinctBy { it.number }.sortedBy { it.number }
            if (episodes.isNotEmpty()) return episodes
        }
        return emptyList()
    }
    override suspend fun getStreams(animeId: String, episodeNumber: Int): List<ProviderStream> {
        val slug = animeId.removePrefix("$id:").trim('/')
        val episodeId = getEpisodes("$id:$slug").firstOrNull { it.number == episodeNumber }?.id?.removePrefix("$id:")?.trim('/') ?: episodeNumber.toString()
        for (url in episodeUrls(slug, episodeId, episodeNumber)) {
            val root = requestJson(url) ?: continue
            val raw = mutableListOf<ProviderStream>(); collectStreams(root, raw)
            if (raw.isEmpty()) continue
            val metadata = raw.associateBy { it.url }
            val resolved = resolver.resolve(raw.map { it.url }, referer = url)
            if (resolved.isNotEmpty()) return resolved.map { stream ->
                val source = metadata[stream.url]
                stream.copy(providerId = id, quality = stream.quality ?: source?.quality, language = stream.language ?: source?.language, subtitleLanguage = stream.subtitleLanguage ?: source?.subtitleLanguage)
            }.distinctBy { it.url }
        }
        return emptyList()
    }
    private fun searchUrls(query: String): List<String> = buildList { val q = encode(query); when (id) { "kuramanime", "oploverz" -> add("$wajik/$sourceSlug/search?q=$q"); "animeindo" -> add("$kumanime/search/$q"); "anoboy", "nimegami", "kusonime", "animekompi" -> add("$vharasc/$id/search/$q") }; add("$sanka/$sourceSlug/search/$q") }
    private fun detailUrls(slug: String): List<String> = buildList { val value = encodePath(slug); when (id) { "kuramanime", "oploverz" -> add("$wajik/$sourceSlug/anime/$value"); "animeindo" -> add("$kumanime/anime/$value"); "anoboy", "nimegami", "kusonime", "animekompi" -> add("$vharasc/$id/detail/$value") }; add("$sanka/$sourceSlug/detail/$value") }
    private fun episodeUrls(slug: String, episodeId: String, number: Int): List<String> = buildList { val ep = encodePath(episodeId); when (id) { "kuramanime", "oploverz" -> { add("$wajik/$sourceSlug/episode/$ep"); add("$wajik/$sourceSlug/episode/$number") }; "animeindo" -> add("$kumanime/episode/$ep"); "anoboy", "nimegami", "kusonime", "animekompi" -> add("$vharasc/$id/episode/$ep") }; add("$sanka/$sourceSlug/episode/$ep") }
    private suspend fun requestJson(url: String): JSONObject? = withContext(Dispatchers.IO) { runCatching { val request = Request.Builder().url(url).header("User-Agent", "KakaAnime/0.1").header("Accept", "application/json").build(); client.newCall(request).execute().use { response -> if (!response.isSuccessful) null else response.body?.string()?.trim()?.takeIf { it.isNotBlank() }?.let { body -> runCatching { JSONObject(body) }.getOrElse { JSONObject().put("_root", JSONArray(body)) } } } }.getOrNull() }
    private fun extractItems(root: JSONObject): List<JSONObject> { root.optJSONArray("_root")?.let { return it.objects() }; for (key in listOf("results", "anime", "items", "search", "list", "animeList", "data")) root.optJSONArray(key)?.let { return it.objects() }; root.optJSONObject("data")?.let { nested -> extractItems(nested).takeIf { it.isNotEmpty() }?.let { return it } }; return if (root.has("title") || root.has("name") || root.has("judul")) listOf(root) else emptyList() }
    private fun extractEpisodeItems(root: JSONObject): List<JSONObject> { for (key in listOf("episodes", "episode", "episodeList", "episode_list", "episodeListData")) root.optJSONArray(key)?.let { return it.objects() }; root.optJSONObject("data")?.let { nested -> extractEpisodeItems(nested).takeIf { it.isNotEmpty() }?.let { return it } }; return emptyList() }
    private fun JSONObject.toAnime(fallbackId: String? = null): ProviderAnime? { val title = firstString("title", "name", "animeTitle", "judul") ?: return null; val rawId = firstString("slug", "endpoint", "id", "animeId", "anime_id") ?: fallbackId ?: title.slugify(); return ProviderAnime("$id:$rawId", title, id, posterUrl = firstString("poster", "posterUrl", "image", "thumbnail", "thumb", "poster_url"), description = firstString("description", "synopsis", "sinopsis") ?: "", genres = extractStrings(this, "genres", "genre"), status = firstString("status") ?: "UNKNOWN", latestEpisode = extractEpisodeItems(this).mapNotNull { it.episodeNumber() }.maxOrNull()) }
    private fun collectStreams(value: Any?, out: MutableList<ProviderStream>, quality: String? = null) { when (value) { is JSONObject -> { val keys = listOf("url", "file", "stream", "streamUrl", "stream_url", "m3u8", "mp4", "source", "videoUrl"); val q = value.firstString("quality", "resolution") ?: quality; keys.forEach { key -> value.optString(key).trim().takeIf { it.startsWith("http", true) }?.let { out += ProviderStream(id, it, q, "Japanese", "Indonesian", StreamType.UNKNOWN) } }; val iterator = value.keys(); while (iterator.hasNext()) { val key = iterator.next(); if (key !in keys) collectStreams(value.opt(key), out, q) } }; is JSONArray -> for (i in 0 until value.length()) collectStreams(value.opt(i), out, quality); is String -> if (value.startsWith("http", true)) out += ProviderStream(id, value, quality, "Japanese", "Indonesian", StreamType.UNKNOWN) } }
    private fun JSONObject.firstString(vararg keys: String): String? = keys.firstNotNullOfOrNull { key -> optString(key).trim().ifBlank { null } }
    private fun JSONObject.episodeNumber(): Int? = firstString("episode", "episodeNumber", "number", "episodeNum")?.toIntOrNull() ?: Regex("(?:episode|eps|ep)[^0-9]*(\\d+)", RegexOption.IGNORE_CASE).find(firstString("title", "name", "slug", "endpoint", "id", "judul").orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()
    private fun extractStrings(root: JSONObject, vararg keys: String): List<String> = keys.firstNotNullOfOrNull { key -> root.optJSONArray(key)?.let { array -> buildList { for (i in 0 until array.length()) array.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add) } } } ?: emptyList()
    private fun JSONArray.objects(): List<JSONObject> = buildList { for (i in 0 until length()) optJSONObject(i)?.let(::add) }
    private fun String.slugify(): String = trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    private fun encode(value: String): String = URLEncoder.encode(value.trim(), "UTF-8")
    private fun encodePath(value: String): String = value.split('/').joinToString("/") { encode(it) }
}
