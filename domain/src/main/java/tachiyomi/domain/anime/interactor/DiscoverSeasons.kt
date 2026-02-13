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
            
            // 1. Strict Title Filtering
            val candidates = searchResult.animes.filter { sAnime ->
                val candidateFullTitle = sAnime.title
                
                // HARD LOCK: Must contain the root title as a substring
                if (!candidateFullTitle.contains(rootTitle, ignoreCase = true)) return@filter false
                
                // Block generic noise
                if (SeasonRecognition.isUnrelated(candidateFullTitle)) return@filter false
                
                // Sequence check: Ensure first word matches exactly
                val originalFirstWord = rootTitle.split(Regex("""\s+""")).firstOrNull()?.lowercase()
                val candidateFirstWord = candidateFullTitle.split(Regex("""\s+""")).firstOrNull()?.lowercase()
                
                candidateFirstWord == originalFirstWord && sAnime.url != anime.url
            }.take(6)

            val verified = mutableListOf<tachiyomi.domain.anime.model.Anime>()
            
            // 2. Hard Metadata Signature Verification
            for (sAnime in candidates) {
                try {
                    val details = source.getAnimeDetails(sAnime)
                    val candidateAuthor = details.author?.lowercase()?.trim()
                    val originalAuthor = anime.author?.lowercase()?.trim()
                    val originalArtist = anime.artist?.lowercase()?.trim()
                    
                    // If original has author info, candidate MUST match it.
                    val authorMatch = if (!originalAuthor.isNullOrBlank()) {
                        val oWords = originalAuthor.split(Regex("""\s+""")).filter { it.length > 2 }
                        val cWords = candidateAuthor?.split(Regex("""\s+"""))?.filter { it.length > 2 } ?: emptyList()
                        oWords.any { ow -> cWords.contains(ow) } || candidateAuthor?.contains(originalAuthor) == true
                    } else true

                    if (authorMatch) {
                        verified.add(sAnime.toDomainAnime(anime.source))
                    }
                } catch (e: Exception) {
                    // Only fallback to title if the original has NO author info at all
                    if (anime.author.isNullOrBlank()) {
                        verified.add(sAnime.toDomainAnime(anime.source))
                    }
                }
            }

            verified.sortedBy { it.title.length }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
