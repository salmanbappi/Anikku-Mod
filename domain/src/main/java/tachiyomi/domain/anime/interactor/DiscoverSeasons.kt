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
        
        // Extract significant words (ignoring particles like 'of', 'the', etc)
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
                    
                    // 1. Strict Word Coverage: Candidate must contain ALL significant words of the original root
                    val candidateWords = candidateFullTitle.lowercase().split(Regex("""\s+""")).toSet()
                    val hasAllWords = originalWords.all { word -> candidateWords.contains(word) }
                    
                    // 2. Similarity Check
                    val similarity = SeasonRecognition.diceCoefficient(rootTitle, candidateRoot)
                    
                    // 3. Combine constraints
                    hasAllWords &&
                    similarity > 0.8 &&
                    candidate.url != anime.url &&
                    // Block dubs/subs versions if they are separate entries
                    !candidateFullTitle.contains("(Dub)", ignoreCase = true) &&
                    !candidateFullTitle.contains("(Sub)", ignoreCase = true)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
