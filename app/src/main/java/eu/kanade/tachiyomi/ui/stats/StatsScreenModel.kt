package eu.kanade.tachiyomi.ui.stats

import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMapNotNull
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.util.fastCountNot
import eu.kanade.core.util.fastFilterNot
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.presentation.more.stats.data.StatsData
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.track.AnimeTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.model.SAnime
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.anime.interactor.GetLibraryAnime
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.library.model.LibraryAnime
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ANIME_HAS_UNSEEN
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ANIME_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ANIME_NON_SEEN
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.source.service.SourceManager
import kotlinx.coroutines.flow.first
import java.util.Calendar

class StatsScreenModel(
    private val downloadManager: DownloadManager = Injekt.get(),
    private val getAnimelibAnime: GetLibraryAnime = Injekt.get(),
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val getHistory: GetHistory = Injekt.get(),
    private val preferences: LibraryPreferences = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val aiManager: eu.kanade.tachiyomi.data.ai.AiManager = Injekt.get(),
) : StateScreenModel<StatsScreenState>(StatsScreenState.Loading) {

    private val loggedInTrackers by lazy { trackerManager.loggedInTrackers().filter { it is AnimeTracker } }

    init {
        screenModelScope.launchIO {
            val animelibAnime = getAnimelibAnime.await()
            val history = getHistory.subscribe("").first()

            val distinctLibraryAnime = animelibAnime.fastDistinctBy { it.id }

            val animeTrackMap = getAnimeTrackMap(distinctLibraryAnime)
            val scoredAnimeTrackerMap = getScoredAnimeTrackMap(animeTrackMap)

            val meanScore = getTrackMeanScore(scoredAnimeTrackerMap)

            val overviewStatData = StatsData.AnimeOverview(
                libraryAnimeCount = distinctLibraryAnime.size,
                completedAnimeCount = distinctLibraryAnime.count {
                    it.anime.status.toInt() == SAnime.COMPLETED && it.unseenCount == 0L
                },
                totalSeenDuration = getWatchTime(distinctLibraryAnime),
            )

            val titlesStatData = StatsData.AnimeTitles(
                globalUpdateItemCount = getGlobalUpdateItemCount(animelibAnime),
                startedAnimeCount = distinctLibraryAnime.count { it.hasStarted },
                localAnimeCount = distinctLibraryAnime.count { it.anime.isLocal() },
            )

            val chaptersStatData = StatsData.Episodes(
                totalEpisodeCount = distinctLibraryAnime.sumOf { it.totalEpisodes }.toInt(),
                readEpisodeCount = distinctLibraryAnime.sumOf { it.seenCount }.toInt(),
                downloadCount = downloadManager.getDownloadCount(),
            )

            val trackersStatData = StatsData.Trackers(
                trackedTitleCount = animeTrackMap.count { it.value.isNotEmpty() },
                meanScore = meanScore,
                trackerCount = loggedInTrackers.size,
            )

            // Extension Usage
            val extensionUsage = StatsData.ExtensionUsage(
                topExtensions = distinctLibraryAnime
                    .map { sourceManager.getOrStub(it.anime.source).name }
                    .groupingBy { it }.eachCount().entries
                    .sortedByDescending { it.value }.take(5)
                    .map { it.toPair() }
            )

            // Genre Affinity
            val genreAffinity = StatsData.GenreAffinity(
                genreScores = distinctLibraryAnime.flatMap { it.anime.genre ?: emptyList() }
                    .groupingBy { it }.eachCount().entries
                    .sortedByDescending { it.value }.take(10)
                    .map { it.toPair() }
            )

            // Time Distribution
            val timeDistribution = calculateTimeDistribution(history)

            // Watch Habits
            val watchHabits = calculateWatchHabits(history, distinctLibraryAnime)

            val aiAnalysis = fetchAiAnalysis(distinctLibraryAnime, chaptersStatData, trackersStatData, extensionUsage, genreAffinity)

            mutableState.update {
                StatsScreenState.SuccessAnime(
                    overview = overviewStatData,
                    titles = titlesStatData,
                    episodes = chaptersStatData,
                    trackers = trackersStatData,
                    extensions = extensionUsage,
                    timeDistribution = timeDistribution,
                    genreAffinity = genreAffinity,
                    watchHabits = watchHabits,
                    aiAnalysis = aiAnalysis,
                )
            }
        }
    }

    private fun calculateTimeDistribution(history: List<tachiyomi.domain.history.model.HistoryWithRelations>): StatsData.TimeDistribution {
        val distribution = mutableMapOf<Int, Long>()
        history.forEach { item ->
            val cal = Calendar.getInstance().apply { time = item.seenAt ?: return@forEach }
            val day = cal.get(Calendar.DAY_OF_WEEK)
            distribution[day] = (distribution[day] ?: 0L) + 1 // Count sessions for now
        }
        return StatsData.TimeDistribution(distribution)
    }

    private fun calculateWatchHabits(
        history: List<tachiyomi.domain.history.model.HistoryWithRelations>,
        animeList: List<LibraryAnime>
    ): StatsData.WatchHabits {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L
        val monthMillis = 30 * dayMillis

        val topDay = history.filter { (it.seenAt?.time ?: 0) > (now - dayMillis) }
            .groupingBy { it.animeId }.eachCount().maxByOrNull { it.value }
            ?.let { id -> animeList.find { it.id == id }?.anime?.title }

        val topMonth = history.filter { (it.seenAt?.time ?: 0) > (now - monthMillis) }
            .groupingBy { it.animeId }.eachCount().maxByOrNull { it.value }
            ?.let { id -> animeList.find { it.id == id }?.anime?.title }

        // Preferred watch time
        val hourCounts = history.mapNotNull { it.readAt }.map { 
            Calendar.getInstance().apply { time = it }.get(Calendar.HOUR_OF_DAY)
        }.groupingBy { it }.eachCount()
        
        val topHour = hourCounts.maxByOrNull { it.value }?.key ?: 0
        val preferredTime = when (topHour) {
            in 5..11 -> "Morning"
            in 12..17 -> "Afternoon"
            in 18..22 -> "Evening"
            else -> "Late Night"
        }

        return StatsData.WatchHabits(topDay, topMonth, preferredTime)
    }

    private suspend fun fetchAiAnalysis(
        animeList: List<LibraryAnime>,
        episodes: StatsData.Episodes,
        trackers: StatsData.Trackers,
        extensions: StatsData.ExtensionUsage,
        genres: StatsData.GenreAffinity
    ): String? {
        val summary = StringBuilder()
        summary.append("Total Anime: ${animeList.size}\n")
        summary.append("Total Episodes Watched: ${episodes.readEpisodeCount}\n")
        summary.append("Top Extensions: ${extensions.topExtensions.joinToString { it.first }}\n")
        summary.append("Favorite Genres: ${genres.genreScores.joinToString { it.first }}\n")
        
        val recentTitles = animeList.take(10).joinToString { it.anime.title }
        summary.append("Recent Highlights: $recentTitles\n")

        return aiManager.getStatisticsAnalysis(summary.toString())
    }

    private fun getGlobalUpdateItemCount(libraryAnime: List<LibraryAnime>): Int {
        val includedCategories = preferences.updateCategories().get().map { it.toLong() }
        val includedAnime = if (includedCategories.isNotEmpty()) {
            libraryAnime.filter { it.category in includedCategories }
        } else {
            libraryAnime
        }

        val excludedCategories = preferences.updateCategoriesExclude().get().map { it.toLong() }
        val excludedMangaIds = if (excludedCategories.isNotEmpty()) {
            libraryAnime.fastMapNotNull { anime ->
                anime.id.takeIf { anime.category in excludedCategories }
            }
        } else {
            emptyList()
        }

        val updateRestrictions = preferences.autoUpdateAnimeRestrictions().get()
        return includedAnime
            .fastFilterNot { it.anime.id in excludedMangaIds }
            .fastDistinctBy { it.anime.id }
            .fastCountNot {
                (ANIME_NON_COMPLETED in updateRestrictions && it.anime.status.toInt() == SAnime.COMPLETED) ||
                    (ANIME_HAS_UNSEEN in updateRestrictions && it.unseenCount != 0L) ||
                    (ANIME_NON_SEEN in updateRestrictions && it.totalEpisodes > 0 && !it.hasStarted)
            }
    }

    private suspend fun getAnimeTrackMap(libraryAnime: List<LibraryAnime>): Map<Long, List<Track>> {
        val loggedInTrackerIds = loggedInTrackers.map { it.id }.toHashSet()
        return libraryAnime.associate { anime ->
            val tracks = getTracks.await(anime.id)
                .fastFilter { it.trackerId in loggedInTrackerIds }

            anime.id to tracks
        }
    }

    private suspend fun getWatchTime(libraryAnimeList: List<LibraryAnime>): Long {
        var watchTime = 0L
        libraryAnimeList.forEach { libraryAnime ->
            getEpisodesByAnimeId.await(libraryAnime.anime.id).forEach { episode ->
                watchTime += if (episode.seen) {
                    episode.totalSeconds
                } else {
                    episode.lastSecondSeen
                }
            }
        }

        return watchTime
    }

    private fun getScoredAnimeTrackMap(trackMap: Map<Long, List<Track>>): Map<Long, List<Track>> {
        return trackMap.mapNotNull { (animeId, tracks) ->
            val trackList = tracks.mapNotNull { track ->
                track.takeIf { it.score > 0.0 }
            }
            if (trackList.isEmpty()) return@mapNotNull null
            animeId to trackList
        }.toMap()
    }

    private fun getTrackMeanScore(scoredTrackMap: Map<Long, List<Track>>): Double {
        return scoredTrackMap
            .map { (_, tracks) ->
                tracks.map(::get10PointScore).average()
            }
            .fastFilter { !it.isNaN() }
            .average()
    }

    private fun get10PointScore(track: Track): Double {
        val service = trackerManager.get(track.trackerId)!!
        return service.animeService.get10PointScore(track)
    }
}
