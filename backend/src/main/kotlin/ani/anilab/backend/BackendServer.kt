package ani.anilab.backend

import com.kakaanime.provider.AnimeProvider
import com.kakaanime.provider.ProviderAnime
import com.kakaanime.provider.ProviderEngine
import com.kakaanime.provider.ProviderEpisode
import com.kakaanime.provider.ProviderRegistry
import com.kakaanime.provider.ProviderStream
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class BackendServer(
    private val registry: ProviderRegistry,
    private val providerEngine: ProviderEngine = ProviderEngine(registry),
    private val port: Int = 8080,
) {
    private var server: HttpServer? = null

    fun start() {
        check(server == null) { "Backend server is already running" }

        val httpServer = HttpServer.create(InetSocketAddress(port), 0)
        httpServer.createContext("/") { exchange -> handle(exchange) }
        httpServer.executor = Executors.newCachedThreadPool()
        httpServer.start()
        server = httpServer
    }

    fun stop() {
        server?.stop(0)
        server = null
    }

    private fun handle(exchange: HttpExchange) {
        try {
            if (exchange.requestMethod != "GET") {
                respond(exchange, 405, "{\"error\":\"method_not_allowed\"}")
                return
            }

            val path = exchange.requestURI.path.trimEnd('/').ifEmpty { "/" }
            val segments = path.split('/').filter(String::isNotEmpty).map(::decode)

            when {
                path == "/" -> respond(exchange, 200, "{\"service\":\"AniLab Provider Backend\",\"status\":\"ok\"}")
                path == "/health" -> respond(exchange, 200, healthJson())
                path == "/providers" -> respond(exchange, 200, providersJson(registry.all()))
                segments.size == 2 && segments[0] == "search" ->
                    respond(exchange, 200, searchJson(providerEngine.search(segments[1])))
                segments.size == 2 && segments[0] == "anime" ->
                    respond(exchange, 200, animeJson(providerEngine.getAnime(segments[1])))
                segments.size == 3 && segments[0] == "anime" && segments[2] == "episodes" ->
                    respond(exchange, 200, episodesJson(providerEngine.getEpisodes(segments[1])))
                segments.size == 5 && segments[0] == "anime" && segments[2] == "episode" && segments[4] == "streams" ->
                    respond(exchange, 200, streamsJson(providerEngine.getFirstStream(segments[1], segments[3].toInt())))
                else -> respond(exchange, 404, "{\"error\":\"not_found\"}")
            }
        } catch (e: NumberFormatException) {
            respond(exchange, 400, "{\"error\":\"invalid_episode_number\"}")
        } catch (e: Exception) {
            respond(exchange, 500, "{\"error\":\"internal_error\"}")
        } finally {
            exchange.close()
        }
    }

    private fun healthJson(): String {
        val snapshots = providerEngine.healthMonitor().all(registry.all().map(AnimeProvider::id))
        val failing = snapshots.count { it.status == "FAILING" }
        val degraded = snapshots.count { it.status == "DEGRADED" }
        return "{\"status\":\"ok\",\"providers\":${snapshots.size},\"failing\":$failing,\"degraded\":$degraded,\"providerHealth\":${healthArray(snapshots)}}"
    }

    private fun healthArray(snapshots: List<com.kakaanime.provider.ProviderHealthSnapshot>): String =
        snapshots.joinToString(prefix = "[", postfix = "]") {
            "{\"providerId\":${json(it.providerId)},\"status\":${json(it.status)},\"successfulRequests\":${it.successfulRequests},\"failedRequests\":${it.failedRequests},\"successRate\":${it.successRate},\"averageLatencyMs\":${it.averageLatencyMs},\"lastCheckedAt\":${it.lastCheckedAt},\"lastSuccessAt\":${it.lastSuccessAt},\"lastFailureAt\":${it.lastFailureAt},\"consecutiveFailures\":${it.consecutiveFailures},\"lastError\":${json(it.lastError)}}"
        }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8)

    private fun providersJson(providers: List<AnimeProvider>): String =
        providers.joinToString(prefix = "[", postfix = "]") {
            "{\"id\":${json(it.id)},\"name\":${json(it.name)},\"priority\":${it.priority}}"
        }

    private fun searchJson(items: List<ProviderAnime>): String =
        items.joinToString(prefix = "[", postfix = "]") {
            "{\"id\":${json(it.id)},\"title\":${json(it.title)},\"providerId\":${json(it.providerId)},\"posterUrl\":${json(it.posterUrl)}}"
        }

    private fun animeJson(item: ProviderAnime?): String = item?.let {
        "{\"id\":${json(it.id)},\"title\":${json(it.title)},\"providerId\":${json(it.providerId)},\"description\":${json(it.description)},\"posterUrl\":${json(it.posterUrl)},\"backdropUrl\":${json(it.backdropUrl)},\"year\":${it.year ?: "null"}}"
    } ?: "null"

    private fun episodesJson(items: List<ProviderEpisode>): String =
        items.joinToString(prefix = "[", postfix = "]") {
            "{\"id\":${json(it.id)},\"animeId\":${json(it.animeId)},\"number\":${it.number},\"providerId\":${json(it.providerId)},\"title\":${json(it.title)}}"
        }

    private fun streamsJson(stream: ProviderStream?): String = stream?.let {
        "{\"providerId\":${json(it.providerId)},\"url\":${json(it.url)},\"quality\":${json(it.quality)},\"language\":${json(it.language)},\"subtitleLanguage\":${json(it.subtitleLanguage)},\"type\":${json(it.type.name)}}"
    } ?: "null"

    private fun json(value: String?): String = value?.let {
        "\"${it.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")}\""
    } ?: "null"
}
