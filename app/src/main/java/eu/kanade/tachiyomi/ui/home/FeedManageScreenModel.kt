package eu.kanade.tachiyomi.ui.home

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.source.interactor.DeleteFeedSavedSearchById
import tachiyomi.domain.source.interactor.GetFeedSavedSearchGlobal
import tachiyomi.domain.source.interactor.GetSavedSearchGlobalFeed
import tachiyomi.domain.source.interactor.ReorderFeed
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedManageScreenModel(
    private val sourceManager: SourceManager = Injekt.get(),
    private val getFeedSavedSearchGlobal: GetFeedSavedSearchGlobal = Injekt.get(),
    private val getSavedSearchGlobalFeed: GetSavedSearchGlobalFeed = Injekt.get(),
    private val reorderFeed: ReorderFeed = Injekt.get(),
    private val deleteFeedSavedSearchById: DeleteFeedSavedSearchById = Injekt.get(),
) : StateScreenModel<FeedManageScreenModel.State>(State()) {

    init {
        getFeed()
    }

    fun getFeed() {
        screenModelScope.launchIO {
            val feedSavedSearches = getFeedSavedSearchGlobal.await()
            val savedSearches = getSavedSearchGlobalFeed.await()
            
            val items = feedSavedSearches.map { feed ->
                val source = sourceManager.get(feed.source)
                FeedItem(
                    feed = feed,
                    title = savedSearches.find { it.id == feed.savedSearch }?.name ?: source?.name ?: "Unknown",
                )
            }

            mutableState.update {
                it.copy(
                    items = items.toImmutableList(),
                )
            }
        }
    }

    fun moveUp(feed: FeedSavedSearch) {
        screenModelScope.launchIO {
            reorderFeed.moveUp(feed)
            getFeed()
        }
    }

    fun moveDown(feed: FeedSavedSearch) {
        screenModelScope.launchIO {
            reorderFeed.moveDown(feed)
            getFeed()
        }
    }

    fun delete(feed: FeedSavedSearch) {
        screenModelScope.launchIO {
            deleteFeedSavedSearchById.await(feed.id)
            getFeed()
        }
    }

    @Immutable
    data class State(
        val items: ImmutableList<FeedItem> = persistentListOf(),
    )

    @Immutable
    data class FeedItem(
        val feed: FeedSavedSearch,
        val title: String,
    )
}
