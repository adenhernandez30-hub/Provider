package com.kakaanime.playbacke2e

import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kakaanime.providerv2.AniLabExtractionContext
import com.kakaanime.providerv2.AniLabProviderFactory
import com.kakaanime.providerv2.AniLabStreamType
import com.kakaanime.providerv2.AniLabVerifiedPipelineResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class SamehadakuMedia3PlaybackTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(PlaybackTestActivity::class.java)

    @Test
    fun real_samehadaku_stream_reaches_media3_first_frame() {
        val instrumentationUrl = runCatching {
            InstrumentationRegistry.getArguments()
                .getString("SAMEHADAKU_E2E_EPISODE_URL")
                ?.trim()
                .orEmpty()
        }.getOrDefault("")

        val episodeUrl = instrumentationUrl.ifBlank {
            System.getenv("SAMEHADAKU_E2E_EPISODE_URL")?.trim().orEmpty()
        }

        check(episodeUrl.isNotBlank()) {
            "SAMEHADAKU_E2E_EPISODE_URL is required for Media3 playback E2E"
        }

        val candidate = runBlocking {
            val stack = AniLabProviderFactory.samehadaku()
            val provider = stack.providers.single { it.id == "samehadaku" }

            // The playback E2E is pinned to a real episode URL. Resolve its
            // series detail directly so a blocked/changed site search endpoint
            // does not prevent the actual extraction/playback path from running.
            val detailUrl = deriveDetailUrl(episodeUrl)
                ?: error("Could not derive Samehadaku detail URL from episode URL: $episodeUrl")
            val detail = provider.load(detailUrl)
                ?: error("Samehadaku detail could not be loaded: $detailUrl")
            check(detail.episodes.isNotEmpty()) { "Samehadaku detail returned no episodes" }

            val episodeCandidates = provider.loadLinks(episodeUrl)
            check(episodeCandidates.isNotEmpty()) { "Samehadaku loadLinks returned no episode-page candidate" }

            val verified = stack.verifiedPipeline.resolve(
                providerId = provider.id,
                candidates = episodeCandidates,
                context = AniLabExtractionContext(referer = episodeUrl),
            )
            check(verified is AniLabVerifiedPipelineResult.Candidates) {
                "No verified stream candidate: $verified"
            }
            verified.candidates.first()
        }

        val firstFrame = CountDownLatch(1)
        val playbackError = AtomicReference<PlaybackException?>(null)
        var player: ExoPlayer? = null

        activityRule.scenario.onActivity { activity ->
            val requestProperties = linkedMapOf<String, String>()
            requestProperties.putAll(candidate.headers)
            candidate.referer?.takeIf { it.isNotBlank() }?.let { requestProperties["Referer"] = it }
            if (candidate.cookies.isNotEmpty()) {
                requestProperties["Cookie"] =
                    candidate.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            }

            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(requestProperties)

            val mediaItem = MediaItem.Builder()
                .setUri(candidate.url)
                .apply {
                    if (candidate.type == AniLabStreamType.HLS) setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                .build()

            val exo = ExoPlayer.Builder(activity)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .build()

            exo.addListener(object : androidx.media3.common.Player.Listener {
                override fun onRenderedFirstFrame() { firstFrame.countDown() }
                override fun onPlayerError(error: PlaybackException) {
                    playbackError.set(error)
                    firstFrame.countDown()
                }
            })

            activity.playerView.player = exo
            player = exo
            exo.setMediaItem(mediaItem)
            exo.prepare()
            exo.playWhenReady = true
        }

        val rendered = firstFrame.await(90, TimeUnit.SECONDS)
        player?.release()
        val error = playbackError.get()
        assertTrue(
            "Media3 did not render first frame. error=${error?.errorCodeName} ${error?.message}",
            rendered && error == null,
        )
    }

    private fun deriveDetailUrl(episodeUrl: String): String? = runCatching {
        val uri = URI(episodeUrl)
        val slug = uri.path.trim('/')
            .substringBeforeLast('/')
            .substringBeforeLast("-episode-")
            .trim('-')
        if (slug.isBlank()) return null
        URI(uri.scheme, uri.authority, "/anime/$slug/", null, null).toString()
    }.getOrNull()
}
