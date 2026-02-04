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
        val sourceCount: Int,
    ) : StatsData

    data class ExtensionUsage(
        val topExtensions: List<ExtensionInfo>,
    ) : StatsData {
        data class ExtensionInfo(
            val name: String,
            val count: Int,
            val repo: String?,
        )
    }

    data class TimeDistribution(
        val daysDistribution: Map<Int, Long>, // DayOfWeek to session count
        val weeklyHeatmap: Map<Int, Int>, // Hour of day to total frequency
    ) : StatsData

    data class WatchHabits(
        val topDayAnime: String?,
        val topMonthAnime: String?,
        val preferredWatchTime: String,
        val avgSessionsPerWeek: Double,
    ) : StatsData
}
