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

    suspend fun await(animeId: Long): List<Season> {
        val anime = animeRepository.getAnimeById(animeId) ?: return emptyList()
        val references = animeMergeRepository.getReferencesById(animeId)
        
        if (references.isEmpty()) {
            return listOf(Season(anime, SeasonRecognition.parseSeasonNumber(anime.title, anime.title), true))
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

    fun subscribe(animeId: Long): Flow<List<Season>> = flow {
        val animeFlow = animeRepository.getAnimeByIdAsFlow(animeId)
        val mergedAnimesFlow = animeMergeRepository.subscribeMergedAnimeById(animeId)
        val referencesFlow = animeMergeRepository.subscribeReferencesById(animeId)

        emitAll(
            combine(animeFlow, mergedAnimesFlow, referencesFlow) { anime, mergedAnimes, references ->
                if (anime == null) return@combine emptyList<Season>()
                
                if (references.isEmpty()) {
                    return@combine listOf(Season(anime, SeasonRecognition.parseSeasonNumber(anime.title, anime.title), true))
                }

                mergedAnimes.map { mergedAnime ->
                    val ref = references.find { it.animeId == mergedAnime.id }
                    Season(
                        anime = mergedAnime,
                        seasonNumber = SeasonRecognition.parseSeasonNumber(anime.title, mergedAnime.title),
                        isPrimary = ref?.isInfoAnime ?: false
                    )
                }.sortedBy { it.seasonNumber }
            }
        )
    }
}
