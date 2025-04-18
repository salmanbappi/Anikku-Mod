package tachiyomi.domain.track.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.track.model.Track

interface TrackRepository {

    suspend fun getTrackByAnimeId(id: Long): Track?

    // SY -->
    suspend fun getAnimeTracks(): List<Track>

    suspend fun getTracksByAnimeIds(animeIds: List<Long>): List<Track>
    // SY <--

    suspend fun getTracksByAnimeId(animeId: Long): List<Track>

    fun getAnimeTracksAsFlow(): Flow<List<Track>>

    fun getTracksByAnimeIdAsFlow(animeId: Long): Flow<List<Track>>

    suspend fun delete(animeId: Long, trackerId: Long)

    suspend fun insertAnime(track: Track)

    suspend fun insertAllAnime(tracks: List<Track>)
}
