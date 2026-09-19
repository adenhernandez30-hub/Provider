package com.kakaanime.provider

import com.kakaanime.provider.extractor.BrowserStreamResolver
import com.kakaanime.provider.extractor.ExtractorRegistry
import com.kakaanime.provider.extractor.StreamResolver
import com.kakaanime.provider.extractor.StreamValidator
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StreamResolverTest {

    @Test
    fun resolvesDirectUrlAndPreservesRefererHeader() = runBlocking {
        val resolver = resolver()
        val referer = "https://episode.example/watch/1"

        val streams = resolver.resolve(
            urls = listOf(" https://cdn.example/video.m3u8?token=abc "),
            referer = referer
        )

        assertEquals(1, streams.size)
        assertEquals("https://cdn.example/video.m3u8?token=abc", streams.first().url)
        assertEquals(StreamType.HLS, streams.first().type)
        assertEquals(referer, streams.first().headers["Referer"])
    }

    @Test
    fun resolvesNonDirectEmbedPathThroughExtractorContract() = runBlocking {
        val server = htmlServer(
            "/embed" to """
                <html>
                    <body>
                        <video src="//media.example/stream.m3u8"></video>
                    </body>
                </html>
            """.trimIndent()
        )
        try {
            val resolver = resolver()
            val referer = "https://episode.example/watch/2"

            val streams = resolver.resolve(
                urls = listOf("${server.baseUrl}/embed"),
                referer = referer
            )

            assertEquals(1, streams.size)
            assertEquals("http://media.example/stream.m3u8", streams.first().url)
            assertEquals(StreamType.HLS, streams.first().type)
            assertTrue(streams.first().headers["Referer"]?.contains("/embed") == true)
        } finally {
            server.close()
        }
    }

    @Test
    fun rejectsMalformedStreamUrlCandidates() = runBlocking {
        val resolver = resolver()

        val streams = resolver.resolve(
            urls = listOf("https://cdn.example/good.m3u8", "http:// bad.example/bad.m3u8"),
            referer = "https://episode.example/watch/3"
        )

        assertEquals(1, streams.size)
        assertEquals("https://cdn.example/good.m3u8", streams.first().url)
    }

    @Test
    fun browserFallbackIsOptionalAndValidatedWhenPresent() = runBlocking {
        val server = htmlServer("/empty" to "<html><body><p>no stream</p></body></html>")
        try {
            val input = "${server.baseUrl}/empty"
            val referer = "https://episode.example/watch/4"

            val withoutBrowser = resolver()
            assertTrue(withoutBrowser.resolve(listOf(input), referer).isEmpty())

            val withBrowser = resolver(
                browserResolver = BrowserStreamResolver { _, _ ->
                    listOf(ProviderStream(providerId = "browser", url = "https://edge.example/tokenized?id=7"))
                }
            )
            val streams = withBrowser.resolve(listOf(input), referer)

            assertEquals(1, streams.size)
            assertEquals(StreamType.HLS, streams.first().type)
            assertEquals("https://edge.example/tokenized?id=7", streams.first().url)
            assertEquals(referer, streams.first().headers["Referer"])
        } finally {
            server.close()
        }
    }

    private fun resolver(browserResolver: BrowserStreamResolver? = null): StreamResolver =
        StreamResolver(
            registry = ExtractorRegistry(
                includeSamehadakuEpisodeExtractor = false,
                browserResolver = browserResolver
            ),
            validator = FakeValidator(),
            browserResolver = browserResolver
        )

    private class FakeValidator : StreamValidator() {
        override suspend fun validate(stream: ProviderStream): ProviderStream? {
            val url = stream.url.trim()
            if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) return null
            if (url.contains(' ')) return null
            val type = when {
                stream.type != StreamType.UNKNOWN -> stream.type
                url.contains(".m3u8", true) || url.contains("tokenized", true) -> StreamType.HLS
                url.contains(".mpd", true) -> StreamType.DASH
                url.contains(".mp4", true) -> StreamType.MP4
                else -> StreamType.UNKNOWN
            }
            if (type == StreamType.UNKNOWN) return null
            return stream.copy(url = url, type = type)
        }
    }

    private data class TestServer(val baseUrl: String, val close: () -> Unit)

    private fun htmlServer(vararg pages: Pair<String, String>): TestServer {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        pages.forEach { (path, body) ->
            server.createContext(path) { exchange ->
                val bytes = body.toByteArray()
                exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
        }
        server.start()
        val base = "http://127.0.0.1:${server.address.port}"
        return TestServer(baseUrl = base, close = { server.stop(0) })
    }
}
