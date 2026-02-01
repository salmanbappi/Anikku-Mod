package eu.kanade.domain.episode.model

import eu.kanade.domain.anime.model.downloadedFilter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.anime.EpisodeItem
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.applyFilter
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.service.getEpisodeSort
import tachiyomi.source.local.isLocal

/**
 * Applies the view filters to the list of episodes obtained from the database.
 * @return an observable of the list of episodes filtered and sorted.
 */
fun List<Episode>.applyFilters(anime: Anime, downloadManager: DownloadManager): List<Episode> {
    val isLocalAnime = anime.isLocal()
    val unseenFilter = anime.unseenFilter
    val downloadedFilter = anime.downloadedFilter
    val bookmarkedFilter = anime.bookmarkedFilter
    // AM (FILLERMARK) -->
    val fillermarkedFilter = anime.fillermarkedFilter
    // <-- AM (FILLERMARK)

    return filter { episode -> applyFilter(unseenFilter) { !episode.seen } }
        .filter { episode -> applyFilter(bookmarkedFilter) { episode.bookmark } }
        // AM (FILLERMARK) -->
        .filter { episode -> applyFilter(fillermarkedFilter) { episode.fillermark } }
        // <-- AM (FILLERMARK)
        .filter { episode ->
            applyFilter(downloadedFilter) {
                val downloaded = downloadManager.isEpisodeDownloaded(
                    episode.name,
                    episode.scanlator,
                    // SY -->
                    anime.ogTitle,
                    // SY <--
                    anime.source,
                )
                downloaded || isLocalAnime
            }
        }
        .sortedWith(getEpisodeSort(anime))
}

/**
 * Applies the view filters to the list of episodes obtained from the database.
 * @return an observable of the list of episodes filtered and sorted.
 */
fun List<EpisodeItem>.applyFilters(anime: Anime): Sequence<EpisodeItem> {
    val isLocalAnime = anime.isLocal()
    val unseenFilter = anime.unseenFilter
    val downloadedFilter = anime.downloadedFilter
    val bookmarkedFilter = anime.bookmarkedFilter
    // AM (FILLERMARK) -->
    val fillermarkedFilter = anime.fillermarkedFilter
    // <-- AM (FILLERMARK)
    return asSequence()
        .filter { item -> applyFilter(unseenFilter) { !item.episode.seen } }
        .filter { item -> applyFilter(bookmarkedFilter) { item.episode.bookmark } }
        // AM (FILLERMARK) -->
        .filter { item -> applyFilter(fillermarkedFilter) { item.episode.fillermark } }
        // <-- AM (FILLERMARK)
        .filter { item -> applyFilter(downloadedFilter) { item.downloadState == eu.kanade.tachiyomi.data.download.model.Download.State.DOWNLOADED || isLocalAnime } }
        .sortedWith { item1, item2 -> getEpisodeSort(anime).invoke(item1.episode, item2.episode) }
}