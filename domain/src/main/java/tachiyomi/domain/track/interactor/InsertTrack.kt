package tachiyomi.domain.track.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository

class InsertTrack(
    private val animetrackRepository: TrackRepository,
) {

    suspend fun await(track: Track) {
        try {
            animetrackRepository.insertAnime(track)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    suspend fun awaitAll(tracks: List<Track>) {
        try {
            animetrackRepository.insertAllAnime(tracks)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
