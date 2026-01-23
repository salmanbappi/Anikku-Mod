package tachiyomi.domain.updates.model

import tachiyomi.domain.anime.interactor.GetCustomAnimeInfo
import tachiyomi.domain.anime.model.AnimeCover
import uy.kohesive.injekt.injectLazy

data class UpdatesWithRelations(
    val animeId: Long,
    // SY -->
    val ogAnimeTitle: String,
    // SY <--
    val episodeId: Long,
    val episodeName: String,
    val scanlator: String?,
    val seen: Boolean,
    val bookmark: Boolean,
    // AM (FILLERMARK) -->
    val fillermark: Boolean,
    // <-- AM (FILLERMARK)
    val lastSecondSeen: Long,
    val totalSeconds: Long,
    val sourceId: Long,
    val dateFetch: Long,
    val coverData: AnimeCover,
) {
    // SY -->
    val animeTitle: String = getCustomAnimeInfo.get(animeId)?.title ?: ogAnimeTitle

    companion object {
        private val getCustomAnimeInfo: GetCustomAnimeInfo by injectLazy()
    }
    // SY <--
}
