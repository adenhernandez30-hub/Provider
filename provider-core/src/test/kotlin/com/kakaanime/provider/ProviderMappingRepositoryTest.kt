package com.kakaanime.provider

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProviderMappingRepositoryTest {

    private fun mapping(id: Int = 21, provider: String = "otakudesu") =
        ProviderMapping(id, provider, "anime-21", confidence = 0.9)

    @Test
    fun cachePreventsRepeatedSourceCalls() = runBlocking {
        var calls = 0
        val source = object : ProviderMappingSource {
            override suspend fun findMappings(identity: CanonicalAnimeIdentity): List<ProviderMapping> {
                calls++
                return listOf(mapping())
            }
        }
        val cache = ProviderMappingCache(this, ttlMs = 60_000)
        val repository = ProviderMappingRepository(source, cache)

        val first = repository.findMappings(identity())
        val second = repository.findMappings(identity())

        assertEquals(first, second)
        assertEquals(1, calls)
    }

    @Test
    fun concurrentMissesShareOneSourceCall() = runBlocking {
        var calls = 0
        val source = object : ProviderMappingSource {
            override suspend fun findMappings(identity: CanonicalAnimeIdentity): List<ProviderMapping> {
                calls++
                delay(50)
                return listOf(mapping())
            }
        }
        val cache = ProviderMappingCache(this, ttlMs = 60_000)
        val repository = ProviderMappingRepository(source, cache)

        val first = async { repository.findMappings(identity()) }
        val second = async { repository.findMappings(identity()) }

        assertEquals(first.await(), second.await())
        assertEquals(1, calls)
    }

    @Test
    fun persistedMappingsAvoidSourceCall() = runBlocking {
        var sourceCalls = 0
        var storeGets = 0
        val stored = listOf(mapping())
        val source = object : ProviderMappingSource {
            override suspend fun findMappings(identity: CanonicalAnimeIdentity): List<ProviderMapping> {
                sourceCalls++
                return emptyList()
            }
        }
        val store = object : ProviderMappingStore {
            override suspend fun get(anilistId: Int): List<ProviderMapping>? {
                storeGets++
                return stored
            }

            override suspend fun put(anilistId: Int, mappings: List<ProviderMapping>) {
                error("put should not be called for a persisted hit")
            }
        }

        val repository = ProviderMappingRepository(
            source = source,
            cache = ProviderMappingCache(this, ttlMs = 60_000),
            store = store,
        )

        assertEquals(stored, repository.findMappings(identity()))
        assertEquals(0, sourceCalls)
        assertEquals(1, storeGets)
    }

    @Test
    fun expiredCacheEntryLoadsAgain() = runBlocking {
        var now = 1_000L
        var calls = 0
        val source = object : ProviderMappingSource {
            override suspend fun findMappings(identity: CanonicalAnimeIdentity): List<ProviderMapping> {
                calls++
                return listOf(mapping())
            }
        }
        val cache = ProviderMappingCache(
            scope = this,
            ttlMs = 100,
            nowMs = { now },
        )
        val repository = ProviderMappingRepository(source, cache)

        repository.findMappings(identity())
        now = 1_101L
        repository.findMappings(identity())

        assertEquals(2, calls)
    }

    @Test
    fun sourceFailureDoesNotLeaveStuckInflightEntry() = runBlocking {
        var calls = 0
        val source = object : ProviderMappingSource {
            override suspend fun findMappings(identity: CanonicalAnimeIdentity): List<ProviderMapping> {
                calls++
                if (calls == 1) error("temporary")
                return listOf(mapping())
            }
        }
        val repository = ProviderMappingRepository(
            source = source,
            cache = ProviderMappingCache(this, ttlMs = 60_000),
        )

        assertNull(runCatching { repository.findMappings(identity()) }.getOrNull())
        assertEquals(listOf(mapping()), repository.findMappings(identity()))
        assertEquals(2, calls)
    }

    private fun identity() = CanonicalAnimeIdentity(
        anilistId = 21,
        title = "One Piece",
    )
}
