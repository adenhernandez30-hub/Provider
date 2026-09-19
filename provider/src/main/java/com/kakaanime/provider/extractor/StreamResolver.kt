package com.kakaanime.provider.extractor

import com.kakaanime.provider.ProviderStream
import com.kakaanime.provider.StreamType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Resolves provider/server URLs through the extractor chain. */
class StreamResolver(
    private val registry: ExtractorRegistry,
    private val validator: StreamValidator = StreamValidator(),
    private val browserResolver: BrowserStreamResolver? = null,
    private val maxParallelValidation: Int = DEFAULT_MAX_PARALLEL_VALIDATION,
) {
    init {
        require(maxParallelValidation > 0) { "maxParallelValidation must be positive" }
    }

    suspend fun resolve(
        urls: List<String>,
        referer: String? = null
    ): List<ProviderStream> = supervisorScope {
        val inputUrls = urls.mapNotNull(::normalizeHttpUrl)
            .distinct()
        val normalizedReferer = referer?.trim()?.takeIf { it.isNotBlank() }

        if (inputUrls.isEmpty()) return@supervisorScope emptyList()

        val directCandidates = DirectStreamFastPath.candidates(inputUrls)
        if (directCandidates.isNotEmpty()) {
            val direct = validateDirectUrls(directCandidates, normalizedReferer)
            if (direct.isNotEmpty()) return@supervisorScope direct
        }

        val extracted = inputUrls.flatMap { url ->
            registry.find(url).map { extractor ->
                async {
                    runCatching { extractor.extract(url, normalizedReferer) }
                        .getOrDefault(emptyList())
                }
            }.awaitAll().flatten()
        }
            .mapNotNull { normalizeCandidateStream(it, normalizedReferer) }
            .distinctBy { it.url to refererHeaderValue(it.headers) }

        if (extracted.isEmpty()) {
            val direct = validateDirectUrls(inputUrls, normalizedReferer)
            if (direct.isNotEmpty()) return@supervisorScope direct
            return@supervisorScope resolveWithBrowser(inputUrls, normalizedReferer)
        }

        val validated = validateCandidates(extracted)
        if (validated.isNotEmpty()) return@supervisorScope validated

        val direct = validateDirectUrls(inputUrls, normalizedReferer)
        if (direct.isNotEmpty()) return@supervisorScope direct

        val browserInputs = (extracted.map { it.url } + inputUrls).distinct()
        resolveWithBrowser(browserInputs, normalizedReferer)
    }

    private suspend fun validateCandidates(candidates: List<ProviderStream>): List<ProviderStream> =
        boundedValidate(candidates)

    private suspend fun validateDirectUrls(urls: List<String>, referer: String?): List<ProviderStream> =
        boundedValidate(
            urls.map { url ->
                ProviderStream(providerId = "", url = url, type = StreamType.UNKNOWN, headers = refererHeaders(referer))
            }
        )

    private suspend fun boundedValidate(candidates: List<ProviderStream>): List<ProviderStream> =
        supervisorScope {
            val semaphore = Semaphore(maxParallelValidation)
            candidates.map { candidate ->
                async {
                    semaphore.withPermit {
                        runCatching { validator.validate(candidate) }.getOrNull()
                    }
                }
            }.awaitAll()
                .filterNotNull()
                .filter { it.type != StreamType.UNKNOWN }
                .distinctBy { it.url to refererHeaderValue(it.headers) }
        }

    private suspend fun resolveWithBrowser(urls: List<String>, referer: String?): List<ProviderStream> {
        val resolver = browserResolver ?: return emptyList()
        val browserCandidates = coroutineScope {
            urls.map { url ->
                async { runCatching { resolver.resolve(url, referer) }.getOrDefault(emptyList()) }
            }.awaitAll().flatten()
                .mapNotNull { normalizeCandidateStream(it, referer) }
                .distinctBy { it.url to refererHeaderValue(it.headers) }
        }
        if (browserCandidates.isEmpty()) return emptyList()
        return validateCandidates(browserCandidates)
    }

    private fun normalizeCandidateStream(stream: ProviderStream, fallbackReferer: String?): ProviderStream? {
        val normalizedUrl = normalizeHttpUrl(stream.url) ?: return null
        return stream.copy(
            url = normalizedUrl,
            headers = mergeReferer(stream.headers, fallbackReferer)
        )
    }

    private fun normalizeHttpUrl(value: String): String? {
        val trimmed = value.trim().removeSurrounding("\"").removeSurrounding("'")
        if (trimmed.isBlank()) return null
        return when {
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> null
        }
    }

    private fun refererHeaders(referer: String?): Map<String, String> =
        referer?.takeIf { it.isNotBlank() }?.let { mapOf("Referer" to it) }.orEmpty()

    private fun mergeReferer(headers: Map<String, String>, fallbackReferer: String?): Map<String, String> {
        val referer = fallbackReferer?.takeIf { it.isNotBlank() } ?: return headers
        if (refererHeaderValue(headers) != null) return headers
        return headers + ("Referer" to referer)
    }

    private fun refererHeaderValue(headers: Map<String, String>): String? =
        headers.entries.firstOrNull { it.key.equals("Referer", ignoreCase = true) }?.value

    companion object {
        const val DEFAULT_MAX_PARALLEL_VALIDATION: Int = 4
    }
}
