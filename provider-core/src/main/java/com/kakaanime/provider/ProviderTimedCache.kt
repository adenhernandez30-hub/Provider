package com.kakaanime.provider

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Small stale-while-revalidate cache with per-key in-flight deduplication.
 *
 * Values remain serveable during the stale window while one background refresh
 * is allowed to update the entry. Loader failures never poison the caller's
 * scope and stale values remain available until their stale deadline.
 */
class ProviderTimedCache<K, V>(
    private val scope: CoroutineScope,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private data class Entry<V>(
        val value: V,
        val freshUntilMs: Long,
        val staleUntilMs: Long,
    )

    private val mutex = Mutex()
    private val values = mutableMapOf<K, Entry<V>>()
    private val inFlight = mutableMapOf<K, Deferred<Result<V>>>()

    suspend fun getOrLoad(
        key: K,
        freshTtlMs: (V) -> Long,
        staleTtlMs: (V) -> Long,
        loader: suspend () -> V,
    ): V {
        val now = nowMs()
        val existing = mutex.withLock { values[key] }

        if (existing != null && now < existing.freshUntilMs) {
            return existing.value
        }

        if (existing != null && now < existing.staleUntilMs) {
            refreshInBackground(key, freshTtlMs, staleTtlMs, loader)
            return existing.value
        }

        return loadAndStore(key, freshTtlMs, staleTtlMs, loader)
    }

    suspend fun get(key: K): V? =
        mutex.withLock {
            values[key]?.takeIf { it.staleUntilMs > nowMs() }?.value
        }

    suspend fun invalidate(key: K) {
        mutex.withLock { values.remove(key) }
    }

    suspend fun clear() {
        mutex.withLock { values.clear() }
    }

    private fun refreshInBackground(
        key: K,
        freshTtlMs: (V) -> Long,
        staleTtlMs: (V) -> Long,
        loader: suspend () -> V,
    ) {
        scope.launch {
            try {
                loadAndStore(key, freshTtlMs, staleTtlMs, loader)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                // Keep the stale value when refresh fails.
            }
        }
    }

    private suspend fun loadAndStore(
        key: K,
        freshTtlMs: (V) -> Long,
        staleTtlMs: (V) -> Long,
        loader: suspend () -> V,
    ): V {
        val deferred = mutex.withLock {
            inFlight[key] ?: scope.async {
                try {
                    Result.success(loader())
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    Result.failure(error)
                }
            }.also { inFlight[key] = it }
        }

        return try {
            val result = deferred.await()
            val value = result.getOrThrow()
            val now = nowMs()
            val freshTtl = freshTtlMs(value).also { require(it > 0) }
            val staleTtl = staleTtlMs(value).also { require(it >= freshTtl) }

            mutex.withLock {
                values[key] = Entry(
                    value = value,
                    freshUntilMs = now + freshTtl,
                    staleUntilMs = now + staleTtl,
                )
                inFlight.remove(key)
            }
            value
        } catch (error: Throwable) {
            mutex.withLock { inFlight.remove(key) }
            throw error
        }
    }
}
