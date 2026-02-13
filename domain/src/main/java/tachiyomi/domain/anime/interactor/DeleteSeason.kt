package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.repository.AnimeMergeRepository

class DeleteSeason(
    private val animeMergeRepository: AnimeMergeRepository,
) {
    suspend fun await(id: Long) {
        animeMergeRepository.deleteById(id)
    }
}
