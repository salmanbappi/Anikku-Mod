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
        
        // 1. Get Root Title (e.g. "Boku no Hero Academia")
        val rootTitle = getRootTitle(anime.title)
        
        return try {
            // 2. Search source for root title
            val searchResult = source.getSearchAnime(1, rootTitle, source.getFilterList())
            
            // 3. Filter results that are actually seasons of this anime
            searchResult.animes
                .map { it.toDomainAnime(anime.source) }
                .filter { candidate ->
                    // Must have same root title and a valid season number
                    getRootTitle(candidate.title).lowercase() == rootTitle.lowercase() &&
                    candidate.url != anime.url
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getRootTitle(title: String): String {
        // Simple logic to remove "Season X", "S2", etc.
        return title.replace(Regex("""(?i)\s+(?:Season\s+\d+|S\d+|II|III|IV|V|VI|VII|VIII|IX|X).*"""), "").trim()
    }
}
