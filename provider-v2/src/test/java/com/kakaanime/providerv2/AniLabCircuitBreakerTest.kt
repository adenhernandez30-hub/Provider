package com.kakaanime.providerv2

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AniLabCircuitBreakerTest {

    @Test
    fun opensAfterFailureThreshold() {
        val breaker = AniLabCircuitBreaker(failureThreshold = 3, cooldownMillis = 1_000L)

        breaker.recordFailure("provider")
        breaker.recordFailure("provider")
        assertTrue(breaker.allow("provider"))

        breaker.recordFailure("provider")
        assertFalse(breaker.allow("provider"))
    }

    @Test
    fun successResetsFailureState() {
        val breaker = AniLabCircuitBreaker(failureThreshold = 2, cooldownMillis = 1_000L)

        breaker.recordFailure("provider")
        breaker.recordSuccess("provider")
        breaker.recordFailure("provider")

        assertTrue(breaker.allow("provider"))
    }

    @Test
    fun cooldownAllowsProviderAgain() {
        var now = 0L
        val breaker = AniLabCircuitBreaker(
            failureThreshold = 1,
            cooldownMillis = 1_000L,
            nowMillis = { now },
        )

        breaker.recordFailure("provider")
        assertFalse(breaker.allow("provider"))

        now = 1_000L
        assertTrue(breaker.allow("provider"))
    }

    @Test
    fun cooldownResetsFailureStreak() {
        var now = 0L
        val breaker = AniLabCircuitBreaker(
            failureThreshold = 2,
            cooldownMillis = 1_000L,
            nowMillis = { now },
        )

        breaker.recordFailure("provider")
        breaker.recordFailure("provider")
        assertFalse(breaker.allow("provider"))

        now = 1_000L
        assertTrue(breaker.allow("provider"))

        breaker.recordFailure("provider")
        assertTrue(breaker.allow("provider"))
    }

    @Test
    fun invalidConfigurationIsRejected() {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            AniLabCircuitBreaker(failureThreshold = 0)
        }
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            AniLabCircuitBreaker(cooldownMillis = -1)
        }
    }

    @Test
    fun providersHaveIndependentHealthState() {
        val breaker = AniLabCircuitBreaker(failureThreshold = 1, cooldownMillis = 1_000L)

        breaker.recordFailure("provider-a")

        assertFalse(breaker.allow("provider-a"))
        assertTrue(breaker.allow("provider-b"))
    }
}
