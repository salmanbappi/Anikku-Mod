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
            .filter { it.isNotBlank() && it.length > 1 }
        
        if (originalWords.isEmpty()) return emptyList()
        
        return try {
            val searchResult = source.getSearchAnime(1, rootTitle, source.getFilterList())
            
            val candidates = searchResult.animes
                .map { it.toDomainAnime(anime.source) }
                .filter { candidate ->
                    val candidateFullTitle = candidate.title
                    val candidateRoot = SeasonRecognition.getRootTitle(candidateFullTitle)
                    
                    if (SeasonRecognition.isUnrelated(candidateFullTitle)) return@filter false
                    
                    val candidateWords = candidateFullTitle.lowercase()
                        .split(Regex("""[\s\:\-\–\—\(\)\[\]\.]+"""))
                        .filter { it.isNotBlank() }

                    if (candidateWords.firstOrNull() != originalWords.first()) return@filter false

                    var lastIndex = -1
                    val hasInOrder = originalWords.all { word ->
                        val index = candidateWords.indexOf(word)
                        if (index > lastIndex) {
                            lastIndex = index
                            true
                        } else false
                    }
                    if (!hasInOrder) return@filter false

                    val similarity = SeasonRecognition.jaroWinklerSimilarity(rootTitle, candidateRoot)
                    val startsWithRoot = candidateFullTitle.lowercase().startsWith(rootTitle.lowercase())

                    (similarity > 0.85 || startsWithRoot) &&
                    candidate.url != anime.url &&
                    !candidateFullTitle.contains("(Dub)", ignoreCase = true) &&
                    !candidateFullTitle.contains("(Sub)", ignoreCase = true)
                }
                .take(10) // Only process top 10 candidates to keep it fast

            // 5. Author Signature Check: Fetch details for top candidates to verify author
            candidates.filter { candidate ->
                try {
                    val details = source.getAnimeDetails(candidate.toSAnime())
                    val candidateAuthor = details.author?.lowercase()?.trim()
                    val originalAuthor = anime.author?.lowercase()?.trim()
                    
                    // If either has no author info, allow it (some sources are incomplete)
                    // otherwise, they MUST share at least one part of the author's name
                    if (candidateAuthor.isNullOrBlank() || originalAuthor.isNullOrBlank()) {
                        true
                    } else {
                        val originalAuthorWords = originalAuthor.split(Regex("""\s+""")).filter { it.length > 2 }.toSet()
                        val candidateAuthorWords = candidateAuthor.split(Regex("""\s+""")).filter { it.length > 2 }.toSet()
                        
                        // Check if they share any significant creator name
                        originalAuthorWords.intersect(candidateAuthorWords).isNotEmpty() ||
                        candidateAuthor.contains(originalAuthor) || 
                        originalAuthor.contains(candidateAuthor)
                    }
                } catch (e: Exception) {
                    true // If network fails, don't block
                }
            }.sortedBy { it.title.length }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
