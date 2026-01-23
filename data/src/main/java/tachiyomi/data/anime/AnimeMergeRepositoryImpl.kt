package tachiyomi.data.anime

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.MergeAnimeSettingsUpdate
import tachiyomi.domain.anime.model.MergedAnimeReference
import tachiyomi.domain.anime.repository.AnimeMergeRepository

class AnimeMergeRepositoryImpl(
    private val handler: DatabaseHandler,
) : AnimeMergeRepository {

    override suspend fun getMergedAnime(): List<Anime> {
        return handler.awaitList { mergedQueries.selectAllMergedAnimes(AnimeMapper::mapAnime) }
    }

    override suspend fun subscribeMergedAnime(): Flow<List<Anime>> {
        return handler.subscribeToList { mergedQueries.selectAllMergedAnimes(AnimeMapper::mapAnime) }
    }

    override suspend fun getMergedAnimeById(id: Long): List<Anime> {
        return handler.awaitList { mergedQueries.selectMergedAnimesById(id, AnimeMapper::mapAnime) }
    }

    override suspend fun subscribeMergedAnimeById(id: Long): Flow<List<Anime>> {
        return handler.subscribeToList { mergedQueries.selectMergedAnimesById(id, AnimeMapper::mapAnime) }
    }

    override suspend fun getReferencesById(id: Long): List<MergedAnimeReference> {
        return handler.awaitList { mergedQueries.selectByMergeId(id, MergedAnimeMapper::map) }
    }

    override suspend fun subscribeReferencesById(id: Long): Flow<List<MergedAnimeReference>> {
        return handler.subscribeToList { mergedQueries.selectByMergeId(id, MergedAnimeMapper::map) }
    }

    override suspend fun updateSettings(update: MergeAnimeSettingsUpdate): Boolean {
        return try {
            partialUpdate(update)
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun updateAllSettings(values: List<MergeAnimeSettingsUpdate>): Boolean {
        return try {
            partialUpdate(*values.toTypedArray())
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    private suspend fun partialUpdate(vararg values: MergeAnimeSettingsUpdate) {
        handler.await(inTransaction = true) {
            values.forEach { value ->
                mergedQueries.updateSettingsById(
                    id = value.id,
                    getEpisodeUpdates = value.getEpisodeUpdates,
                    downloadEpisodes = value.downloadEpisodes,
                    infoAnime = value.isInfoAnime,
                    episodePriority = value.episodePriority?.toLong(),
                    episodeSortMode = value.episodeSortMode?.toLong(),
                )
            }
        }
    }

    override suspend fun insert(reference: MergedAnimeReference): Long? {
        return handler.awaitOneOrNullExecutable {
            mergedQueries.insert(
                infoAnime = reference.isInfoAnime,
                getEpisodeUpdates = reference.getEpisodeUpdates,
                episodeSortMode = reference.episodeSortMode.toLong(),
                episodePriority = reference.episodePriority.toLong(),
                downloadEpisodes = reference.downloadEpisodes,
                mergeId = reference.mergeId!!,
                mergeUrl = reference.mergeUrl,
                animeId = reference.animeId,
                animeUrl = reference.animeUrl,
                animeSource = reference.animeSourceId,
            )
            mergedQueries.selectLastInsertedRowId()
        }
    }

    override suspend fun insertAll(references: List<MergedAnimeReference>) {
        handler.await(true) {
            references.forEach { reference ->
                mergedQueries.insert(
                    infoAnime = reference.isInfoAnime,
                    getEpisodeUpdates = reference.getEpisodeUpdates,
                    episodeSortMode = reference.episodeSortMode.toLong(),
                    episodePriority = reference.episodePriority.toLong(),
                    downloadEpisodes = reference.downloadEpisodes,
                    mergeId = reference.mergeId!!,
                    mergeUrl = reference.mergeUrl,
                    animeId = reference.animeId,
                    animeUrl = reference.animeUrl,
                    animeSource = reference.animeSourceId,
                )
            }
        }
    }

    override suspend fun deleteById(id: Long) {
        handler.await {
            mergedQueries.deleteById(id)
        }
    }

    override suspend fun deleteByMergeId(mergeId: Long) {
        handler.await {
            mergedQueries.deleteByMergeId(mergeId)
        }
    }

    override suspend fun getMergeAnimeForDownloading(mergeId: Long): List<Anime> {
        return handler.awaitList { mergedQueries.selectMergedAnimesForDownloadingById(mergeId, AnimeMapper::mapAnime) }
    }
}
