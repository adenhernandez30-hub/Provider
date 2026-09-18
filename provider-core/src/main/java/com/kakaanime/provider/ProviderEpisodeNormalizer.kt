package com.kakaanime.provider

/**
 * Normalizes provider episode lists without changing provider-specific IDs.
 *
 * Invalid/non-positive episodes are dropped, duplicates are collapsed by
 * season + episode number, and the strongest available metadata is retained.
 */
object ProviderEpisodeNormalizer {

    fun normalize(
        episodes: List<ProviderEpisode>,
        seasonNumber: Int? = null,
    ): List<ProviderEpisode> =
        episodes
            .asSequence()
            .filter { it.number > 0 }
            .filter { seasonNumber == null || it.seasonNumber == null || it.seasonNumber == seasonNumber }
            .groupBy { episode ->
                EpisodeKey(
                    seasonNumber = episode.seasonNumber ?: seasonNumber,
                    number = episode.number,
                )
            }
            .values
            .map { duplicates -> duplicates.reduce(::merge) }
            .sortedWith(
                compareBy<ProviderEpisode> { it.seasonNumber ?: Int.MAX_VALUE }
                    .thenBy { it.number }
            )
            .toList()

    private fun merge(left: ProviderEpisode, right: ProviderEpisode): ProviderEpisode {
        fun <T> firstNonNull(a: T?, b: T?): T? = a ?: b
        return left.copy(
            title = firstNonNull(left.title, right.title),
            thumbnailUrl = firstNonNull(left.thumbnailUrl, right.thumbnailUrl),
            releasedAt = firstNonNull(left.releasedAt, right.releasedAt),
            animeGroupId = if (left.animeGroupId != left.animeId) left.animeGroupId else right.animeGroupId,
            seasonTitle = firstNonNull(left.seasonTitle, right.seasonTitle),
            availability = when {
                left.availability == EpisodeAvailability.AVAILABLE -> left.availability
                right.availability == EpisodeAvailability.AVAILABLE -> right.availability
                left.availability == EpisodeAvailability.NOT_RELEASED -> left.availability
                else -> right.availability
            },
            isNew = left.isNew || right.isNew,
        )
    }

    private data class EpisodeKey(
        val seasonNumber: Int?,
        val number: Int,
    )
}
