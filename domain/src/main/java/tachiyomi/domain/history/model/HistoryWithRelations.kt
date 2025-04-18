package tachiyomi.domain.history.model

import tachiyomi.domain.anime.interactor.GetCustomAnimeInfo
import tachiyomi.domain.anime.model.AnimeCover
import uy.kohesive.injekt.injectLazy
import java.util.Date

data class HistoryWithRelations(
    val id: Long,
    val episodeId: Long,
    val animeId: Long,
    // SY -->
    val ogTitle: String,
    // SY <--
    val episodeNumber: Double,
    val seenAt: Date?,
    val watchDuration: Long,
    val coverData: AnimeCover,
) {
    // SY -->
    val title: String = customAnimeManager.get(animeId)?.title ?: ogTitle

    companion object {
        private val customAnimeManager: GetCustomAnimeInfo by injectLazy()
    }
    // SY <--
}
