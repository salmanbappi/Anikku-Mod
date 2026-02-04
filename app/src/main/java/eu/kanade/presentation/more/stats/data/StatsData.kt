package eu.kanade.presentation.more.stats.data

import eu.kanade.tachiyomi.network.model.ExtensionHealth

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
    ) : StatsData

    data class TimeDistribution(
        val daysDistribution: Map<Int, Long>, // DayOfWeek to session count
        val weeklyHeatmap: Map<Int, Int>, // Hour of day to total frequency
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
        val preferredWatchTime: String,
        val avgSessionsPerWeek: Double,
    ) : StatsData

    data class InfrastructureAnalytics(
        val latencyMatrix: List<Pair<String, Int>>,
        val throughputDistribution: List<Pair<String, Long>>,
        val reliabilityIndex: List<Pair<String, Double>>,
        val topologyBreakdown: Map<String, Int>,
        val healthReport: List<ExtensionHealth>,
    ) : StatsData
}

data class ExtensionInfo(
    val name: String,
    val count: Int,
    val repo: String?,
)
