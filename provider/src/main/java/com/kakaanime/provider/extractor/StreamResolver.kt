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
    init { require(maxParallelValidation > 0) { "maxParallelValidation must be positive" } }

    suspend fun resolve(urls: List<String>, referer: String? = null): List<ProviderStream> = supervisorScope {
        val inputUrls = urls.map(String::trim).filter(String::isNotBlank).distinct()
        if (inputUrls.isEmpty()) return@supervisorScope emptyList()

        val directCandidates = DirectStreamFastPath.candidates(inputUrls)
        if (directCandidates.isNotEmpty()) {
            val direct = validateDirectUrls(directCandidates)
            if (direct.isNotEmpty()) return@supervisorScope direct
        }

        val extracted = inputUrls.flatMap { url ->
            registry.find(url).map { extractor ->
                async { runCatching { extractor.extract(url, referer) }.getOrDefault(emptyList()) }
            }.awaitAll().flatten()
        }.filter { it.url.startsWith("http", true) }.distinctBy { it.url }

        if (extracted.isEmpty()) {
            val direct = validateDirectUrls(inputUrls)
            if (direct.isNotEmpty()) return@supervisorScope direct
            return@supervisorScope resolveWithBrowser(inputUrls, referer)
        }

        val validated = boundedValidate(extracted)
        if (validated.isNotEmpty()) return@supervisorScope validated

        val direct = validateDirectUrls(inputUrls)
        if (direct.isNotEmpty()) return@supervisorScope direct
        resolveWithBrowser((extracted.map { it.url } + inputUrls).distinct(), referer)
    }

    private suspend fun validateDirectUrls(urls: List<String>): List<ProviderStream> = boundedValidate(urls.map { ProviderStream("", it, type = StreamType.UNKNOWN) })

    private suspend fun boundedValidate(candidates: List<ProviderStream>): List<ProviderStream> = supervisorScope {
        val semaphore = Semaphore(maxParallelValidation)
        candidates.map { candidate -> async { semaphore.withPermit { runCatching { validator.validate(candidate) }.getOrNull() } } }
            .awaitAll().filterNotNull().filter { it.type != StreamType.UNKNOWN }.distinctBy { it.url }
    }

    private suspend fun resolveWithBrowser(urls: List<String>, referer: String?): List<ProviderStream> {
        val resolver = browserResolver ?: return emptyList()
        val discovered = coroutineScope {
            urls.map { url -> async { runCatching { resolver.resolve(url, referer) }.getOrDefault(emptyList()) } }
                .awaitAll().flatten().filter { it.url.startsWith("http", true) }.distinctBy { it.url }
        }
        // Browser/WebView discovery is only a candidate-producing stage. Do not
        // expose its result as playable until the same network validator confirms
        // the final URL and media type.
        return boundedValidate(discovered)
    }

    companion object { const val DEFAULT_MAX_PARALLEL_VALIDATION: Int = 4 }
}
