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
        
        val rootTitle = getRootTitle(anime.title)
        
        return try {
            val searchResult = source.getSearchAnime(1, rootTitle, source.getFilterList())
            
            searchResult.animes
                .map { it.toDomainAnime(anime.source) }
                .filter { candidate ->
                    val candidateRoot = getRootTitle(candidate.title)
                    // Upgraded Strictness: Root titles must match almost exactly (ignoring case)
                    // and must not be the same entry.
                    candidateRoot.equals(rootTitle, ignoreCase = true) &&
                    candidate.url != anime.url &&
                    // Exclude entries that are likely completely different series but share words
                    !candidate.title.contains("Movie", ignoreCase = true)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getRootTitle(title: String): String {
        return title
            .replace(Regex("""(?i)\s+(:|--|–).*"""), "") // Remove subtitles after colons/dashes
            .replace(Regex("""(?i)\s+(?:Season\s+\d+|S\d+|II|III|IV|V|VI|VII|VIII|IX|X|\d+)(?:\s+|$)"""), "") // Remove season markers
            .replace(Regex("""(?i)\s+\(?(?:TV|OAV|OVA|ONA|Special|Movie|BD|Remux)\)?.*"""), "") // Remove format tags
            .trim()
    }
}
