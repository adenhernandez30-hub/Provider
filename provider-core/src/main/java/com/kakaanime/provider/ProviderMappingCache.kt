package com.kakaanime.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Small in-memory TTL cache with per-key in-flight request deduplication.
 *
 * A shared scope is supplied by the owner so lifecycle and cancellation stay
 * outside the cache itself.
 */
class ProviderMappingCache(
    private val scope: CoroutineScope,
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    init {
        require(ttlMs > 0) { "ttlMs must be positive" }
    }

    private data class Entry(
        val mappings: List<ProviderMapping>,
        val expiresAtMs: Long,
    )

    private val mutex = Mutex()
    private val values = mutableMapOf<Int, Entry>()
    private val inFlight = mutableMapOf<Int, Deferred<Result<List<ProviderMapping>>>>()

    suspend fun get(anilistId: Int): List<ProviderMapping>? =
        mutex.withLock {
            values[anilistId]
                ?.takeIf { it.expiresAtMs > nowMs() }
                ?.mappings
        }

    suspend fun getOrLoad(
        anilistId: Int,
        loader: suspend () -> List<ProviderMapping>,
    ): List<ProviderMapping> {
        get(anilistId)?.let { return it }

        val deferred = mutex.withLock {
            getCachedUnsafe(anilistId)?.let { return@withLock null }
            inFlight[anilistId] ?: scope.async {
                try {
                    Result.success(loader())
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    Result.failure(error)
                }
            }.also { created ->
                inFlight[anilistId] = created
            }
        }

        if (deferred == null) {
            return get(anilistId).orEmpty()
        }

        val result = deferred.await()
        val mappings = result.getOrThrow()
        mutex.withLock {
            values[anilistId] = Entry(
                mappings = mappings.toList(),
                expiresAtMs = nowMs() + ttlMs,
            )
            inFlight.remove(anilistId)
        }
        return mappings
    }

    suspend fun invalidate(anilistId: Int) {
        mutex.withLock { values.remove(anilistId) }
    }

    suspend fun clear() {
        mutex.withLock { values.clear() }
    }

    private fun getCachedUnsafe(anilistId: Int): List<ProviderMapping>? =
        values[anilistId]
            ?.takeIf { it.expiresAtMs > nowMs() }
            ?.mappings

    companion object {
        const val DEFAULT_TTL_MS: Long = 6 * 60 * 60 * 1000L
    }
}
