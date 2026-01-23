package tachiyomi.data.anime

import tachiyomi.domain.anime.model.MergedAnimeReference

object MergedAnimeMapper {
    fun map(
        id: Long,
        isInfoAnime: Boolean,
        getEpisodeUpdates: Boolean,
        episodeSortMode: Long,
        episodePriority: Long,
        downloadEpisodes: Boolean,
        mergeId: Long,
        mergeUrl: String,
        animeId: Long?,
        animeUrl: String,
        animeSourceId: Long,
    ): MergedAnimeReference {
        return MergedAnimeReference(
            id = id,
            isInfoAnime = isInfoAnime,
            getEpisodeUpdates = getEpisodeUpdates,
            episodeSortMode = episodeSortMode.toInt(),
            episodePriority = episodePriority.toInt(),
            downloadEpisodes = downloadEpisodes,
            mergeId = mergeId,
            mergeUrl = mergeUrl,
            animeId = animeId,
            animeUrl = animeUrl,
            animeSourceId = animeSourceId,
        )
    }
}
