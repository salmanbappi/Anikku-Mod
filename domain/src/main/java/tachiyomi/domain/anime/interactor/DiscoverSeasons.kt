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
                
                // Word Coverage Check: Candidate must contain EVERY word from the original root
                val candidateWordSet = SeasonRecognition.getWordSet(candidateFullTitle)
                if (!candidateWordSet.containsAll(originalWordSet)) return@filter false
                
                // Sequence check: First word must match
                val cWords = candidateFullTitle.lowercase().split(Regex("""\s+""")).filter { it.isNotBlank() }
                val oWords = rootTitle.lowercase().split(Regex("""\s+""")).filter { it.isNotBlank() }
                if (cWords.firstOrNull() != oWords.firstOrNull()) return@filter false

                sAnime.url != anime.url
            }.take(5)

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
