package tachiyomi.domain.anime.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.Season
import tachiyomi.domain.anime.repository.AnimeMergeRepository
import tachiyomi.domain.anime.repository.AnimeRepository
import tachiyomi.domain.anime.service.SeasonRecognition

class GetSeasonsByAnimeId(
    private val animeRepository: AnimeRepository,
    private val animeMergeRepository: AnimeMergeRepository,
) {

    suspend fun await(animeId: Long, virtualSeasons: List<Anime> = emptyList()): List<Season> {
        val anime = animeRepository.getAnimeById(animeId) ?: return emptyList()
        val references = animeMergeRepository.getReferencesById(animeId)
        
        if (references.isEmpty()) {
            if (virtualSeasons.isEmpty()) {
                return listOf(Season(anime, SeasonRecognition.parseSeasonNumber(anime.title, anime.title), true))
            }
            // Use virtual seasons from discovery
            val all = (listOf(anime) + virtualSeasons).distinctBy { it.id }
            return all.map { 
                Season(
                    anime = it,
                    seasonNumber = SeasonRecognition.parseSeasonNumber(anime.title, it.title),
                    isPrimary = it.id == anime.id
                )
            }.sortedBy { it.seasonNumber }
        }

        val mergedAnimes = animeMergeRepository.getMergedAnimeById(animeId)
        
        return mergedAnimes.map { mergedAnime ->
            val ref = references.find { it.animeId == mergedAnime.id }
            Season(
                anime = mergedAnime,
                seasonNumber = SeasonRecognition.parseSeasonNumber(anime.title, mergedAnime.title),
                isPrimary = ref?.isInfoAnime ?: false
            )
        }.sortedBy { it.seasonNumber }
    }

    fun subscribe(animeId: Long, virtualSeasonsFlow: Flow<List<Anime>>? = null): Flow<List<Season>> = flow {
        val animeFlow = animeRepository.getAnimeByIdAsFlow(animeId)
        val mergedAnimesFlow = animeMergeRepository.subscribeMergedAnimeById(animeId)
        val referencesFlow = animeMergeRepository.subscribeReferencesById(animeId)

        val flow = if (virtualSeasonsFlow != null) {
            combine(animeFlow, mergedAnimesFlow, referencesFlow, virtualSeasonsFlow) { anime, mergedAnimes, references, virtual ->
                mapToSeasons(anime, mergedAnimes, references, virtual)
            }
        } else {
            combine(animeFlow, mergedAnimesFlow, referencesFlow) { anime, mergedAnimes, references ->
                mapToSeasons(anime, mergedAnimes, references, emptyList())
            }
        }
        emitAll(flow)
    }

    private fun mapToSeasons(
        anime: Anime?,
        mergedAnimes: List<Anime>,
        references: List<tachiyomi.domain.anime.model.MergedAnimeReference>,
        virtualSeasons: List<Anime>
    ): List<Season> {
        if (anime == null) return emptyList()
        
        if (references.isEmpty()) {
            if (virtualSeasons.isEmpty()) {
                return listOf(Season(anime, SeasonRecognition.parseSeasonNumber(anime.title, anime.title), true))
            }
            val all = (listOf(anime) + virtualSeasons).distinctBy { it.id }
            return all.map { 
                Season(
                    anime = it,
                    seasonNumber = SeasonRecognition.parseSeasonNumber(anime.title, it.title),
                    isPrimary = it.id == anime.id
                )
            }.sortedBy { it.seasonNumber }
        }

        return mergedAnimes.map { mergedAnime ->
            val ref = references.find { it.animeId == mergedAnime.id }
            Season(
                anime = mergedAnime,
                seasonNumber = SeasonRecognition.parseSeasonNumber(anime.title, mergedAnime.title),
                isPrimary = ref?.isInfoAnime ?: false
            )
        }.sortedBy { it.seasonNumber }
    }
}
