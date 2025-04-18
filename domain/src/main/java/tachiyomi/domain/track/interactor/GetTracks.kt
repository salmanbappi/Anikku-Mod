package tachiyomi.domain.track.interactor

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository

class GetTracks(
    private val animetrackRepository: TrackRepository,
) {

    suspend fun awaitOne(id: Long): Track? {
        return try {
            animetrackRepository.getTrackByAnimeId(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    // SY -->
    suspend fun await(): List<Track> {
        return try {
            animetrackRepository.getAnimeTracks()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    suspend fun await(animeIds: List<Long>): Map<Long, List<Track>> {
        return try {
            animetrackRepository.getTracksByAnimeIds(animeIds)
                .groupBy { it.animeId }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyMap()
        }
    }
    // SY <--

    suspend fun await(animeId: Long): List<Track> {
        return try {
            animetrackRepository.getTracksByAnimeId(animeId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    fun subscribe(animeId: Long): Flow<List<Track>> {
        return animetrackRepository.getTracksByAnimeIdAsFlow(animeId)
    }
}
