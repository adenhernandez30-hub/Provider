package com.kakaanime.provider

/**
 * Orders providers using configured priority plus observed health/latency.
 *
 * Unknown providers preserve registry priority. Providers with repeated
 * failures are moved behind usable providers; among otherwise healthy
 * providers, lower observed latency is preferred.
 */
object ProviderRoutingPolicy {

    fun order(
        providers: List<AnimeProvider>,
        healthMonitor: ProviderHealthMonitor,
    ): List<AnimeProvider> =
        providers.withIndex()
            .sortedWith(
                compareBy<IndexedValue<AnimeProvider>> { failureBucket(healthMonitor.snapshot(it.value.id)) }
                    .thenBy { latencyBucket(healthMonitor.snapshot(it.value.id)) }
                    .thenBy { it.value.priority }
                    .thenBy { it.index }
            )
            .map { it.value }

    private fun failureBucket(snapshot: ProviderHealthSnapshot): Int = when (snapshot.status) {
        "FAILING" -> 3
        "DEGRADED" -> 2
        "UNKNOWN" -> 1
        else -> 0
    }

    private fun latencyBucket(snapshot: ProviderHealthSnapshot): Long =
        if (snapshot.lastCheckedAt == 0L) Long.MAX_VALUE else snapshot.averageLatencyMs
}
