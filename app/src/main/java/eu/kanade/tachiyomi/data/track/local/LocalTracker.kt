package eu.kanade.tachiyomi.data.track.local

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.AnimeTracker
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import tachiyomi.domain.track.model.Track as DomainAnimeTrack

class LocalTracker(id: Long) : BaseTracker(id, "Local Metadata"), AnimeTracker {

    override val isLoggedIn: Boolean = true
    override val isLoggedInFlow: Flow<Boolean> = flowOf(true)

    override suspend fun search(query: String): List<TrackSearch> = emptyList()

    override suspend fun refresh(track: Track): Track = track

    override suspend fun update(track: Track): Track = track

    override fun getStatusList(): ImmutableList<Long> = persistentListOf(
        WATCHING,
        COMPLETED,
        ON_HOLD,
        DROPPED,
        PLAN_TO_WATCH,
    )

    override fun getStatus(status: Long): Int? = null

    override fun get10PointScore(track: DomainAnimeTrack): Double = track.score

    companion object {
        const val WATCHING = 1L
        const val COMPLETED = 2L
        const val ON_HOLD = 3L
        const val DROPPED = 4L
        const val PLAN_TO_WATCH = 5L
    }
}
