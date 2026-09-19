package com.kakaanime.providerv2

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AniLabStreamVerifierTest {

    private val server = MockWebServer()

    @Test
    fun hlsResponse_isAcceptedAndTypeIsDetected() = runBlocking {
        server.start()
        try {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/vnd.apple.mpegurl")
                    .setBody("#EXTM3U\n#EXT-X-VERSION:3\n"),
            )

            val candidate = AniLabStreamCandidate(
                providerId = "test",
                serverId = "hls",
                url = server.url("/stream").toString(),
            )

            val result = AniLabStreamVerifier(testClient()).verify(candidate)

            val valid = assertIs<AniLabVerificationResult.Valid>(result)
            assertEquals(AniLabStreamType.HLS, valid.candidate.type)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun declaredTypeMismatch_isRejected() = runBlocking {
        server.start()
        try {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/vnd.apple.mpegurl")
                    .setBody("#EXTM3U\n"),
            )

            val candidate = AniLabStreamCandidate(
                providerId = "test",
                serverId = "mismatch",
                url = server.url("/stream").toString(),
                type = AniLabStreamType.MP4,
            )

            val result = AniLabStreamVerifier(testClient()).verify(candidate)

            val failure = assertIs<AniLabVerificationResult.Failure>(result)
            assertEquals(AniLabFailureType.INVALID_MEDIA, failure.failure.type)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun blockedResponse_isClassifiedAsHttpBlocked() = runBlocking {
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(403))

            val candidate = AniLabStreamCandidate(
                providerId = "test",
                serverId = "blocked",
                url = server.url("/stream").toString(),
            )

            val result = AniLabStreamVerifier(testClient()).verify(candidate)

            val failure = assertIs<AniLabVerificationResult.Failure>(result)
            assertEquals(AniLabFailureType.HTTP_BLOCKED, failure.failure.type)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun blankUrl_isRejectedWithoutHttpCall() = runBlocking {
        server.start()
        try {
            val candidate = AniLabStreamCandidate(
                providerId = "test",
                serverId = "blank",
                url = " ",
            )

            val result = AniLabStreamVerifier(testClient()).verify(candidate)

            val failure = assertIs<AniLabVerificationResult.Failure>(result)
            assertEquals(AniLabFailureType.INVALID_MEDIA, failure.failure.type)
            assertEquals(0, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    private fun testClient(): OkHttpClient = OkHttpClient.Builder()
        .build()
}
