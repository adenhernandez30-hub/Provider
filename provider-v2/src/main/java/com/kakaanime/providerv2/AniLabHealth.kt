package com.kakaanime.providerv2

import java.util.concurrent.ConcurrentHashMap

/**
 * Provider-level circuit breaker.
 *
 * A provider is opened after the configured number of consecutive failures.
 * When cooldown expires, the failure streak is reset before the next attempt.
 */
class AniLabCircuitBreaker(
    private val failureThreshold: Int = 3,
    private val cooldownMillis: Long = 60_000L,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private data class State(
        var failures: Int = 0,
        var openUntil: Long = 0L,
    )

    init {
        require(failureThreshold > 0) { "failureThreshold must be > 0" }
        require(cooldownMillis >= 0) { "cooldownMillis must be >= 0" }
    }

    private val states = ConcurrentHashMap<String, State>()

    fun allow(providerId: String): Boolean {
        if (providerId.isBlank()) return false

        val state = states[providerId] ?: return true
        synchronized(state) {
            val now = nowMillis()
            if (state.openUntil > now) return false
            if (state.openUntil != 0L) {
                state.openUntil = 0L
                state.failures = 0
            }
            return true
        }
    }

    fun recordSuccess(providerId: String) {
        if (providerId.isNotBlank()) states.remove(providerId)
    }

    fun recordFailure(providerId: String) {
        if (providerId.isBlank()) return

        val state = states.computeIfAbsent(providerId) { State() }
        synchronized(state) {
            state.failures++
            if (state.failures >= failureThreshold) {
                state.openUntil = nowMillis() + cooldownMillis
            }
        }
    }
}
