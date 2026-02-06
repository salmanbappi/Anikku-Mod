package tachiyomi.domain.source.interactor

import tachiyomi.domain.source.model.FeedSavedSearchUpdate
import tachiyomi.domain.source.repository.FeedSavedSearchRepository

class UpdateFeedSavedSearch(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {
    suspend fun await(update: FeedSavedSearchUpdate) {
        feedSavedSearchRepository.updatePartial(update)
    }

    suspend fun awaitAll(updates: List<FeedSavedSearchUpdate>) {
        feedSavedSearchRepository.updatePartial(updates)
    }
}
