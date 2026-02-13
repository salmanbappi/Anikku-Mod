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
            
            // 5. Combined Title and Author Check
            searchResult.animes
                .filter { sAnime ->
                    val candidateFullTitle = sAnime.title
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
                    sAnime.url != anime.url &&
                    !candidateFullTitle.contains("(Dub)", ignoreCase = true) &&
                    !candidateFullTitle.contains("(Sub)", ignoreCase = true)
                }
                .take(10)
                .filter { sAnime ->
                    try {
                        // Fetch details to verify author
                        val details = source.getAnimeDetails(sAnime)
                        val candidateAuthor = details.author?.lowercase()?.trim()
                        val originalAuthor = anime.author?.lowercase()?.trim()
                        
                        if (candidateAuthor.isNullOrBlank() || originalAuthor.isNullOrBlank()) {
                            true
                        } else {
                            val originalAuthorWords = originalAuthor.split(Regex("""\s+""")).filter { it.length > 2 }.toSet()
                            val candidateAuthorWords = candidateAuthor.split(Regex("""\s+""")).filter { it.length > 2 }.toSet()
                            
                            originalAuthorWords.intersect(candidateAuthorWords).isNotEmpty() ||
                            candidateAuthor.contains(originalAuthor) || 
                            originalAuthor.contains(candidateAuthor)
                        }
                    } catch (e: Exception) {
                        true
                    }
                }
                .map { it.toDomainAnime(anime.source) }
                .sortedBy { it.title.length }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
