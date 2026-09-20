package com.kakaanime.providerv2

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/** Lightweight HTTP/media verification before handing a candidate to Media3. */
class AniLabStreamVerifier(
    private val client: OkHttpClient = defaultClient(),
) {
    suspend fun verify(candidate: AniLabStreamCandidate): AniLabVerificationResult =
        withContext(Dispatchers.IO) {
            if (candidate.url.isBlank()) {
                return@withContext AniLabVerificationResult.Failure(
                    AniLabFailure(candidate.providerId, AniLabFailureType.INVALID_MEDIA, "Candidate URL is blank"),
                )
            }

            val requestBuilder = Request.Builder()
                .url(candidate.url)
                .header("Accept", "*/*")
                .header("Range", "bytes=0-4095")

            candidate.referer?.takeIf { it.isNotBlank() }?.let {
                requestBuilder.header("Referer", it)
            }
            candidate.headers.forEach { (name, value) ->
                if (name.isNotBlank() && value.isNotBlank() &&
                    !name.equals("Range", true) &&
                    !name.equals("Referer", true) &&
                    !name.equals("Cookie", true)
                ) {
                    requestBuilder.header(name, value)
                }
            }
            if (candidate.cookies.isNotEmpty()) {
                requestBuilder.header(
                    "Cookie",
                    candidate.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" },
                )
            }

            try {
                client.newCall(requestBuilder.build()).execute().use { response ->
                    val code = response.code
                    if (code == 401 || code == 403 || code == 451) {
                        return@withContext AniLabVerificationResult.Failure(
                            AniLabFailure(candidate.providerId, AniLabFailureType.HTTP_BLOCKED, "HTTP $code"),
                        )
                    }
                    if (!response.isSuccessful) {
                        return@withContext AniLabVerificationResult.Failure(
                            AniLabFailure(candidate.providerId, AniLabFailureType.INVALID_MEDIA, "HTTP $code"),
                        )
                    }

                    val body = response.body ?: return@withContext AniLabVerificationResult.Failure(
                        AniLabFailure(candidate.providerId, AniLabFailureType.INVALID_MEDIA, "Empty response body"),
                    )
                    val contentType = body.contentType()?.toString()?.lowercase().orEmpty()
                    val sample = response.peekBody(MAX_SAMPLE_BYTES.toLong()).bytes()
                    val detected = detectType(candidate.url, contentType, sample)

                    if (detected == AniLabStreamType.HLS && !isValidHlsManifest(sample)) {
                        return@withContext AniLabVerificationResult.Failure(
                            AniLabFailure(
                                candidate.providerId,
                                AniLabFailureType.MANIFEST_INVALID,
                                "HLS response is not a valid manifest",
                            ),
                        )
                    }
                    if (detected == AniLabStreamType.DASH && !isValidDashManifest(sample)) {
                        return@withContext AniLabVerificationResult.Failure(
                            AniLabFailure(
                                candidate.providerId,
                                AniLabFailureType.MANIFEST_INVALID,
                                "DASH response is not a valid manifest",
                            ),
                        )
                    }

                    if (!isCompatible(candidate.type, detected)) {
                        return@withContext AniLabVerificationResult.Failure(
                            AniLabFailure(
                                candidate.providerId,
                                AniLabFailureType.INVALID_MEDIA,
                                "Detected $detected but candidate declares ${candidate.type}",
                            ),
                        )
                    }
                    AniLabVerificationResult.Valid(candidate.copy(type = detected))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SocketTimeoutException) {
                AniLabVerificationResult.Failure(
                    AniLabFailure(candidate.providerId, AniLabFailureType.TIMEOUT, e.message, e),
                )
            } catch (e: IOException) {
                AniLabVerificationResult.Failure(
                    AniLabFailure(candidate.providerId, AniLabFailureType.PROVIDER_UNAVAILABLE, e.message, e),
                )
            } catch (e: Exception) {
                AniLabVerificationResult.Failure(
                    AniLabFailure(candidate.providerId, AniLabFailureType.INVALID_MEDIA, e.message, e),
                )
            }
        }

    private fun detectType(url: String, contentType: String, bytes: ByteArray): AniLabStreamType {
        val normalizedUrl = url.substringBefore('?').lowercase()
        val text = bytes.toString(Charsets.UTF_8).trimStart()
        return when {
            contentType.contains("mpegurl") || contentType.contains("vnd.apple.mpegurl") ||
                normalizedUrl.endsWith(".m3u8") || text.startsWith("#EXTM3U") -> AniLabStreamType.HLS
            contentType.contains("dash+xml") || normalizedUrl.endsWith(".mpd") ||
                (text.startsWith("<?xml") && text.contains("<MPD", ignoreCase = true)) -> AniLabStreamType.DASH
            contentType.contains("webm") || normalizedUrl.endsWith(".webm") -> AniLabStreamType.WEBM
            contentType.contains("mp4") || normalizedUrl.endsWith(".mp4") ||
                (bytes.size >= 8 && bytes.copyOfRange(4, 8).contentEquals(byteArrayOf(0x66, 0x74, 0x79, 0x70))) -> AniLabStreamType.MP4
            else -> AniLabStreamType.UNKNOWN
        }
    }

    private fun isValidHlsManifest(bytes: ByteArray): Boolean =
        bytes.toString(Charsets.UTF_8).trimStart().startsWith("#EXTM3U")

    private fun isValidDashManifest(bytes: ByteArray): Boolean {
        val text = bytes.toString(Charsets.UTF_8).trimStart()
        return text.contains("<MPD", ignoreCase = true)
    }

    private fun isCompatible(declared: AniLabStreamType, detected: AniLabStreamType): Boolean =
        declared == AniLabStreamType.UNKNOWN || detected == declared

    companion object {
        private const val MAX_SAMPLE_BYTES = 16 * 1024

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}

sealed interface AniLabVerificationResult {
    data class Valid(val candidate: AniLabStreamCandidate) : AniLabVerificationResult
    data class Failure(val failure: AniLabFailure) : AniLabVerificationResult
}
