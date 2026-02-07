package eu.kanade.tachiyomi.ui.home

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import tachiyomi.domain.anime.model.toDomainAnime
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.source.interactor.GetFeedSavedSearchGlobal
import tachiyomi.domain.source.interactor.GetSavedSearchGlobalFeed
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedScreenModel(
    private val sourceManager: SourceManager = Injekt.get(),
    private val getFeedSavedSearchGlobal: GetFeedSavedSearchGlobal = Injekt.get(),
    private val getSavedSearchGlobalFeed: GetSavedSearchGlobalFeed = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
) : StateScreenModel<FeedScreenModel.State>(State()) {

    init {
        screenModelScope.launchIO {
            getFeedSavedSearchGlobal.subscribe()
                .distinctUntilChanged()
                .onEach { mutableState.update { it.copy(isLoading = true) } }
                .collectLatest { feedSavedSearches ->
                    val savedSearches = getSavedSearchGlobalFeed.await()
                    val feedItems = coroutineScope {
                        feedSavedSearches.map { feed ->
                            async {
                                val source = sourceManager.get(feed.source) as? AnimeCatalogueSource
                                if (source != null) {
                                    val results = try {
                                        when (FeedSavedSearch.Type.from(feed.type)) {
                                            FeedSavedSearch.Type.Latest -> {
                                                try {
                                                    source.getLatestUpdates(1).animes
                                                } catch (e: Exception) {
                                                    source.getPopularAnime(1).animes
                                                }
                                            }
                                            FeedSavedSearch.Type.Popular -> source.getPopularAnime(1).animes
                                            FeedSavedSearch.Type.SavedSearch -> {
                                                val savedSearch = savedSearches.find { it.id == feed.savedSearch }
                                                if (savedSearch != null) {
                                                    val filters = source.getFilterList()
                                                    source.getSearchAnime(1, savedSearch.query ?: "", filters).animes
                                                } else {
                                                    emptyList()
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        emptyList()
                                    }
                                    
                                    val animeList = results.map {
                                        async {
                                            val domainAnime = it.toDomainAnime(source.id)
                                            networkToLocalAnime.await(domainAnime)
                                        }
                                    }.awaitAll()
                                    
                                    FeedItem(
                                        feed = feed,
                                        source = source,
                                        savedSearch = savedSearches.find { it.id == feed.savedSearch },
                                        animeList = animeList.distinctBy { it.id }.toImmutableList(),
                                    )
                                } else {
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }
                    
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            items = feedItems.toImmutableList(),
                        )
                    }
                }
        }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = false,
        val items: ImmutableList<FeedItem> = persistentListOf(),
    )

    @Immutable
    data class FeedItem(
        val feed: FeedSavedSearch,
        val source: AnimeCatalogueSource,
        val savedSearch: SavedSearch?,
        val animeList: ImmutableList<Anime>,
    )
}
