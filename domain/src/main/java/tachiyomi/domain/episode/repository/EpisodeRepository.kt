package tachiyomi.domain.episode.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.model.EpisodeUpdate

interface EpisodeRepository {

    suspend fun addAllEpisodes(episodes: List<Episode>): List<Episode>

    suspend fun updateEpisode(episodeUpdate: EpisodeUpdate)

    suspend fun updateAllEpisodes(episodeUpdates: List<EpisodeUpdate>)

    suspend fun removeEpisodesWithIds(episodeIds: List<Long>)

    suspend fun getEpisodeByAnimeId(animeId: Long): List<Episode>

    suspend fun getBookmarkedEpisodesByAnimeId(animeId: Long): List<Episode>

    // AM (FILLERMARK) -->
    suspend fun getFillermarkedEpisodesByAnimeId(animeId: Long): List<Episode>
    // <-- AM (FILLERMARK)

    suspend fun getEpisodeById(id: Long): Episode?

    suspend fun getEpisodeByAnimeIdAsFlow(animeId: Long): Flow<List<Episode>>

    suspend fun getEpisodeByUrlAndAnimeId(url: String, animeId: Long): Episode?
}
