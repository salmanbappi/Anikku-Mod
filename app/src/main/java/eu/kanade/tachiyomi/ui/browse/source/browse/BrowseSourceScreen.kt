package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.presentation.anime.DuplicateAnimeDialog
import eu.kanade.presentation.browse.BrowseSourceContent
import eu.kanade.presentation.browse.MissingSourceScreen
import eu.kanade.presentation.browse.components.BrowseSourceToolbar
import eu.kanade.presentation.browse.components.RemoveAnimeDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.browse.extension.details.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateDialog
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateDialogScreenModel
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.Constants
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only

data class BrowseSourceScreen(
    private val sourceId: Long,
    private val listingQuery: String?,
) : Screen(), AssistContentScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val screenModel = rememberScreenModel { BrowseSourceScreenModel(sourceId, listingQuery) }
        val state by screenModel.state.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val navigateUp: () -> Unit = {
            when {
                !state.isUserQuery && state.toolbarQuery != null -> screenModel.setToolbarQuery(null)

                else -> navigator.pop()
            }
        }

        if (screenModel.source is StubSource) {
            MissingSourceScreen(
                source = screenModel.source,
                navigateUp = navigateUp,
            )
            return
        }

        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val uriHandler = LocalUriHandler.current
        val snackbarHostState = remember { SnackbarHostState() }

        val onHelpClick = { uriHandler.openUri(LocalSource.HELP_URL) }
        val onWebViewClick = f@{
            val source = screenModel.source as? HttpSource ?: return@f
            navigator.push(
                WebViewScreen(
                    url = source.baseUrl,
                    initialTitle = source.name,
                    sourceId = source.id,
                ),
            )
        }

        LaunchedEffect(screenModel.source) {
            assistUrl = (screenModel.source as? HttpSource)?.baseUrl
        }

        var topBarHeight by remember { mutableIntStateOf(0) }
        val pagingFlow by screenModel.animePagerFlowFlow.collectAsState()
        val animeList = pagingFlow.collectAsLazyPagingItems()

        val entries = screenModel.getColumnsPreferenceForCurrentOrientation(LocalConfiguration.current.orientation)

        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .then(
                            if (entries > 0) {
                                Modifier.onGloballyPositioned { layoutCoordinates ->
                                    topBarHeight = layoutCoordinates.size.height
                                }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    BrowseSourceToolbar(
                        searchQuery = state.toolbarQuery,
                        onSearchQueryChange = screenModel::setToolbarQuery,
                        source = screenModel.source,
                        displayMode = screenModel.displayMode,
                        onDisplayModeChange = { screenModel.displayMode = it },
                        navigateUp = navigateUp,
                        onWebViewClick = onWebViewClick,
                        onHelpClick = onHelpClick,
                        onSettingsClick = { navigator.push(SourcePreferencesScreen(sourceId)) },
                        onSearch = screenModel::search,
                        selectedCount = state.selection.size,
                        onUnselectAll = screenModel::clearSelection,
                        onSelectAll = {
                            val items = animeList.itemSnapshotList.items.filterNotNull()
                            if (items.isNotEmpty()) {
                                screenModel.selectAll(items)
                            }
                        },
                        onInvertSelection = {
                            screenModel.invertSelection(animeList.itemSnapshotList.items.filterNotNull())
                        },
                    )

                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = MaterialTheme.padding.small),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                    ) {
                        FilterChip(
                            selected = state.listing == Listing.Popular,
                            onClick = {
                                screenModel.resetFilters()
                                screenModel.setListing(Listing.Popular)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(FilterChipDefaults.IconSize),
                                )
                            },
                            label = {
                                Text(text = stringResource(MR.strings.popular))
                            },
                        )
                        if ((screenModel.source as CatalogueSource).supportsLatest) {
                            FilterChip(
                                selected = state.listing == Listing.Latest,
                                onClick = {
                                    screenModel.resetFilters()
                                    screenModel.setListing(Listing.Latest)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.NewReleases,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(FilterChipDefaults.IconSize),
                                    )
                                },
                                label = {
                                    Text(text = stringResource(MR.strings.latest))
                                },
                            )
                        }
                        if (state.filters.isNotEmpty()) {
                            FilterChip(
                                selected = state.listing is Listing.Search && state.currentSavedSearch == null,
                                onClick = screenModel::openFilterSheet,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterList,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(FilterChipDefaults.IconSize),
                                    )
                                },
                                label = {
                                    Text(text = stringResource(MR.strings.action_filter))
                                },
                            )
                        }

                        if (state.savedSearches.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .size(width = 1.dp, height = FilterChipDefaults.Height),
                            )
                            state.savedSearches.forEach { savedSearch ->
                                FilterChip(
                                    selected = state.currentSavedSearch?.id == savedSearch.id,
                                    onClick = { screenModel.loadSearch(savedSearch) },
                                    label = { Text(text = savedSearch.name) },
                                )
                            }
                        }
                    }

                    HorizontalDivider()
                }
            },
            bottomBar = {
                val context = LocalContext.current
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.selectionMode,
                    enter = androidx.compose.animation.expandVertically(),
                    exit = androidx.compose.animation.shrinkVertically(),
                ) {
                    val allFavorite = remember(state.selection, state.favoriteIds) {
                        state.selection.all { it.id in state.favoriteIds }
                    }
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.large.copy(
                            bottomEnd = androidx.compose.foundation.shape.ZeroCornerSize,
                            bottomStart = androidx.compose.foundation.shape.ZeroCornerSize,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(
                                    androidx.compose.foundation.layout.WindowInsets.navigationBars
                                        .only(androidx.compose.foundation.layout.WindowInsetsSides.Bottom)
                                        .asPaddingValues(),
                                )
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            androidx.compose.material3.TextButton(
                                onClick = { 
                                    if (allFavorite) {
                                        screenModel.removeSelectionFromLibrary()
                                    } else {
                                        screenModel.addSelectionToLibrary()
                                    }
                                },
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = if (allFavorite) Icons.Outlined.Delete else Icons.Outlined.Favorite,
                                        contentDescription = null,
                                        tint = if (allFavorite) MaterialTheme.colorScheme.error else LocalContentColor.current
                                    )
                                    Text(
                                        text = stringResource(
                                            if (allFavorite) MR.strings.action_remove else MR.strings.add_to_library
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (allFavorite) MaterialTheme.colorScheme.error else LocalContentColor.current
                                    )
                                }
                            }
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { paddingValues ->
            // Reactive Selection Engine: Observes load state to expand selection in 'Select All' mode.
            val isSelectAllMode = state.isSelectAllMode
            val targetCount = state.targetCount
            val selectionSize = state.selection.size
            val itemCount = animeList.itemCount
            var isPoking by remember { mutableStateOf(false) }

            LaunchedEffect(isSelectAllMode, targetCount, itemCount) {
                if (isSelectAllMode) {
                    val snapshot = animeList.itemSnapshotList
                    val loadedItems = snapshot.items.filterNotNull()
                    
                    // Expand selection to available items, capped at the current targetCount.
                    if (loadedItems.size > selectionSize) {
                        val nextBatch = loadedItems.take(targetCount)
                        if (nextBatch.size > selectionSize) {
                            screenModel.updateSelection(nextBatch)
                        }
                    }

                    // TRIGGER THE NEXT PAGE (The Safe Poke) only if we haven't reached the manual targetCount
                    // AND we are not already poking.
                    if (selectionSize < targetCount && itemCount > 0 && itemCount < targetCount && !isPoking) {
                        val appendState = animeList.loadState.append
                        if (appendState is androidx.paging.LoadState.NotLoading && !appendState.endOfPaginationReached) {
                            isPoking = true
                            try {
                                animeList[itemCount - 1]
                            } catch (e: Exception) {}
                            // The next itemCount update will trigger this LaunchedEffect again and reset isPoking
                        }
                    }
                } else {
                    isPoking = false
                }
            }
            
            // Reset poking state when loading completes or state changes
            LaunchedEffect(animeList.loadState.append) {
                if (animeList.loadState.append is androidx.paging.LoadState.NotLoading) {
                    isPoking = false
                }
            }

            BrowseSourceContent(
                source = screenModel.source,
                animeList = animeList,
                columns = screenModel.getColumnsPreference(LocalConfiguration.current.orientation),
                entries = screenModel.getColumnsPreferenceForCurrentOrientation(LocalConfiguration.current.orientation),
                topBarHeight = topBarHeight,
                displayMode = screenModel.displayMode,
                snackbarHostState = snackbarHostState,
                contentPadding = paddingValues,
                onWebViewClick = onWebViewClick,
                onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
                onLocalSourceHelpClick = onHelpClick,
                onAnimeClick = {
                    if (state.selectionMode) {
                        screenModel.toggleSelection(it)
                    } else {
                        navigator.push((AnimeScreen(it.id, true)))
                    }
                },
                onAnimeLongClick = { anime ->
                    if (state.selectionMode) {
                        screenModel.toggleSelection(anime)
                    } else {
                        screenModel.toggleSelection(anime)
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                selection = state.selection,
                favoriteIds = state.favoriteIds,
                onBatchIncrement = { /* Manual increment only via Select All button */ },
            )
        }

        val onDismissRequest = { screenModel.setDialog(null) }
        when (val dialog = state.dialog) {
            is BrowseSourceScreenModel.Dialog.Filter -> {
                key(state.filtersId) {
                    SourceFilterSheet(
                        onDismissRequest = onDismissRequest,
                        filters = state.filters,
                        onReset = screenModel::resetFilters,
                        onSave = { screenModel.setDialog(BrowseSourceScreenModel.Dialog.SaveSearch) },
                        onFilter = { screenModel.search(filters = state.filters) },
                        onUpdate = screenModel::onFilterUpdate,
                        savedSearches = state.savedSearches,
                        currentSavedSearchId = state.currentSavedSearch?.id,
                        onSavedSearchClick = {
                            screenModel.loadSearch(it)
                            onDismissRequest()
                        },
                        onSavedSearchLongClick = {
                            screenModel.setDialog(BrowseSourceScreenModel.Dialog.DeleteSavedSearch(it))
                        },
                    )
                }
            }
            is BrowseSourceScreenModel.Dialog.DeleteSavedSearch -> {
                AlertDialog(
                    onDismissRequest = onDismissRequest,
                    title = { Text(text = "Delete Saved Search?") },
                    text = { Text(text = "Are you sure you want to delete '${dialog.savedSearch.name}'?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                screenModel.deleteSearch(dialog.savedSearch.id)
                                onDismissRequest()
                            },
                        ) {
                            Text(text = stringResource(MR.strings.action_ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismissRequest) {
                            Text(text = stringResource(MR.strings.action_cancel))
                        }
                    },
                )
            }
            is BrowseSourceScreenModel.Dialog.SaveSearch -> {
                var name by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = onDismissRequest,
                    title = { Text(text = "Save current search query?") },
                    text = {
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(text = "Name") },
                            singleLine = true,
                        )
                    },
                    confirmButton = {
                        TextButton(
                            enabled = name.isNotBlank(),
                            onClick = {
                                screenModel.saveSearch(name)
                                onDismissRequest()
                            },
                        ) {
                            Text(text = stringResource(MR.strings.action_ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismissRequest) {
                            Text(text = stringResource(MR.strings.action_cancel))
                        }
                    },
                )
            }
            is BrowseSourceScreenModel.Dialog.AddDuplicateAnime -> {
                DuplicateAnimeDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = { scope.launch { screenModel.addFavorite(dialog.anime) } },
                    onOpenAnime = { navigator.push(AnimeScreen(dialog.duplicate.id)) },
                    onMigrate = {
                        screenModel.setDialog(
                            BrowseSourceScreenModel.Dialog.Migrate(dialog.anime, dialog.duplicate),
                        )
                    },
                )
            }

            is BrowseSourceScreenModel.Dialog.Migrate -> {
                MigrateDialog(
                    oldAnime = dialog.oldAnime,
                    newAnime = dialog.newAnime,
                    screenModel = MigrateDialogScreenModel(),
                    onDismissRequest = onDismissRequest,
                    onClickTitle = { navigator.push(AnimeScreen(dialog.oldAnime.id)) },
                    onPopScreen = {
                        onDismissRequest()
                    },
                )
            }
            is BrowseSourceScreenModel.Dialog.RemoveAnime -> {
                RemoveAnimeDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        screenModel.changeAnimeFavorite(dialog.anime)
                    },
                    animeToRemove = dialog.anime.title,
                )
            }
            is BrowseSourceScreenModel.Dialog.ChangeAnimeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoryScreen) },
                    onConfirm = { include, _ ->
                        screenModel.changeAnimeFavorite(dialog.anime)
                        screenModel.moveAnimeToCategories(dialog.anime, include)
                    },
                )
            }
            else -> {}
        }

        // Reactive Selection Engine: Observes load state to expand selection in 'Select All' mode.
        // It selects items in batches of 60 and uses 'safe boundary access' to trigger Paging 3 fetches.
        LaunchedEffect(Unit) {
            queryEvent.receiveAsFlow()
                .collectLatest {
                    when (it) {
                        is SearchType.Genre -> screenModel.searchGenre(it.txt)
                        is SearchType.Text -> screenModel.search(it.txt)
                    }
                }
        }
    }

    suspend fun search(query: String) = queryEvent.send(SearchType.Text(query))
    suspend fun searchGenre(name: String) = queryEvent.send(SearchType.Genre(name))

    companion object {
        private val queryEvent = Channel<SearchType>()
    }

    sealed class SearchType(val txt: String) {
        class Text(txt: String) : SearchType(txt)
        class Genre(txt: String) : SearchType(txt)
    }
}
