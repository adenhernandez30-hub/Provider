package ani.anilab.backend

import com.kakaanime.provider.ProviderFactory

/**
 * Entry point for the standalone AniLab Provider backend.
 *
 * HTTP/server wiring is intentionally kept separate from provider adapters.
 */
object Backend

fun main() {
    val engine = ProviderFactory.createEngine()
    val server = BackendServer(engine)
    server.start()
    println("AniLab Provider Backend listening on http://0.0.0.0:8080")
}
