package tachiyomi.domain.history.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.model.HistoryWithRelations

interface HistoryRepository {

    fun getAnimeHistory(query: String): Flow<List<HistoryWithRelations>>

    suspend fun getLastAnimeHistory(): HistoryWithRelations?

    suspend fun resetAnimeHistory(historyId: Long)

    suspend fun getHistoryByAnimeId(animeId: Long): List<History>

    suspend fun resetHistoryByAnimeId(animeId: Long)

    suspend fun deleteAllAnimeHistory(): Boolean

    suspend fun upsertAnimeHistory(historyUpdate: HistoryUpdate)
}
