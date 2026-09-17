package com.kakaanime.provider

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class ProviderHealthMonitor {
    private data class MutableHealth(
        val successfulRequests: AtomicInteger = AtomicInteger(0),
        val failedRequests: AtomicInteger = AtomicInteger(0),
        val totalLatencyMs: AtomicLong = AtomicLong(0),
        val lastCheckedAt: AtomicLong = AtomicLong(0),
        val lastSuccessAt: AtomicLong = AtomicLong(0),
        val lastFailureAt: AtomicLong = AtomicLong(0),
        val consecutiveFailures: AtomicInteger = AtomicInteger(0),
        @Volatile var lastError: String? = null,
    )

    private val states = ConcurrentHashMap<String, MutableHealth>()

    fun recordSuccess(providerId: String, latencyMs: Long) {
        val state = states.computeIfAbsent(providerId) { MutableHealth() }
        val now = System.currentTimeMillis()
        state.successfulRequests.incrementAndGet()
        state.totalLatencyMs.addAndGet(latencyMs.coerceAtLeast(0))
        state.lastCheckedAt.set(now)
        state.lastSuccessAt.set(now)
        state.consecutiveFailures.set(0)
        state.lastError = null
    }

    fun recordFailure(providerId: String, latencyMs: Long, error: Throwable?) {
        val state = states.computeIfAbsent(providerId) { MutableHealth() }
        val now = System.currentTimeMillis()
        state.failedRequests.incrementAndGet()
        state.totalLatencyMs.addAndGet(latencyMs.coerceAtLeast(0))
        state.lastCheckedAt.set(now)
        state.lastFailureAt.set(now)
        state.consecutiveFailures.incrementAndGet()
        state.lastError = error?.message?.take(200) ?: error?.javaClass?.simpleName
    }

    fun snapshot(providerId: String): ProviderHealthSnapshot {
        val state = states[providerId] ?: return ProviderHealthSnapshot(providerId = providerId)
        val success = state.successfulRequests.get()
        val failed = state.failedRequests.get()
        val total = success + failed
        return ProviderHealthSnapshot(
            providerId = providerId,
            successfulRequests = success,
            failedRequests = failed,
            averageLatencyMs = if (total == 0) 0L else state.totalLatencyMs.get() / total,
            lastCheckedAt = state.lastCheckedAt.get(),
            lastSuccessAt = state.lastSuccessAt.get(),
            lastFailureAt = state.lastFailureAt.get(),
            consecutiveFailures = state.consecutiveFailures.get(),
            lastError = state.lastError,
        )
    }

    fun all(providerIds: List<String>): List<ProviderHealthSnapshot> =
        providerIds.map(::snapshot)
}

data class ProviderHealthSnapshot(
    val providerId: String,
    val successfulRequests: Int = 0,
    val failedRequests: Int = 0,
    val averageLatencyMs: Long = 0L,
    val lastCheckedAt: Long = 0L,
    val lastSuccessAt: Long = 0L,
    val lastFailureAt: Long = 0L,
    val consecutiveFailures: Int = 0,
    val lastError: String? = null,
) {
    val successRate: Double
        get() {
            val total = successfulRequests + failedRequests
            return if (total == 0) 0.0 else successfulRequests.toDouble() / total
        }

    val status: String
        get() = when {
            lastCheckedAt == 0L -> "UNKNOWN"
            consecutiveFailures == 0 -> "HEALTHY"
            consecutiveFailures < 3 -> "DEGRADED"
            else -> "FAILING"
        }
}
