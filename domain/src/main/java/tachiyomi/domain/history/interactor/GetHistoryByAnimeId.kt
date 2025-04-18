package tachiyomi.domain.history.interactor

import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.repository.HistoryRepository

class GetHistoryByAnimeId(
    private val repository: HistoryRepository,
) {

    suspend fun await(animeId: Long): List<History> {
        return repository.getByAnimeId(animeId)
    }
}
