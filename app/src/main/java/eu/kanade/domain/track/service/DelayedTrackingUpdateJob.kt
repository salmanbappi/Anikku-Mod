package eu.kanade.domain.track.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import eu.kanade.domain.track.interactor.TrackEpisode
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.util.system.workManager
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.interactor.GetTracks
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class DelayedTrackingUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (runAttemptCount > 3) {
            return Result.failure()
        }

        val getTracks = Injekt.get<GetTracks>()
        val trackEpisode = Injekt.get<TrackEpisode>()

        val delayedTrackingStore = Injekt.get<DelayedTrackingStore>()

        withIOContext {
            delayedTrackingStore.getAnimeItems()
                .mapNotNull {
                    val track = getTracks.awaitOne(it.trackId)
                    if (track == null) {
                        delayedTrackingStore.removeAnimeItem(it.trackId)
                    }
                    track?.copy(lastEpisodeSeen = it.lastEpisodeSeen.toDouble())
                }
                .forEach { animeTrack ->
                    logcat(LogPriority.DEBUG) {
                        "Updating delayed track item: ${animeTrack.animeId}" +
                            ", last chapter read: ${animeTrack.lastEpisodeSeen}"
                    }
                    trackEpisode.await(
                        context,
                        animeTrack.animeId,
                        animeTrack.lastEpisodeSeen,
                        setupJobOnFailure = false,
                    )
                }
        }

        return if (delayedTrackingStore.getAnimeItems().isEmpty()) Result.success() else Result.retry()
    }

    companion object {
        private const val TAG = "DelayedAnimeTrackingUpdate"

        fun setupTask(context: Context) {
            val constraints = Constraints(
                requiredNetworkType = NetworkType.CONNECTED,
            )

            val request = OneTimeWorkRequestBuilder<DelayedTrackingUpdateJob>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5.minutes.toJavaDuration())
                .addTag(TAG)
                .build()

            context.workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
