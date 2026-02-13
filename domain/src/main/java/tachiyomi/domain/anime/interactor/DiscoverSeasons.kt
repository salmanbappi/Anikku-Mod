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
        
        // Split into words for order and coverage check
        val originalWords = rootTitle.lowercase()
            .split(Regex("""[\s\:\-\–\—\(\)\[\]\.]+"""))
            .filter { it.isNotBlank() && it.length > 1 } // Ignore single chars
        
        if (originalWords.isEmpty()) return emptyList()
        
        return try {
            // Search with root title
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

                    // 1. Strict Prefix Word Check: Candidate MUST start with the first word of original
                    if (candidateWords.firstOrNull() != originalWords.first()) return@filter false

                    // 2. Word Order Enforcer: All original words must appear in the candidate in the same relative order
                    var lastIndex = -1
                    val hasInOrder = originalWords.all { word ->
                        val index = candidateWords.indexOf(word)
                        if (index > lastIndex) {
                            lastIndex = index
                            true
                        } else false
                    }
                    if (!hasInOrder) return@filter false

                    // 3. Advanced Similarity: Use Jaro-Winkler for prefix sensitivity
                    val similarity = SeasonRecognition.jaroWinklerSimilarity(rootTitle, candidateRoot)
                    
                    // 4. Inclusive Matching: Allow if candidate starts with original root (common for sequels)
                    val startsWithRoot = candidateFullTitle.lowercase().startsWith(rootTitle.lowercase())

                    (similarity > 0.85 || startsWithRoot) &&
                    candidate.url != anime.url &&
                    !candidateFullTitle.contains("(Dub)", ignoreCase = true) &&
                    !candidateFullTitle.contains("(Sub)", ignoreCase = true)
                }
                .sortedBy { it.title.length } // Shortest titles (main seasons) first
        } catch (e: Exception) {
            emptyList()
        }
    }
}
