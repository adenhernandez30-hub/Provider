package com.kakaanime.provider.wibuku

import com.kakaanime.provider.ProviderStream
import com.kakaanime.provider.StreamType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WibukuProviderTest {

    @Test
    fun `maps episode metadata stream sources to provider streams`() = runBlocking {
        val api = FakeWibukuApi(
            EpisodeMetaResponse(
                httpCode = 200,
                status = "success",
                error = null,
                meta = EpisodeMeta(
                    id = "ep-7",
                    parentId = "anime-1",
                    name = "Episode 7",
                    views = null,
                    date = null,
                    comments = null,
                    streamSources = listOf(
                        WibukuStreamSource("s1", "https://cdn.example/7.m3u8", "1080P", "hls"),
                        WibukuStreamSource("s2", "https://cdn.example/7.mp4", "720P", "mp4"),
                        WibukuStreamSource("s3", "not-a-url", "480P", "mp4"),
                        WibukuStreamSource("s4", "https://cdn.example/7.m3u8", "1080P", "hls")
                    ),
                    likes = null,
                    dislikes = null,
                    react = null,
                    history = null,
                    giveaways = null
                )
            )
        )
        val provider = WibukuProvider(
            api = api,
            episodeIdResolver = WibukuEpisodeIdResolver { animeId, episodeNumber ->
                assertEquals("anime-1", animeId)
                assertEquals(7, episodeNumber)
                "ep-7"
            }
        )

        val streams = provider.getStreams("anime-1", 7)

        assertEquals(2, streams.size)
        assertEquals(
            listOf(
                ProviderStream("wibuku", "https://cdn.example/7.m3u8", "1080P", type = StreamType.HLS),
                ProviderStream("wibuku", "https://cdn.example/7.mp4", "720P", type = StreamType.MP4)
            ),
            streams
        )
    }

    @Test
    fun `auth failure response produces no streams`() = runBlocking {
        val provider = WibukuProvider(
            api = FakeWibukuApi(
                EpisodeMetaResponse(
                    httpCode = 200,
                    status = "failed",
                    error = "Invalid Auth credential.",
                    meta = null
                )
            ),
            episodeIdResolver = WibukuEpisodeIdResolver { _, _ -> "ep-7" }
        )

        val streams = provider.getStreams("anime-1", 7)

        assertTrue(streams.isEmpty())
    }

    @Test
    fun `non success HTTP response produces no streams`() = runBlocking {
        val provider = WibukuProvider(
            api = FakeWibukuApi(
                EpisodeMetaResponse(
                    httpCode = 401,
                    status = null,
                    error = "Unauthorized",
                    meta = null
                )
            ),
            episodeIdResolver = WibukuEpisodeIdResolver { _, _ -> "ep-7" }
        )

        assertTrue(provider.getStreams("anime-1", 7).isEmpty())
    }

    private class FakeWibukuApi(
        private val response: EpisodeMetaResponse?
    ) : WibukuApi {
        override suspend fun getEpisodeMeta(id: String, mode: Int): EpisodeMetaResponse? = response
    }
}
