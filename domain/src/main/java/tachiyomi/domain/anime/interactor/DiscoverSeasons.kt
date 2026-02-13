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
        
        // Extract significant words
        val originalWords = rootTitle.lowercase()
            .split(Regex("""\s+"""))
            .filter { it.length > 2 || it.all { char -> char.isDigit() } }
            .toSet()
        
        return try {
            val searchResult = source.getSearchAnime(1, rootTitle, source.getFilterList())
            
            searchResult.animes
                .map { it.toDomainAnime(anime.source) }
                .filter { candidate ->
                    val candidateFullTitle = candidate.title
                    val candidateRoot = SeasonRecognition.getRootTitle(candidateFullTitle)
                    
                    // 1. Block Unrelated Categories (Recaps, Spin-offs, etc)
                    if (SeasonRecognition.isUnrelated(candidateFullTitle)) return@filter false
                    
                    // 2. Strict Word Coverage
                    val candidateWords = candidateFullTitle.lowercase().split(Regex("""\s+""")).toSet()
                    val hasAllWords = originalWords.all { word -> candidateWords.contains(word) }
                    
                    // 3. Word Density check: Candidate shouldn't have too many "extra" significant words
                    // This prevents "Sword Art Online" matching "Sword Art Online Alternative" if Alternative is not in originalWords
                    val candidateSignificantWords = candidateRoot.lowercase().split(Regex("""\s+"""))
                        .filter { it.length > 2 || it.all { char -> char.isDigit() } }
                    
                    val extraWordsCount = candidateSignificantWords.size - originalWords.size
                    
                    // 4. Similarity and combine
                    val similarity = SeasonRecognition.diceCoefficient(rootTitle, candidateRoot)
                    
                    hasAllWords &&
                    similarity > 0.7 && // Lowered slightly since getRootTitle is now more aggressive
                    extraWordsCount <= 2 && // Strictly allow only small title variations
                    candidate.url != anime.url &&
                    !candidateFullTitle.contains("(Dub)", ignoreCase = true) &&
                    !candidateFullTitle.contains("(Sub)", ignoreCase = true)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
