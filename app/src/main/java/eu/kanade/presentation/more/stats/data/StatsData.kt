package eu.kanade.presentation.more.stats.data

sealed interface StatsData {

    data class AnimeOverview(
        val libraryAnimeCount: Int,
        val completedAnimeCount: Int,
        val totalSeenDuration: Long,
    ) : StatsData

    data class AnimeTitles(
        val globalUpdateItemCount: Int,
        val startedAnimeCount: Int,
        val localAnimeCount: Int,
    ) : StatsData

    data class Episodes(
        val totalEpisodeCount: Int,
        val readEpisodeCount: Int,
        val downloadCount: Int,
    ) : StatsData

    data class Trackers(
        val trackedTitleCount: Int,
        val meanScore: Double,
        val trackerCount: Int,
    ) : StatsData

    data class ExtensionUsage(
        val topExtensions: List<Pair<String, Int>>,
    ) : StatsData

    data class TimeDistribution(
        val daysDistribution: Map<Int, Long>, // DayOfWeek to Milliseconds
    ) : StatsData

    data class GenreAffinity(
        val genreScores: List<Pair<String, Int>>, // Genre to count
    ) : StatsData

    data class ScoreDistribution(
        val scoredAnimeCount: Int,
        val distribution: Map<Int, Int>, // Score (1-10) to count
    ) : StatsData

    data class StatusBreakdown(
        val completedCount: Int,
        val ongoingCount: Int,
        val droppedCount: Int,
        val onHoldCount: Int,
        val planToWatchCount: Int,
    ) : StatsData

    data class WatchHabits(
        val topDayAnime: String?,
        val topMonthAnime: String?,
        val preferredWatchTime: String, // e.g., "Late Night"
    ) : StatsData
}
