package eu.kanade.tachiyomi.util.lang

import kotlin.math.max
import kotlin.math.min

object StringSimilarity {

    /**
     * Calculates the Jaro-Winkler similarity between two strings.
     * Result is between 0.0 (no similarity) and 1.0 (exact match).
     */
    fun jaroWinkler(s1: String, s2: String): Double {
        val m = 0.1 // scaling factor
        val jaro = jaro(s1, s2)
        val prefix = s1.commonPrefixWith(s2).length.coerceAtMost(4)
        return jaro + (prefix * m * (1 - jaro))
    }

    private fun jaro(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0 || len2 == 0) return 0.0
        
        val matchDistance = (max(len1, len2) / 2) - 1
        val s1Matches = BooleanArray(len1)
        val s2Matches = BooleanArray(len2)
        var matches = 0.0
        var transpositions = 0.0

        for (i in 0 until len1) {
            val start = max(0, i - matchDistance)
            val end = min(i + matchDistance + 1, len2)
            for (j in start until end) {
                if (s2Matches[j]) continue
                if (s1[i].lowercaseChar() != s2[j].lowercaseChar()) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }
        if (matches == 0.0) return 0.0

        var k = 0
        for (i in 0 until len1) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i].lowercaseChar() != s2[k].lowercaseChar()) transpositions++
            k++
        }

        return ((matches / len1) + (matches / len2) + ((matches - transpositions / 2) / matches)) / 3.0
    }
}
