package com.kakaanime.provider.wibuku

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Small, credential-agnostic Wibuku API client.
 *
 * The API endpoint is known from the reverse-engineered app flow, but the
 * server may require credentials that are not embedded in this project.
 * Credentials are therefore injected by the host instead of hard-coded.
 */
class WibukuApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val deviceHash: String? = null,
    private val extraHeaders: Map<String, String> = emptyMap(),
    private val client: OkHttpClient = defaultClient()
) : WibukuApi {
    override suspend fun getEpisodeMeta(id: String, mode: Int): EpisodeMetaResponse? = withContext(Dispatchers.IO) {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return@withContext null

        runCatching {
            val url = baseUrl.trimEnd('/').toHttpUrl().newBuilder()
                .addPathSegment("anime")
                .addPathSegment("episodemeta")
                .addPathSegment(cleanId)
                .addQueryParameter("mode", mode.toString())
                .build()

            val requestBuilder = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "KakaAnime-Provider/0.1")

            deviceHash?.trim()?.takeIf { it.isNotBlank() }?.let {
                requestBuilder.header("device_hash", it)
            }
            extraHeaders.forEach { (key, value) ->
                if (key.isNotBlank() && value.isNotBlank()) requestBuilder.header(key, value)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val body = response.body?.string()?.trim().orEmpty()
                if (body.isBlank()) return@use null
                EpisodeMetaResponse.fromJson(body, response.code)
            }
        }.getOrNull()
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://panel.wibuku.app"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}

data class EpisodeMetaResponse(
    val httpCode: Int,
    val status: String?,
    val error: String?,
    val meta: EpisodeMeta?
) {
    companion object {
        fun fromJson(body: String, httpCode: Int): EpisodeMetaResponse {
            val root = runCatching { JSONObject(body) }.getOrElse {
                return EpisodeMetaResponse(httpCode, null, "Invalid JSON response", null)
            }

            val status = root.optString("status").trim().ifBlank { null }
            val error = root.optString("error").trim().ifBlank { null }
            val source = root.optJSONObject("data") ?: root
            val meta = EpisodeMeta.fromJson(source)
            return EpisodeMetaResponse(httpCode, status, error, meta)
        }
    }
}

data class EpisodeMeta(
    val id: String,
    val parentId: String?,
    val name: String?,
    val views: Int?,
    val date: String?,
    val comments: Int?,
    val streamSources: List<WibukuStreamSource>,
    val likes: Int?,
    val dislikes: Int?,
    val react: String?,
    val history: Any?,
    val giveaways: Any?
) {
    companion object {
        fun fromJson(root: JSONObject): EpisodeMeta? {
            val id = root.optString("id").trim()
            val sources = root.optJSONArray("stream_sources")?.let(WibukuStreamSource::fromArray).orEmpty()
            if (id.isBlank() && sources.isEmpty() && !root.has("name")) return null

            return EpisodeMeta(
                id = id,
                parentId = root.optString("parent_id").trim().ifBlank { null },
                name = root.optString("name").trim().ifBlank { null },
                views = root.optIntOrNull("views"),
                date = root.optString("date").trim().ifBlank { null },
                comments = root.optIntOrNull("comments"),
                streamSources = sources,
                likes = root.optIntOrNull("likes"),
                dislikes = root.optIntOrNull("dislikes"),
                react = root.optString("react").trim().ifBlank { null },
                history = root.opt("history").takeUnless { it == JSONObject.NULL },
                giveaways = root.opt("giveaways").takeUnless { it == JSONObject.NULL }
            )
        }
    }
}

data class WibukuStreamSource(
    val id: String?,
    val link: String,
    val quality: String?,
    val type: String?
) {
    companion object {
        fun fromArray(array: JSONArray): List<WibukuStreamSource> = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val link = item.optString("link").trim()
                if (link.isBlank()) continue
                add(
                    WibukuStreamSource(
                        id = item.optString("id").trim().ifBlank { null },
                        link = link,
                        quality = item.optString("quality").trim().ifBlank { null },
                        type = item.optString("type").trim().ifBlank { null }
                    )
                )
            }
        }
    }
}

private fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key).takeIf { opt(key) !is String || (optString(key).trim().toIntOrNull() != null) }
}
