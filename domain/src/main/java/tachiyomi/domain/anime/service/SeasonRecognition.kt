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
    private val ordinals = Regex("""(\d+)(?:st|nd|rd|th)\s+season""", RegexOption.IGNORE_CASE)

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

    fun getRootTitle(title: String): String {
        return title
            .replace(Regex("""(?i)\s+(:|--|–).*"""), "") // Remove subtitles after colons/dashes
            // Only remove standalone numbers or explicit season markers to avoid breaking titles like "7Seeds"
            .replace(Regex("""(?i)\s+(?:Season\s+\d+|S\d+|II|III|IV|V|VI|VII|VIII|IX|X)\b"""), "")
            .replace(Regex("""(?i)\s+\(?(?:TV|OAV|OVA|ONA|Special|Movie|BD|Remux)\)?.*"""), "") // Remove format tags
            .trim()
    }

    fun parseSeasonNumber(animeTitle: String, seasonName: String, existingNumber: Double? = null): Double {
        if (existingNumber != null && (existingNumber == -2.0 || existingNumber > -1.0)) {
            return existingNumber
        }

        val rootTitle = getRootTitle(animeTitle)
        val cleanSeasonTitle = getRootTitle(seasonName)

        // UPGRADE: If the name matches the root title exactly, it's Season 1
        if (cleanSeasonTitle.equals(rootTitle, ignoreCase = true)) {
            return 1.0
        }

        var cleanName = seasonName.lowercase()
            .replace(animeTitle.lowercase(), "").trim()
            .replace(',', '.')
            .replace('-', '.')
            .replace(unwantedWhiteSpace, "")

        while (tagRegex.containsMatchIn(cleanName)) {
            cleanName = tagRegex.replace(cleanName, "")
        }

        // Remove resolution numbers (1080, 720, etc) to prevent false matches
        cleanName = cleanName.replace(Regex("""\b\d{3,4}p?\b"""), "")

        // 1. Try Ordinal Detection (2nd Season)
        ordinals.find(cleanName)?.let {
            return it.groups[1]?.value?.toDoubleOrNull() ?: 1.0
        }

        // 2. Try Upgraded Roman Numeral Detection
        romanNumerals.find(cleanName)?.let {
            val roman = it.groups[1]?.value?.uppercase()
            return romanMap[roman] ?: -1.0
        }

        // 3. Check for specific format tags
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