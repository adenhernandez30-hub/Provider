package com.kakaanime.providerv2

import java.util.concurrent.ConcurrentHashMap

class AniLabCircuitBreaker(
    private val failureThreshold: Int = 3,
    private val cooldownMillis: Long = 60_000L,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private data class State(
        var failures: Int = 0,
        var openUntil: Long = 0L,
    )

    private val states = ConcurrentHashMap<String, State>()

    fun allow(providerId: String): Boolean {
        val state = states[providerId] ?: return true
        if (state.openUntil <= nowMillis()) {
            if (state.openUntil != 0L) {
                synchronized(state) {
                    if (state.openUntil <= nowMillis()) state.openUntil = 0L
                }
            }
            return true
        }
        return false
    }

    fun recordSuccess(providerId: String) {
        states.remove(providerId)
    }

    fun recordFailure(providerId: String) {
        val state = states.computeIfAbsent(providerId) { State() }
        synchronized(state) {
            state.failures++
            if (state.failures >= failureThreshold) {
                state.openUntil = nowMillis() + cooldownMillis
            }
        }
    }
}
