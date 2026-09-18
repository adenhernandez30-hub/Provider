package com.kakaanime.provider

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProviderMappingResolverTest {

    private val identity = CanonicalAnimeIdentity(
        anilistId = 21,
        title = "One Piece",
        alternativeTitles = listOf("ONE PIECE"),
        year = 1999,
        format = "TV"
    )

    @Test
    fun resolvesMappingsByConfidenceAndDeduplicatesProviderIds() = runBlocking {
        val source = object : ProviderMappingSource {
            override suspend fun findMappings(identity: CanonicalAnimeIdentity) =
                listOf(
                    ProviderMapping(21, "samehadaku", "op", confidence = 0.80),
                    ProviderMapping(21, "otakudesu", "op-1", confidence = 0.95),
                    ProviderMapping(21, "samehadaku", "op", confidence = 0.70),
                    ProviderMapping(999, "ignored", "wrong", confidence = 1.0)
                )
        }

        val result = ProviderMappingResolver(source).resolve(identity)

        assertEquals(
            listOf(
                ProviderAnimeRef("otakudesu", "op-1", 0.95),
                ProviderAnimeRef("samehadaku", "op", 0.80)
            ),
            result
        )
    }

    @Test
    fun rejectsInvalidCanonicalIdentity() {
        assertFailsWith<IllegalArgumentException> {
            CanonicalAnimeIdentity(anilistId = 0, title = "One Piece")
        }
        assertFailsWith<IllegalArgumentException> {
            CanonicalAnimeIdentity(anilistId = 21, title = " ")
        }
    }

    @Test
    fun rejectsInvalidMappingConfidence() {
        assertFailsWith<IllegalArgumentException> {
            ProviderMapping(21, "provider", "anime", confidence = 1.1)
        }
    }
}
