package tachiyomi.domain.anime.service

object SeasonRecognition {

    private const val NUMBER_PATTERN = """([0-9]+)(\.[0-9]+)?(\.?[a-z]+)?"""

    /**
     * All cases with s.xx, s xx, season xx, or sxx
     */
    private val basic = Regex("""(?<=\bs\.|\bs|season) *$NUMBER_PATTERN""", RegexOption.IGNORE_CASE)

    /**
     * Ordinal support: 2nd season, 3rd season, etc.
     */
    private val ordinals = Regex("""(\d+)(?:st|nd|rd|th)\s+(?:season|part)""", RegexOption.IGNORE_CASE)

    /**
     * Part support: Part 1, Part 2
     */
    private val parts = Regex("""(?<=\bpart) *$NUMBER_PATTERN""", RegexOption.IGNORE_CASE)

    /**
     * Format tags support
     */
    private val formatTags = Regex("""\b(OVA|OAV|ONA|Special|Movie|BD|Remux)\b""", RegexOption.IGNORE_CASE)

    /**
     * Example: Boku no Hero Academia 2 -R> 2
     */
    private val number = Regex(NUMBER_PATTERN)

    /**
     * Roman numeral support (Upgraded)
     */
    private val romanNumerals = Regex("""\b(I|II|III|IV|V|VI|VII|VIII|IX|X)\b(?:\s+|$)""", RegexOption.IGNORE_CASE)

    /**
     * Regex to remove tags
     */
    private val tagRegex = Regex("""^\[[^\]]+\]|\[[^\]]+\]\s*${'$'}|^\([^\)]+\)|\([^\)]+\)\s*${'$'}""")

    /**
     * Regex used to remove unwanted qualities and year
     */
    private val unwanted = Regex("""\b\d+p\b|\d+x\d+|Hi10|\(\d+\)|BD|RE|Remux|Dual.Audio""", RegexOption.IGNORE_CASE)

    private val unwantedWhiteSpace = Regex("""\s(?=extra|special|omake)""", RegexOption.IGNORE_CASE)

    private val romanMap = mapOf(
        "I" to 1.0, "II" to 2.0, "III" to 3.0, "IV" to 4.0, "V" to 5.0,
        "VI" to 6.0, "VII" to 7.0, "VIII" to 8.0, "IX" to 9.0, "X" to 10.0
    )

    fun diceCoefficient(s1: String, s2: String): Double {
        val str1 = s1.lowercase().replace(Regex("""\s+"""), "")
        val str2 = s2.lowercase().replace(Regex("""\s+"""), "")
        if (str1 == str2) return 1.0
        if (str1.length < 2 || str2.length < 2) return 0.0

        val set1 = str1.zipWithNext { a, b -> "$a$b" }.toSet()
        val set2 = str2.zipWithNext { a, b -> "$a$b" }.toSet()

        val intersection = set1.intersect(set2).size
        return 2.0 * intersection / (set1.size + set2.size)
    }

