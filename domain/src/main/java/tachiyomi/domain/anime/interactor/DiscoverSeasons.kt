package tachiyomi.domain.anime.interactor

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import tachiyomi.domain.anime.model.toDomainAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.repository.AnimeRepository
import tachiyomi.domain.anime.service.SeasonRecognition
import tachiyomi.domain.source.service.SourceManager

class DiscoverSeasons(
    private val sourceManager: SourceManager,
    private val animeRepository: AnimeRepository,
) {
    suspend fun await(anime: Anime): List<Anime> {
        val source = sourceManager.get(anime.source) as? AnimeCatalogueSource ?: return emptyList()
        
        val originalFullTitle = anime.title
        val rootTitle = SeasonRecognition.getRootTitle(originalFullTitle)
        
        if (rootTitle.length < 3) return emptyList()
        
        return try {
            val searchResult = source.getSearchAnime(1, rootTitle, source.getFilterList())
            
            // 1. Strict Word Coverage Lock
            val originalWordSet = SeasonRecognition.getWordSet(rootTitle)
            if (originalWordSet.isEmpty()) return emptyList()

            val candidates = searchResult.animes.filter { sAnime ->
                val candidateFullTitle = sAnime.title
                
                // NO-TOLERANCE FILTERS
                if (SeasonRecognition.isUnrelated(originalFullTitle, candidateFullTitle)) return@filter false
                
                // Smart Fuzzy Match: Allow variations like "Frieren" vs "Sousou no Frieren"
                // 1. Check if the candidate contains the MAIN keyword (longest word from root)
                val longestWord = rootTitle.split(Regex("""\s+""")).maxByOrNull { it.length } ?: ""
                if (longestWord.length > 3 && !candidateFullTitle.contains(longestWord, ignoreCase = true)) {
                     // If the most unique word isn't there, it's likely wrong.
                     // Exception: deeply localized titles (ignoring for now to prevent noise)
                     return@filter false
                }

                // 2. Similarity Check (Dice Coefficient)
                // We accept > 0.4 because sequels often add many words ("...Season 2 Part 3")
                // which lowers the score against the short root title.
                val similarity = SeasonRecognition.diceCoefficient(rootTitle, candidateFullTitle)
                if (similarity < 0.4 && !candidateFullTitle.contains(rootTitle, ignoreCase = true)) return@filter false
                
                true
            }.take(10)

            val verified = mutableListOf<tachiyomi.domain.anime.model.Anime>()
            
            // 2. Mandatory Metadata Signature Verification
            for (sAnime in candidates) {
                try {
                    val details = source.getAnimeDetails(sAnime)
                    val candidateAuthor = details.author?.lowercase()?.trim()
                    val originalAuthor = anime.author?.lowercase()?.trim()
                    
                    // If metadata exists, it MUST match significantly
                    val isVerified = if (!originalAuthor.isNullOrBlank() && !candidateAuthor.isNullOrBlank()) {
                        val oAuthWords = originalAuthor.split(Regex("""\s+""")).filter { it.length > 2 }.toSet()
                        val cAuthWords = candidateAuthor.split(Regex("""\s+""")).filter { it.length > 2 }.toSet()
                        oAuthWords.intersect(cAuthWords).isNotEmpty() || candidateAuthor.contains(originalAuthor)
                    } else if (!originalAuthor.isNullOrBlank() && candidateAuthor.isNullOrBlank()) {
                        false // Original has author but candidate doesn't? Suspicious, block it.
                    } else {
                        true // No metadata to compare, trust the strict title lock
                    }
                    
                    if (isVerified) {
                        verified.add(sAnime.toDomainAnime(anime.source))
                    }
                } catch (e: Exception) {
                    // Fail-safe: if network fails, don't show the candidate to be safe
                }
            }

            verified.sortedBy { it.title.length }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
