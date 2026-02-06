package tachiyomi.data.source

import tachiyomi.domain.source.model.FeedSavedSearch

object FeedSavedSearchMapper {
    fun map(
        id: Long,
        source: Long,
        savedSearch: Long?,
        global: Boolean,
        feedOrder: Long,
        searchType: Long,
    ): FeedSavedSearch {
        return FeedSavedSearch(
            id = id,
            source = source,
            savedSearch = savedSearch,
            global = global,
            feedOrder = feedOrder,
            type = searchType.toInt(),
        )
    }
}
