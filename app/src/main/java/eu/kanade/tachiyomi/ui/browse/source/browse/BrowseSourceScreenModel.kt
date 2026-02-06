package eu.kanade.tachiyomi.ui.browse.source.browse

import android.content.res.Configuration
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.domain.anime.interactor.UpdateAnime
import tachiyomi.domain.anime.model.toDomainAnime
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.util.removeCovers
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.GetDuplicateLibraryAnime
import tachiyomi.domain.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.toAnimeUpdate
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetAnimeCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.episode.interactor.SetAnimeDefaultEpisodeFlags
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.interactor.DeleteSavedSearchById
import tachiyomi.domain.source.interactor.GetRemoteAnime
import tachiyomi.domain.source.interactor.GetSavedSearchBySourceId
import tachiyomi.domain.source.interactor.InsertSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import eu.kanade.tachiyomi.animesource.model.AnimeFilter as AnimeSourceModelFilter

class BrowseSourceScreenModel(
    private val sourceId: Long,
    listingQuery: String?,
    sourceManager: SourceManager = Injekt.get(),
    sourcePreferences: SourcePreferences = Injekt.get(),
    basePreferences: BasePreferences = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val getRemoteAnime: GetRemoteAnime = Injekt.get(),
    private val getDuplicateAnimelibAnime: GetDuplicateLibraryAnime = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val setAnimeCategories: SetAnimeCategories = Injekt.get(),
    private val setAnimeDefaultEpisodeFlags: SetAnimeDefaultEpisodeFlags = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val addTracks: AddTracks = Injekt.get(),
    private val getSavedSearchBySourceId: GetSavedSearchBySourceId = Injekt.get(),
    private val insertSavedSearch: InsertSavedSearch = Injekt.get(),
    private val deleteSavedSearchById: DeleteSavedSearchById = Injekt.get(),
    private val filterSerializer: FilterSerializer = Injekt.get(),
    private val getFavorites: tachiyomi.domain.anime.interactor.GetFavorites = Injekt.get(),
) : StateScreenModel<BrowseSourceScreenModel.State>(State(Listing.valueOf(listingQuery))) {

    var displayMode by sourcePreferences.sourceDisplayMode().asState(screenModelScope)

    val source = sourceManager.getOrStub(sourceId)

    init {
        if (source is CatalogueSource) {
            mutableState.update {
                var query: String? = null
                var listing = it.listing

                if (listing is Listing.Search) {
                    query = listing.query
                    listing = Listing.Search(query, source.getFilterList())
                }

                it.copy(
                    listing = listing,
                    filters = source.getFilterList(),
                    toolbarQuery = query,
                )
            }
        }

        if (!basePreferences.incognitoMode().get()) {
            sourcePreferences.lastUsedSource().set(source.id)
        }

        getSavedSearchBySourceId.subscribe(sourceId)
            .onEach { savedSearches ->
                mutableState.update { it.copy(savedSearches = savedSearches.toImmutableList()) }
            }
            .launchIn(screenModelScope)

        getFavorites.subscribe(sourceId)
            .onEach { favorites ->
                mutableState.update { it.copy(favoriteIds = favorites.map { fav -> fav.id }.toSet()) }
            }
            .launchIn(screenModelScope)
    }

    /**
     * Flow of Pager flow tied to [State.listing]
     */
    private val hideInLibraryItems = sourcePreferences.hideInAnimeLibraryItems().get()
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val animePagerFlowFlow = state.map { it.listing }
        .distinctUntilChanged()
        .map { listing ->
            Pager(PagingConfig(pageSize = 25)) {
                getRemoteAnime.subscribe(sourceId, listing.query ?: "", listing.filters)
            }.flow.map { pagingData ->
                pagingData.map {
                    networkToLocalAnime.await(it.toDomainAnime(sourceId))
                }
                    .filter { !hideInLibraryItems || !it.favorite }
            }
                .cachedIn(screenModelScope)
        }
        .stateIn(screenModelScope, SharingStarted.Lazily, emptyFlow())

    fun getColumnsPreference(orientation: Int): GridCells {
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        val columns = if (isLandscape) {
            libraryPreferences.landscapeColumns()
        } else {
            libraryPreferences.portraitColumns()
        }.get()
        return if (columns == 0) GridCells.Adaptive(128.dp) else GridCells.Fixed(columns)
    }

    // returns the number from the size slider
    fun getColumnsPreferenceForCurrentOrientation(orientation: Int): Int {
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        return if (isLandscape) {
            libraryPreferences.landscapeColumns()
        } else {
            libraryPreferences.portraitColumns()
        }.get()
    }

    fun resetFilters() {
        if (source !is CatalogueSource) return

        mutableState.update { it.copy(filters = source.getFilterList()) }
    }

    fun setListing(listing: Listing) {
        mutableState.update { it.copy(listing = listing, toolbarQuery = null, currentSavedSearch = null) }
    }

    fun setFilters(filters: FilterList) {
        if (source !is CatalogueSource) return

        mutableState.update {
            it.copy(
                filters = filters,
            )
        }
    }

    /**
     * Updated to handle mutual exclusivity in filter groups
     */
    fun onFilterUpdate(filter: AnimeSourceModelFilter<*>) {
        if (source !is CatalogueSource) return

        val currentFilters = state.value.filters
        if (filter is AnimeSourceModelFilter.Group<*>) {
            // Check if this group has any active selections
            val isNotEmpty = filter.state.any {
                (it as? AnimeSourceModelFilter.CheckBox)?.state == true ||
                    (it as? AnimeSourceModelFilter.TriState)?.state != AnimeSourceModelFilter.TriState.STATE_IGNORE
            }

            if (isNotEmpty) {
                // Clear other groups
                currentFilters.filterIsInstance<AnimeSourceModelFilter.Group<*>>()
                    .filter { it != filter }
                    .forEach { group ->
                        group.state.forEach { subFilter ->
                            when (subFilter) {
                                is AnimeSourceModelFilter.CheckBox -> subFilter.state = false
                                is AnimeSourceModelFilter.TriState -> subFilter.state = AnimeSourceModelFilter.TriState.STATE_IGNORE
                                else -> {}
                            }
                        }
                    }
            }
        }
        mutableState.update { it.copy(filters = currentFilters) }
    }

    fun saveSearch(name: String) {
        val query = state.value.toolbarQuery
        val filters = state.value.filters
        val filtersJson = if (filters.isNotEmpty()) filterSerializer.serialize(filters) else null

        screenModelScope.launchIO {
            insertSavedSearch.await(
                SavedSearch(
                    id = -1,
                    source = sourceId,
                    name = name,
                    query = query,
                    filtersJson = filtersJson,
                ),
            )
        }
    }

    fun deleteSearch(savedSearchId: Long) {
        screenModelScope.launchIO {
            deleteSavedSearchById.await(savedSearchId)
        }
    }

    fun loadSearch(savedSearch: SavedSearch) {
        if (source !is CatalogueSource) return

        val filters = source.getFilterList()
        savedSearch.filtersJson?.let {
            filterSerializer.deserialize(filters, it)
        }

        mutableState.update {
            it.copy(
                filters = filters,
                toolbarQuery = savedSearch.query,
                listing = Listing.Search(query = savedSearch.query, filters = filters),
                currentSavedSearch = savedSearch,
            )
        }
    }

    fun search(query: String? = null, filters: FilterList? = null) {
        val nextListing = Listing.Search(query, filters ?: state.value.filters)
        if (state.value.listing == nextListing) return
        mutableState.update { it.copy(listing = nextListing) }
    }

    fun searchGenre(genreName: String) {
        if (source !is CatalogueSource) return

        val defaultFilters = source.getFilterList()
        var genreExists = false

        filter@ for (sourceFilter in defaultFilters) {
            if (sourceFilter is AnimeSourceModelFilter.Group<*>) {
                for (filter in sourceFilter.state) {
                    if (filter is AnimeSourceModelFilter<*> && filter.name.equals(genreName, true)) {
                        when (filter) {
                            is AnimeSourceModelFilter.TriState -> filter.state = 1
                            is AnimeSourceModelFilter.CheckBox -> filter.state = true
                            else -> {}
                        }
                        genreExists = true
                        break@filter
                    }
                }
            } else if (sourceFilter is AnimeSourceModelFilter.Select<*>) {
                val index = sourceFilter.values.filterIsInstance<String>()
                    .indexOfFirst { it.equals(genreName, true) }

                if (index != -1) {
                    sourceFilter.state = index
                    genreExists = true
                    break
                }
            }
        }
        mutableState.update {
            val listing = if (genreExists) {
                Listing.Search(query = null, filters = defaultFilters)
            } else {
                Listing.Search(query = genreName, filters = defaultFilters)
            }
            it.copy(
                filters = defaultFilters,
                listing = listing,
                toolbarQuery = listing.query,
            )
        }
    }

    /**
     * Adds or removes an anime from the library.
     *
     * @param anime the anime to update.
     */
    fun changeAnimeFavorite(anime: Anime) {
        screenModelScope.launch {
            var new = anime.copy(
                favorite = !anime.favorite,
                dateAdded = when (anime.favorite) {
                    true -> 0
                    false -> Instant.now().toEpochMilli()
                },
            )

            if (!new.favorite) {
                new = new.removeCovers(coverCache)
            } else {
                setAnimeDefaultEpisodeFlags.await(anime)
                addTracks.bindEnhancedTrackers(anime, source)
            }

            updateAnime.await(new.toAnimeUpdate())
        }
    }

    suspend fun addFavorite(anime: Anime) {
        val categories = getCategories()
        val defaultCategoryId = libraryPreferences.defaultCategory().get()
        val defaultCategory = categories.find { it.id == defaultCategoryId.toLong() }

        when {
            // Default category set
            defaultCategory != null -> {
                moveAnimeToCategories(anime, defaultCategory)

                changeAnimeFavorite(anime)
            }
            // Automatic 'Default' or no categories
            defaultCategoryId == 0 || categories.isEmpty() -> {
                moveAnimeToCategories(anime)

                changeAnimeFavorite(anime)
            }

            // Choose a category
            else -> {
                val preselectedIds = getCategories.await(anime.id).map { it.id }
                setDialog(
                    Dialog.ChangeAnimeCategory(
                        anime,
                        categories.mapAsCheckboxState { it.id in preselectedIds }.toImmutableList(),
                    ),
                )
            }
        }
    }

    fun toggleSelection(anime: Anime) {
        mutableState.update { state ->
            val newSelection = state.selection.mutate { list ->
                if (list.fastAny { it.id == anime.id }) {
                    list.removeAll { it.id == anime.id }
                } else {
                    list.add(anime)
                }
            }
            state.copy(selection = newSelection)
        }
    }

    fun selectAll(animeList: List<Anime>) {
        mutableState.update { state ->
            val newSelection = state.selection.mutate { list ->
                animeList.forEach { anime ->
                    if (list.none { it.id == anime.id }) {
                        list.add(anime)
                    }
                }
            }
            state.copy(
                selection = newSelection,
                isSelectAllMode = true,
                targetCount = if (state.targetCount == 0) 60 else state.targetCount
            )
        }
    }

    fun updateSelection(animeList: List<Anime>) {
        mutableState.update { state ->
            val newSelection = state.selection.mutate { list ->
                animeList.forEach { anime ->
                    if (list.none { it.id == anime.id }) {
                        list.add(anime)
                    }
                }
            }
            state.copy(selection = newSelection)
        }
    }

    fun setTargetCount(count: Int) {
        mutableState.update { it.copy(targetCount = count) }
    }

    fun invertSelection(animeList: List<Anime>) {
        mutableState.update { state ->
            val newSelection = state.selection.mutate { list ->
                animeList.forEach { anime ->
                    val index = list.indexOfFirst { it.id == anime.id }
                    if (index != -1) {
                        list.removeAt(index)
                    } else {
                        list.add(anime)
                    }
                }
            }
            // Invert selection turns off select all mode usually as it's a specific manual action
            state.copy(selection = newSelection, isSelectAllMode = false, targetCount = 0)
        }
    }

    fun clearSelection() {
        mutableState.update { it.copy(selection = persistentListOf(), isSelectAllMode = false, targetCount = 0) }
    }

    fun addSelectionToLibrary() {
        val selection = state.value.selection
        val favoriteIds = state.value.favoriteIds
        screenModelScope.launch {
            selection.forEach { anime ->
                if (anime.id !in favoriteIds) {
                    addFavorite(anime)
                }
            }
            clearSelection()
        }
    }

    fun removeSelectionFromLibrary() {
        val selection = state.value.selection
        val favoriteIds = state.value.favoriteIds
        screenModelScope.launch {
            selection.forEach { anime ->
                if (anime.id in favoriteIds) {
                    changeAnimeFavorite(anime)
                }
            }
            clearSelection()
        }
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<Category> {
        return getCategories.subscribe()
            .firstOrNull()
            ?.filterNot { it.isSystemCategory }
            .orEmpty()
    }

    suspend fun getDuplicateAnimelibAnime(anime: Anime): Anime? {
        return getDuplicateAnimelibAnime.await(anime).getOrNull(0)
    }

    private fun moveAnimeToCategories(anime: Anime, vararg categories: Category) {
        moveAnimeToCategories(anime, categories.filter { it.id != 0L }.map { it.id })
    }

    fun moveAnimeToCategories(anime: Anime, categoryIds: List<Long>) {
        screenModelScope.launchIO {
            setAnimeCategories.await(
                mangaId = anime.id,
                categoryIds = categoryIds.toList(),
            )
        }
    }

    fun openFilterSheet() {
        setDialog(Dialog.Filter)
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    fun setToolbarQuery(query: String?) {
        mutableState.update { it.copy(toolbarQuery = query) }
    }

    sealed class Listing(open val query: String?, open val filters: FilterList) {
        data object Popular : Listing(
            query = GetRemoteAnime.QUERY_POPULAR,
            filters = FilterList(),
        )
        data object Latest : Listing(
            query = GetRemoteAnime.QUERY_LATEST,
            filters = FilterList(),
        )
        data class Search(override val query: String?, override val filters: FilterList) : Listing(
            query = query,
            filters = filters,
        )

        companion object {
            fun valueOf(query: String?): Listing {
                return when (query) {
                    GetRemoteAnime.QUERY_POPULAR -> Popular
                    GetRemoteAnime.QUERY_LATEST -> Latest
                    else -> Search(query = query, filters = FilterList()) // filters are filled in later
                }
            }
        }
    }

    sealed interface Dialog {
        data object Filter : Dialog
        data class RemoveAnime(val anime: Anime) : Dialog
        data class AddDuplicateAnime(val anime: Anime, val duplicate: Anime) : Dialog
        data class ChangeAnimeCategory(
            val anime: Anime,
            val initialSelection: ImmutableList<CheckboxState.State<Category>>,
        ) : Dialog
        data class Migrate(val newAnime: Anime, val oldAnime: Anime) : Dialog
        data object SaveSearch : Dialog
        data class DeleteSavedSearch(val savedSearch: SavedSearch) : Dialog
    }

    @Immutable
    data class State(
        val listing: Listing,
        val filters: FilterList = FilterList(),
        val toolbarQuery: String? = null,
        val savedSearches: ImmutableList<SavedSearch> = persistentListOf(),
        val currentSavedSearch: SavedSearch? = null,
        val dialog: Dialog? = null,
        val selection: PersistentList<Anime> = persistentListOf(),
        val isSelectAllMode: Boolean = false,
        val favoriteIds: Set<Long> = emptySet(),
        val targetCount: Int = 0,
    ) {
        val isUserQuery get() = listing is Listing.Search && !listing.query.isNullOrEmpty()
        val selectionMode get() = selection.isNotEmpty()
    }
}