    fun getRootTitle(title: String): String {
        return title
            // Only remove explicit season markers, not just colons
            .replace(Regex("""(?i)\s+(?:Season\s+\d+|S\d+|II|III|IV|V|VI|VII|VIII|IX|X|\d+(?:st|nd|rd|th)\s+season)\b"""), "")
            .replace(Regex("""(?i)\s+\(?(?:TV|OAV|OVA|ONA|Special|Movie|BD|Remux)\)?.*"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun parseSeasonNumber(animeTitle: String, seasonName: String, existingNumber: Double? = null): Double {
        if (existingNumber != null && (existingNumber == -2.0 || existingNumber > -1.0)) {
            return existingNumber
        }

        val rootTitle = getRootTitle(animeTitle)
        val cleanSeasonTitle = getRootTitle(seasonName)

        // Only return 1.0 if it's actually the same title as the root
        // If there are extra words (like "Shippuden"), it's not Season 1
        if (cleanSeasonTitle.equals(rootTitle, ignoreCase = true)) {
            // Check if the ORIGINAL name has a number. If it has "2", it's not 1.0.
            if (!Regex("""(?i)\b(?:Season\s*1|S1|Part\s*1)\b""").containsMatchIn(seasonName) &&
                Regex("""(?i)\b(?:Season\s*\d+|S\d+|Part\s*\d+|II|III|IV|V|VI|VII|VIII|IX|X)\b""").containsMatchIn(seasonName)) {
                // Let it fall through to regex detection
            } else {
                return 1.0
            }
        }

        var cleanName = seasonName.lowercase()
            .replace(rootTitle.lowercase(), "").trim() // Use root title for cleaning
            .replace(',', '.')
            .replace('-', '.')
            .replace(unwantedWhiteSpace, "")

        while (tagRegex.containsMatchIn(cleanName)) {
            cleanName = tagRegex.replace(cleanName, "")
        }

        // Remove resolution numbers (1080, 720, etc) to prevent false matches
        cleanName = cleanName.replace(Regex("""\b\d{3,4}p?\b"""), "")

        // 1. Try Ordinal Detection (2nd Season, 3rd Part)
        ordinals.find(cleanName)?.let {
            return it.groups[1]?.value?.toDoubleOrNull() ?: 1.0
        }

        // 2. Try Part Detection (Part 2)
        parts.find(cleanName)?.let {
            return getSeasonNumberFromMatch(it)
        }

        // 3. Try Upgraded Roman Numeral Detection
        romanNumerals.find(cleanName)?.let {
            val roman = it.groups[1]?.value?.uppercase()
            return romanMap[roman] ?: -1.0
        }

        // 4. Check for specific format tags
        if (cleanName.contains("movie", ignoreCase = true)) return -2.0
        if (cleanName.contains("ova", ignoreCase = true) || cleanName.contains("oav", ignoreCase = true)) return -3.0
        if (cleanName.contains("ona", ignoreCase = true)) return -4.0
        if (cleanName.contains("special", ignoreCase = true)) return -5.0

        // 4. Try basic detection (Season 2, S02)
        val basicMatch = basic.find(cleanName)
        if (basicMatch != null) {
            return getSeasonNumberFromMatch(basicMatch)
        }

        val numberMatches = number.findAll(cleanName)
        if (numberMatches.none()) {
            return 1.0
        }

        if (numberMatches.count() > 1) {
            val filteredName = unwanted.replace(cleanName, "")
            basic.find(filteredName)?.let { return getSeasonNumberFromMatch(it) }
            number.find(filteredName)?.let { return getSeasonNumberFromMatch(it) }
        }

        return getSeasonNumberFromMatch(numberMatches.first())
    }

    private fun getSeasonNumberFromMatch(match: MatchResult): Double {
        return try {
            val initial = match.groups[1]?.value?.toDouble() ?: 0.0
            val subSeasonDecimal = match.groups[2]?.value
            val subSeasonAlpha = match.groups[3]?.value
            val addition = checkForDecimal(subSeasonDecimal, subSeasonAlpha)
            initial + addition
        } catch (e: Exception) {
            -1.0
        }
    }

    private fun checkForDecimal(decimal: String?, alpha: String?): Double {
        if (!decimal.isNullOrEmpty()) {
            return decimal.toDoubleOrNull() ?: 0.0
        }

        if (!alpha.isNullOrEmpty()) {
            val alphaLower = alpha.lowercase()
            return when {
                alphaLower.contains("extra") -> 0.99
                alphaLower.contains("omake") -> 0.98
                alphaLower.contains("special") -> 0.97
                else -> {
                    val trimmedAlpha = alphaLower.trimStart('.')
                    if (trimmedAlpha.length == 1) {
                        val num = trimmedAlpha[0].code - ('a'.code - 1)
                        if (num in 1..9) num / 10.0 else 0.0
                    } else 0.0
                }
            }
        }

        return 0.0
    }
}