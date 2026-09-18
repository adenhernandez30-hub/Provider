package com.kakaanime.provider

import kotlin.test.Test
import kotlin.test.assertEquals

class ProviderRoutingPolicyTest {

    private fun provider(id: String, priority: Int) = object : AnimeProvider {
        override val id = id
        override val name = id
        override val priority = priority
        override suspend fun search(query: String) = emptyList<ProviderAnime>()
        override suspend fun getAnime(animeId: String) = null
        override suspend fun getEpisodes(animeId: String) = emptyList<ProviderEpisode>()
        override suspend fun getStreams(animeId: String, episodeNumber: Int) = emptyList<ProviderStream>()
    }

    @Test
    fun unknownProvidersKeepConfiguredPriority() {
        val monitor = ProviderHealthMonitor()
        val result = ProviderRoutingPolicy.order(
            listOf(provider("slow-priority-1", 1), provider("priority-2", 2)),
            monitor,
        )
        assertEquals(listOf("slow-priority-1", "priority-2"), result.map { it.id })
    }

    @Test
    fun failingProviderMovesBehindHealthyProvider() {
        val monitor = ProviderHealthMonitor()
        repeat(3) { monitor.recordFailure("failing", 10, RuntimeException("boom")) }
        monitor.recordSuccess("healthy", 80)

        val result = ProviderRoutingPolicy.order(
            listOf(provider("failing", 1), provider("healthy", 2)),
            monitor,
        )

        assertEquals(listOf("healthy", "failing"), result.map { it.id })
    }

    @Test
    fun lowerLatencyHealthyProviderComesFirst() {
        val monitor = ProviderHealthMonitor()
        monitor.recordSuccess("slow", 500)
        monitor.recordSuccess("fast", 100)

        val result = ProviderRoutingPolicy.order(
            listOf(provider("slow", 1), provider("fast", 2)),
            monitor,
        )

        assertEquals(listOf("fast", "slow"), result.map { it.id })
    }
}
