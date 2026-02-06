package eu.kanade.tachiyomi.util.lang

import kotlin.math.max
import kotlin.math.min

object StringSimilarity {

    private val STOP_WORDS = setOf(
        "the", "a", "an", "my", "no", "in", "of", "to", "for", "with", "on", "at", "by", "from",
        "season", "2nd", "3rd", "4th", "5th", "s1", "s2", "s3", "s4", "s5", "tv", "ova", "ona", "movie"
    )

    /**
     * Extracts the most significant keywords from a title for searching.
     * Removes stop words and special characters.
     */
    fun getSearchKeywords(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "") // Remove special chars
            .split(" ")
            .filter { it.isNotBlank() && it !in STOP_WORDS }
            .take(3) // Take up to 3 significant words
            .joinToString(" ")
    }

    /**
     * Token Sort Ratio: Splits strings into words, sorts them, and calculates Levenshtein similarity.
     * This solves the "order-independent" problem while maintaining character-level accuracy.
     */
    fun tokenSortRatio(s1: String, s2: String): Double {
        val t1 = s1.lowercase().replace(Regex("[^a-z0-9\\s]"), "").split(" ").filter { it.isNotBlank() }.sorted().joinToString("")
        val t2 = s2.lowercase().replace(Regex("[^a-z0-9\\s]"), "").split(" ").filter { it.isNotBlank() }.sorted().joinToString("")
        return levenshteinSimilarity(t1, t2)
    }

    fun diceCoefficient(s1: String, s2: String): Double {
        val str1 = s1.lowercase().replace(Regex("[^a-z0-9]"), "")
        val str2 = s2.lowercase().replace(Regex("[^a-z0-9]"), "")
        if (str1 == str2) return 1.0
        if (str1.length < 2 || str2.length < 2) return 0.0
        val s1Bigrams = str1.windowed(2)
        val s2Bigrams = str2.windowed(2).toMutableList()
        var matches = 0
        for (bigram in s1Bigrams) {
            if (s2Bigrams.remove(bigram)) matches++
        }
        return (2.0 * matches) / (s1Bigrams.size + str2.length - 1)
    }

    private fun levenshteinSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
            }
        }
        return 1.0 - (dp[len1][len2].toDouble() / max(len1, len2))
    }
}
