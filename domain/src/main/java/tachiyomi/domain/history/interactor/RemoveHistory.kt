package tachiyomi.domain.history.interactor

import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.repository.HistoryRepository

class RemoveHistory(
    private val repository: HistoryRepository,
) {

    suspend fun awaitAll(): Boolean {
        return repository.deleteAllAnimeHistory()
    }

    suspend fun await(history: HistoryWithRelations) {
        repository.resetAnimeHistory(history.id)
    }

    suspend fun await(animeId: Long) {
        repository.resetHistoryByAnimeId(animeId)
    }
}
