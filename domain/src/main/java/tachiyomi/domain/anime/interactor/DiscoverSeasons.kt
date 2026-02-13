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
        
        val rootTitle = SeasonRecognition.getRootTitle(anime.title)
        
        return try {
            val searchResult = source.getSearchAnime(1, rootTitle, source.getFilterList())
            
            searchResult.animes
                .map { it.toDomainAnime(anime.source) }
                .filter { candidate ->
                    val candidateRoot = SeasonRecognition.getRootTitle(candidate.title)
                    
                    // 1. Root Similarity: Must be very similar to the original root (e.g. 85%+)
                    val similarity = SeasonRecognition.diceCoefficient(rootTitle, candidateRoot)
                    
                    // 2. Strict Constraints
                    similarity > 0.8 && // High threshold to block unrelated stuff
                    candidate.url != anime.url && // Not the same anime
                    // Ensure the candidate isn't just a completely different series that contains the root words
                    (candidateRoot.contains(rootTitle, ignoreCase = true) || rootTitle.contains(candidateRoot, ignoreCase = true))
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
