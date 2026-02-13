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
        
        // 1. Core Signature: The words that define this franchise
        val originalWords = rootTitle.lowercase()
            .split(Regex("""[\s\:\-\–\—\(\)\[\]\.]+"""))
            .filter { it.isNotBlank() && it.length > 1 }
        
        if (originalWords.isEmpty()) return emptyList()
        val firstWord = originalWords.first()
        
        return try {
            val searchResult = source.getSearchAnime(1, rootTitle, source.getFilterList())
            
            // 2. Pre-filter by Title (Fast)
            val titleMatched = searchResult.animes.filter { sAnime ->
                val candidateFullTitle = sAnime.title
                val candidateRoot = SeasonRecognition.getRootTitle(candidateFullTitle)
                
                if (SeasonRecognition.isUnrelated(candidateFullTitle)) return@filter false
                
                val candidateWords = candidateFullTitle.lowercase()
                    .split(Regex("""[\s\:\-\–\—\(\)\[\]\.]+"""))
                    .filter { it.isNotBlank() }

                // MUST start with same first word (kills Attack No.1)
                if (candidateWords.firstOrNull() != firstWord) return@filter false

                // Sequence Check: Original words must appear in same order
                var lastIndex = -1
                val inOrder = originalWords.all { word ->
                    val index = candidateWords.indexOf(word)
                    if (index > lastIndex) {
                        lastIndex = index
                        true
                    } else false
                }
                if (!inOrder) return@filter false

                val similarity = SeasonRecognition.jaroWinklerSimilarity(rootTitle, candidateRoot)
                val startsWithRoot = candidateFullTitle.lowercase().startsWith(rootTitle.lowercase())

                (similarity > 0.8 || startsWithRoot) && sAnime.url != anime.url
            }.take(8) // Limit heavy metadata checks to top 8

            // 3. Metadata Signature Lock (Slow, Accurate)
            val verified = mutableListOf<tachiyomi.domain.anime.model.Anime>()
            for (sAnime in titleMatched) {
                try {
                    val details = source.getAnimeDetails(sAnime)
                    val candidateAuthor = details.author?.lowercase()?.trim()
                    val originalAuthor = anime.author?.lowercase()?.trim()
                    
                    val isAuthorMatch = when {
                        candidateAuthor.isNullOrBlank() || originalAuthor.isNullOrBlank() -> true
                        else -> {
                            val cAuthWords = candidateAuthor.split(Regex("""\s+""")).filter { it.length > 2 }.toSet()
                            val oAuthWords = originalAuthor.split(Regex("""\s+""")).filter { it.length > 2 }.toSet()
                            cAuthWords.intersect(oAuthWords).isNotEmpty() || 
                            candidateAuthor.contains(originalAuthor) || 
                            originalAuthor.contains(candidateAuthor)
                        }
                    }
                    
                    if (isAuthorMatch) {
                        verified.add(sAnime.toDomainAnime(anime.source))
                    }
                } catch (e: Exception) {
                    // Fallback to title only if network fails
                    verified.add(sAnime.toDomainAnime(anime.source))
                }
            }

            verified.sortedBy { it.title.length }
        } catch (e: Exception) {
            emptyList()
        }
    }
}