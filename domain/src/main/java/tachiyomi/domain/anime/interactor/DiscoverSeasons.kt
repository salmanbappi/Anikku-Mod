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
        
        // UPGRADE: Keep ALL words. Every word in a title is significant for short titles.
        val originalWords = rootTitle.lowercase()
            .split(Regex("""[\s\:\-\–\—\(\)\[\]\.]+"""))
            .filter { it.isNotBlank() }
            .toSet()
        
        if (originalWords.isEmpty()) return emptyList()

        val firstWord = originalWords.first()
        
        return try {
            val searchResult = source.getSearchAnime(1, rootTitle, source.getFilterList())
            
            searchResult.animes
                .map { it.toDomainAnime(anime.source) }
                .filter { candidate ->
                    val candidateFullTitle = candidate.title
                    val candidateRoot = SeasonRecognition.getRootTitle(candidateFullTitle)
                    
                    if (SeasonRecognition.isUnrelated(candidateFullTitle)) return@filter false
                    
                    val candidateWords = candidateFullTitle.lowercase()
                        .split(Regex("""[\s\:\-\–\—\(\)\[\]\.]+"""))
                        .filter { it.isNotBlank() }
                        .toSet()
                    
                    // 1. Strict Bidirectional Word Coverage
                    // Candidate must contain ALL words from the original root
                    val hasAllOriginalWords = originalWords.all { word -> candidateWords.contains(word) }
                    
                    // 2. Strict Prefix Match: Most anime sequels start with the same first word
                    // This prevents "Attack on Titan" matching "Attack No.1"
                    val startsWithSameWord = candidateFullTitle.lowercase().trim().startsWith(firstWord)
                    
                    // 3. Similarity check
                    val similarity = SeasonRecognition.diceCoefficient(rootTitle, candidateRoot)
                    
                    // 4. Combine constraints
                    hasAllOriginalWords &&
                    startsWithSameWord &&
                    similarity > 0.75 &&
                    candidate.url != anime.url &&
                    !candidateFullTitle.contains("(Dub)", ignoreCase = true) &&
                    !candidateFullTitle.contains("(Sub)", ignoreCase = true)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
