package com.kakaanime.provider

/**
 * Scores a provider result against a canonical identity.
 *
 * The score is a matching signal, not a routing priority. Callers can apply
 * their own minimum confidence threshold.
 */
object AnimeMatchScorer {

    fun score(identity: CanonicalAnimeIdentity, candidate: ProviderAnime): Double {
        val titles = buildList {
            add(identity.title)
            addAll(identity.alternativeTitles)
        }.map(::normalizeTitle).filter(String::isNotBlank).distinct()

        val candidateTitles = buildList {
            add(candidate.title)
            addAll(candidate.alternativeTitles)
            addAll(candidate.searchAliases)
        }.map(::normalizeTitle).filter(String::isNotBlank).distinct()

        if (titles.isEmpty() || candidateTitles.isEmpty()) return 0.0

        val titleScore = titles.maxOf { expected ->
            candidateTitles.maxOf { actual -> titleSimilarity(expected, actual) }
        }

        var score = titleScore
        if (identity.year != null && candidate.year != null && identity.year == candidate.year) {
            score += 0.05
        }
        if (identity.seasonNumber != null && candidate.seasonNumber == identity.seasonNumber) {
            score += 0.10
        }

        return score.coerceIn(0.0, 1.0)
    }

    fun normalizeTitle(value: String): String =
        value
            .lowercase()
            .replace("&", " and ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\s+"), " ")

    private fun titleSimilarity(expected: String, actual: String): Double {
        if (expected == actual) return 1.0
        val expectedTokens = expected.split(' ').filter(String::isNotBlank).toSet()
        val actualTokens = actual.split(' ').filter(String::isNotBlank).toSet()
        if (expectedTokens.isEmpty() || actualTokens.isEmpty()) return 0.0

        val intersection = expectedTokens.intersect(actualTokens).size.toDouble()
        val union = expectedTokens.union(actualTokens).size.toDouble()
        return if (union == 0.0) 0.0 else intersection / union
    }
}
