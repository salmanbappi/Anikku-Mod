package eu.kanade.presentation.anime

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.anime.components.AnimeActionRow
import eu.kanade.presentation.anime.components.AnimeBottomActionMenu
import eu.kanade.presentation.anime.components.AnimeEpisodeListItem
import eu.kanade.presentation.anime.components.AnimeInfoBox
import eu.kanade.presentation.anime.components.AnimeToolbar
import eu.kanade.presentation.anime.components.AnimeSeasonSection
import eu.kanade.presentation.anime.components.EpisodeDownloadAction
import eu.kanade.presentation.anime.components.EpisodeHeader
import eu.kanade.presentation.anime.components.ExpandableAnimeDescription
import eu.kanade.presentation.anime.components.MissingEpisodeCountListItem
import eu.kanade.presentation.anime.components.NextEpisodeAiringListItem
import eu.kanade.presentation.components.relativeDateTimeText
import eu.kanade.presentation.theme.DynamicTachiyomiTheme
import eu.kanade.presentation.util.formatEpisodeNumber
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.getNameForAnimeInfo
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.ui.anime.AnimeScreenModel
import eu.kanade.tachiyomi.ui.anime.EpisodeList
import eu.kanade.tachiyomi.ui.anime.SuggestionSection
import eu.kanade.tachiyomi.ui.browse.extension.details.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.RelatedAnimeScreen
import eu.kanade.tachiyomi.ui.anime.track.TrackItem
import eu.kanade.tachiyomi.util.system.CoverColorObserver
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.coroutines.delay
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.asAnimeCover
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.service.missingEpisodesCount
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.TwoPanelBox
import tachiyomi.presentation.core.components.VerticalFastScroller
import tachiyomi.presentation.core.components.material.ExtendedFloatingActionButton
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.shouldExpandFAB
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.injectLazy
import java.time.Instant
import java.util.concurrent.TimeUnit

@Composable
fun AnimeScreen(
    state: AnimeScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    isTabletUi: Boolean,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    showNextEpisodeAirTime: Boolean,
    alwaysUseExternalPlayer: Boolean,
    showFileSize: Boolean,
    autoExpandDescription: Boolean,
    showSeasonsSection: Boolean,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (episode: Episode, alt: Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onTagSearch: (String) -> Unit,
    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueWatching: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,
    onCoverClicked: () -> Unit,
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditFetchIntervalClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    changeAnimeSkipIntro: (() -> Unit)?,
    onEditInfoClicked: () -> Unit,
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiFillermarkClicked: (List<Episode>, fillermarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,
    onEpisodeSwipe: (EpisodeList.Item, LibraryPreferences.EpisodeSwipeAction) -> Unit,
    onEpisodeSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onAllEpisodeSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onLocalScoreClicked: () -> Unit,
    onToggleDiscoveryExpansion: () -> Unit,
) {
    val sourcePreferences: SourcePreferences by injectLazy()
    val context = LocalContext.current
    val onCopyTagToClipboard: (tag: String) -> Unit = {
        if (it.isNotEmpty()) {
            context.copyToClipboard(it, it)
        }
    }

    val navigator = LocalNavigator.currentOrThrow
    val onSettingsClicked: (() -> Unit)? = {
        navigator.push(SourcePreferencesScreen(state.source.id))
    }.takeIf { state.source is ConfigurableSource }

    if (!isTabletUi) {
        AnimeScreenSmallImpl(
            state = state,
            sourcePreferences = sourcePreferences,
            snackbarHostState = snackbarHostState,
            nextUpdate = nextUpdate,
            episodeSwipeStartAction = episodeSwipeStartAction,
            episodeSwipeEndAction = episodeSwipeEndAction,
            showNextEpisodeAirTime = showNextEpisodeAirTime,
            alwaysUseExternalPlayer = alwaysUseExternalPlayer,
            showFileSize = showFileSize,
            autoExpandDescription = autoExpandDescription,
            showSeasonsSection = showSeasonsSection,
            onBackClicked = onBackClicked,
            onEpisodeClicked = onEpisodeClicked,
            onDownloadEpisode = onDownloadEpisode,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = onWebViewLongClicked,
            onTrackingClicked = onTrackingClicked,
            onTagSearch = onTagSearch,
            onCopyTagToClipboard = onCopyTagToClipboard,
            onFilterClicked = onFilterButtonClicked,
            onRefresh = onRefresh,
            onContinueWatching = onContinueWatching,
            onSearch = onSearch,
            onCoverClicked = onCoverClicked,
            onShareClicked = onShareClicked,
            onDownloadActionClicked = onDownloadActionClicked,
            onEditCategoryClicked = onEditCategoryClicked,
            onEditIntervalClicked = onEditFetchIntervalClicked,
            onMigrateClicked = onMigrateClicked,
            changeAnimeSkipIntro = changeAnimeSkipIntro,
            onEditInfoClicked = onEditInfoClicked,
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiFillermarkClicked = onMultiFillermarkClicked,
            onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
            onMarkPreviousAsSeenClicked = onMarkPreviousAsSeenClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
            onEpisodeSwipe = onEpisodeSwipe,
            onEpisodeSelected = onEpisodeSelected,
            onAllEpisodeSelected = onAllEpisodeSelected,
            onInvertSelection = onInvertSelection,
            onSettingsClicked = onSettingsClicked,
            onLocalScoreClicked = onLocalScoreClicked,
            onToggleDiscoveryExpansion = onToggleDiscoveryExpansion,
        )
    } else {
        AnimeScreenLargeImpl(
            state = state,
            sourcePreferences = sourcePreferences,
            snackbarHostState = snackbarHostState,
            nextUpdate = nextUpdate,
            episodeSwipeStartAction = episodeSwipeStartAction,
            episodeSwipeEndAction = episodeSwipeEndAction,
            showNextEpisodeAirTime = showNextEpisodeAirTime,
            alwaysUseExternalPlayer = alwaysUseExternalPlayer,
            showFileSize = showFileSize,
            autoExpandDescription = autoExpandDescription,
            showSeasonsSection = showSeasonsSection,
            onBackClicked = onBackClicked,
            onEpisodeClicked = onEpisodeClicked,
            onDownloadEpisode = onDownloadEpisode,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = onWebViewLongClicked,
            onTrackingClicked = onTrackingClicked,
            onTagSearch = onTagSearch,
            onCopyTagToClipboard = onCopyTagToClipboard,
            onFilterButtonClicked = onFilterButtonClicked,
            onRefresh = onRefresh,
            onContinueWatching = onContinueWatching,
            onSearch = onSearch,
            onCoverClicked = onCoverClicked,
            onShareClicked = onShareClicked,
            onDownloadActionClicked = onDownloadActionClicked,
            onEditCategoryClicked = onEditCategoryClicked,
            onEditIntervalClicked = onEditFetchIntervalClicked,
            changeAnimeSkipIntro = changeAnimeSkipIntro,
            onMigrateClicked = onMigrateClicked,
            onEditInfoClicked = onEditInfoClicked,
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiFillermarkClicked = onMultiFillermarkClicked,
            onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
            onMarkPreviousAsSeenClicked = onMarkPreviousAsSeenClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
            onEpisodeSwipe = onEpisodeSwipe,
            onEpisodeSelected = onEpisodeSelected,
            onAllEpisodeSelected = onAllEpisodeSelected,
            onInvertSelection = onInvertSelection,
            onSettingsClicked = onSettingsClicked,
            onLocalScoreClicked = onLocalScoreClicked,
            onToggleDiscoveryExpansion = onToggleDiscoveryExpansion,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimeScreenSmallImpl(
    state: AnimeScreenModel.State.Success,
    sourcePreferences: SourcePreferences,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    showNextEpisodeAirTime: Boolean,
    alwaysUseExternalPlayer: Boolean,
    showFileSize: Boolean,
    autoExpandDescription: Boolean,
    showSeasonsSection: Boolean,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,
    onFilterClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueWatching: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,
    onCoverClicked: () -> Unit,
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditIntervalClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    changeAnimeSkipIntro: (() -> Unit)?,
    onSettingsClicked: (() -> Unit)?,
    onEditInfoClicked: () -> Unit,
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiFillermarkClicked: (List<Episode>, fillermarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,
    onEpisodeSwipe: (EpisodeList.Item, LibraryPreferences.EpisodeSwipeAction) -> Unit,
    onEpisodeSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onAllEpisodeSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onLocalScoreClicked: () -> Unit,
    onToggleDiscoveryExpansion: () -> Unit,
) {
    val episodeListState = rememberLazyListState()
    val episodes = remember(state) { state.processedEpisodes }
    val listItem = remember(state) { state.episodeListItems }

            val isFirstItemVisible by remember {
                derivedStateOf { episodeListState.firstVisibleItemIndex == 0 }
            }
    
            val showSuggestions = sourcePreferences.relatedAnimeShowSource().collectAsState().value
    
            val isAnySelected by remember {
                derivedStateOf { episodes.fastAny { it.selected } }
            }
    
            val internalOnBackPressed = {
                if (isAnySelected) {
                    onAllEpisodeSelected(false)
                } else {
                    onBackClicked()
                }
            }
            BackHandler(onBack = internalOnBackPressed)
    
            val vibrantColors by CoverColorObserver.vibrantColors.collectAsState()
            val vibrantColor = vibrantColors[state.anime.id] ?: state.anime.asAnimeCover().vibrantCoverColor
    
            DynamicTachiyomiTheme(colorSeed = vibrantColor) {
                val backgroundColor = MaterialTheme.colorScheme.background
                val isLight = backgroundColor.luminance() > 0.5f
                val context = LocalContext.current
    
                LaunchedEffect(backgroundColor) {
                    val activity = context as? ComponentActivity ?: return@LaunchedEffect
                    val lightStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK)
                    val darkStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    activity.enableEdgeToEdge(
                        statusBarStyle = if (isLight) lightStyle else darkStyle,
                    )
                }
    
                        Box(
    
                            modifier = Modifier
    
                                .fillMaxSize()
    
                                .background(MaterialTheme.colorScheme.background),
    
                        ) {
    
                            Scaffold(
    
                                hazeEnabled = false,
    
                                floatingActionButton = {
                    val isFABVisible = remember(episodes) {
                        episodes.fastAny { !it.episode.seen } && !isAnySelected
                    }
                    AnimatedVisibility(
                        visible = isFABVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        val isWatching = remember(state.episodes) {
                            state.episodes.fastAny { it.episode.seen }
                        }
                        ExtendedFloatingActionButton(
                            text = {
                                Text(text = stringResource(if (isWatching) MR.strings.action_resume else MR.strings.action_start))
                            },
                            icon = {
                                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                            },
                            onClick = onContinueWatching,
                            expanded = episodeListState.shouldExpandFAB(),
                        )
                    }
                },
                topBar = {
                    val selectedEpisodeCount: Int = remember(episodes) {
                        episodes.count { it.selected }
                    }
                    val isFirstItemScrolled by remember {
                        derivedStateOf { episodeListState.firstVisibleItemScrollOffset > 0 }
                    }
                    val animatedTitleAlpha by animateFloatAsState(
                        targetValue = if (!isFirstItemVisible) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(200),
                        label = "Top Bar Title",
                    )
                    val animatedBgAlpha by animateFloatAsState(
                        targetValue = if (!isFirstItemVisible || isFirstItemScrolled) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(200),
                        label = "Top Bar Background",
                    )
                    AnimeToolbar(
                        title = state.anime.title,
                        titleAlphaProvider = { animatedTitleAlpha },
                        backgroundAlphaProvider = { animatedBgAlpha },
                        hasFilters = state.filterActive,
                        onBackClicked = internalOnBackPressed,
                        onClickFilter = onFilterClicked,
                        onClickShare = onShareClicked,
                        onClickDownload = onDownloadActionClicked,
                        onClickEditCategory = onEditCategoryClicked,
                        onClickRefresh = onRefresh,
                        onClickMigrate = onMigrateClicked,
                        onClickEditInfo = onEditInfoClicked.takeIf { state.anime.favorite },
                        onClickSettings = onSettingsClicked,
                        changeAnimeSkipIntro = changeAnimeSkipIntro,
                        actionModeCounter = selectedEpisodeCount,
                        onSelectAll = { onAllEpisodeSelected(true) },
                        onInvertSelection = { onInvertSelection() },
                    )
                },
                bottomBar = {
                    val selectedEpisodes = remember(episodes) {
                        episodes.filter { it.selected }
                    }
                    SharedAnimeBottomActionMenu(
                        selected = selectedEpisodes,
                        onEpisodeClicked = onEpisodeClicked,
                        onMultiBookmarkClicked = onMultiBookmarkClicked,
                        onMultiFillermarkClicked = onMultiFillermarkClicked,
                        onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
                        onMarkPreviousAsSeenClicked = onMarkPreviousAsSeenClicked,
                        onDownloadEpisode = onDownloadEpisode,
                        onMultiDeleteClicked = onMultiDeleteClicked,
                        onContinueWatching = onContinueWatching,
                        fillFraction = 1f,
                        alwaysUseExternalPlayer = alwaysUseExternalPlayer,
                    )
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            ) { contentPadding ->
                val topPadding = contentPadding.calculateTopPadding()
                PullRefresh(
                    refreshing = state.isRefreshingData,
                    onRefresh = onRefresh,
                    enabled = !isAnySelected,
                    indicatorPadding = PaddingValues(top = topPadding),
                ) {
                    val layoutDirection = LocalLayoutDirection.current
                    VerticalFastScroller(
                        listState = episodeListState,
                        topContentPadding = topPadding,
                        endContentPadding = contentPadding.calculateEndPadding(layoutDirection),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight(),
                            state = episodeListState,
                            contentPadding = PaddingValues(
                                start = contentPadding.calculateStartPadding(layoutDirection),
                                end = contentPadding.calculateEndPadding(layoutDirection),
                                bottom = contentPadding.calculateBottomPadding(),
                            ),
                        ) {
                            item(key = AnimeScreenItem.INFO_BOX, contentType = AnimeScreenItem.INFO_BOX) {
                                AnimeInfoBox(
                                    isTabletUi = false,
                                    appBarPadding = topPadding,
                                    anime = state.anime,
                                    totalScore = state.totalScore,
                                    sourceName = remember { state.source.getNameForAnimeInfo() },
                                    isStubSource = remember { state.source is StubSource },
                                    onCoverClick = onCoverClicked,
                                    doSearch = onSearch,
                                )
                            }
                            if (showSeasonsSection) {
                                item(key = "season-section", contentType = "season") {
                                    val navigator = LocalNavigator.currentOrThrow
                                    AnimeSeasonSection(
                                        seasons = state.seasons,
                                        onSeasonClick = { navigator.push(eu.kanade.tachiyomi.ui.anime.AnimeScreen(it)) },
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    )
                                }
                            }
                            item(key = AnimeScreenItem.ACTION_ROW, contentType = AnimeScreenItem.ACTION_ROW) {
                                val isWatching = remember(state.episodes) {
                                    state.episodes.fastAny { it.episode.seen }
                                }
                                AnimeActionRow(
                                    favorite = state.anime.favorite,
                                    trackingCount = state.trackingCount,
                                    nextUpdate = nextUpdate,
                                    isUserIntervalMode = state.anime.fetchInterval < 0,
                                    onAddToLibraryClicked = onAddToLibraryClicked,
                                    onWebViewClicked = onWebViewClicked,
                                    onWebViewLongClicked = onWebViewLongClicked,
                                    onTrackingClicked = onTrackingClicked,
                                    onEditIntervalClicked = onEditIntervalClicked,
                                    onEditCategory = onEditCategoryClicked,
                                    onContinueWatching = onContinueWatching,
                                    isWatching = isWatching,
                                    localScore = state.totalScore,
                                    onLocalScoreClicked = onLocalScoreClicked,
                                    mainTrackItem = remember(state.trackItems) { state.trackItems.find { it.tracker.id == 999L } ?: state.trackItems.firstOrNull() },
                                )
                            }

                            if (showSuggestions && state.suggestionSections.isNotEmpty()) {
                                item(key = "discovery-section-container", contentType = "discovery") {
                                    val navigator = LocalNavigator.currentOrThrow
                                    Surface(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .animateContentSize(),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = MaterialTheme.shapes.medium,
                                    ) {
                                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                            DiscoveryHeader(
                                                expanded = state.discoveryExpanded,
                                                onToggle = onToggleDiscoveryExpansion
                                            )

                                            if (!state.discoveryExpanded) {
                                                val combinedItems = remember(state.suggestionSections) {
                                                    state.suggestionSections.flatMap { it.items.take(3) }
                                                        .distinctBy { it.id }.take(15)
                                                }
                                                androidx.compose.foundation.lazy.LazyRow(
                                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    itemsIndexed(
                                                        items = combinedItems,
                                                        key = { index: Int, anime: tachiyomi.domain.anime.model.Anime -> "suggestion-combined-${anime.id}-$index" },
                                                    ) { _: Int, anime: tachiyomi.domain.anime.model.Anime ->
                                                        SuggestionItem(
                                                            anime = anime,
                                                            onClick = { navigator.push(eu.kanade.tachiyomi.ui.anime.AnimeScreen(anime.id)) }
                                                        )
                                                    }
                                                }
                                            } else {
                                                state.suggestionSections.forEach { section ->
                                                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            val icon = when (section.type) {
                                                                SuggestionSection.Type.Franchise -> androidx.compose.material.icons.Icons.Filled.AutoAwesome
                                                                SuggestionSection.Type.Similarity -> androidx.compose.material.icons.Icons.Outlined.Compare
                                                                SuggestionSection.Type.Author -> androidx.compose.material.icons.Icons.Outlined.Person
                                                                SuggestionSection.Type.Source -> androidx.compose.material.icons.Icons.Outlined.Language
                                                                SuggestionSection.Type.Tag -> androidx.compose.material.icons.Icons.Outlined.Label
                                                                SuggestionSection.Type.Community -> androidx.compose.material.icons.Icons.Outlined.NewReleases
                                                            }
                                                            val label = when (section.type) {
                                                                SuggestionSection.Type.Franchise -> stringResource(KMR.strings.related_mangas_website_suggestions)
                                                                SuggestionSection.Type.Similarity -> stringResource(SYMR.strings.relation_similar)
                                                                SuggestionSection.Type.Author -> section.title
                                                                SuggestionSection.Type.Source -> section.title
                                                                SuggestionSection.Type.Tag -> stringResource(SYMR.strings.az_recommends)
                                                                SuggestionSection.Type.Community -> stringResource(MR.strings.latest)
                                                            }
                                                            Icon(
                                                                imageVector = icon,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp),
                                                                tint = MaterialTheme.colorScheme.secondary
                                                            )
                                                            Text(
                                                                text = label,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.secondary
                                                            )
                                                        }
                                                        androidx.compose.foundation.lazy.LazyRow(
                                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        ) {
                                                            itemsIndexed(
                                                                items = section.items,
                                                                key = { index: Int, anime: tachiyomi.domain.anime.model.Anime -> "suggestion-${section.type}-${anime.id}-$index" },
                                                            ) { _: Int, anime: tachiyomi.domain.anime.model.Anime ->
                                                                SuggestionItem(
                                                                    anime = anime,
                                                                    onClick = { navigator.push(eu.kanade.tachiyomi.ui.anime.AnimeScreen(anime.id)) }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item(key = AnimeScreenItem.DESCRIPTION_WITH_TAG, contentType = AnimeScreenItem.DESCRIPTION_WITH_TAG) {
                                ExpandableAnimeDescription(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    defaultExpandState = autoExpandDescription,
                                    description = state.anime.description,
                                    tagsProvider = { state.anime.genre },
                                    onTagSearch = onTagSearch,
                                    onCopyTagToClipboard = onCopyTagToClipboard,
                                )
                            }
                            
                            item(key = AnimeScreenItem.EPISODE_HEADER, contentType = AnimeScreenItem.EPISODE_HEADER) {
                                val missingEpisodeCount = remember(episodes) {
                                    episodes.map { it.episode.episodeNumber }.missingEpisodesCount()
                                }
                                EpisodeHeader(
                                    enabled = !isAnySelected,
                                    episodeCount = episodes.size,
                                    missingEpisodeCount = missingEpisodeCount,
                                    onClick = onFilterClicked,
                                )
                            }
                            if (state.airingTime > 0L) {
                                item(key = AnimeScreenItem.AIRING_TIME, contentType = AnimeScreenItem.AIRING_TIME) {
                                    var timer by remember { mutableLongStateOf(state.airingTime) }
                                    LaunchedEffect(key1 = timer) {
                                        if (timer > 0L) {
                                            delay(1000L)
                                            timer -= 1000L
                                        }
                                    }
                                    if (timer > 0L && showNextEpisodeAirTime && state.anime.status.toInt() != SAnime.COMPLETED) {
                                        NextEpisodeAiringListItem(
                                            title = stringResource(
                                                MR.strings.display_mode_episode,
                                                formatEpisodeNumber(state.airingEpisodeNumber),
                                            ),
                                            date = formatTime(state.airingTime, useDayFormat = true),
                                        )
                                    }
                                }
                            }
                            sharedEpisodeItems(
                                anime = state.anime,
                                source = state.source,
                                showFileSize = showFileSize,
                                episodes = listItem,
                                isAnyEpisodeSelected = episodes.fastAny { it.selected },
                                episodeSwipeStartAction = episodeSwipeStartAction,
                                episodeSwipeEndAction = episodeSwipeEndAction,
                                onEpisodeClicked = onEpisodeClicked,
                                onDownloadEpisode = onDownloadEpisode,
                                onEpisodeSelected = onEpisodeSelected,
                                onEpisodeSwipe = onEpisodeSwipe,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeScreenLargeImpl(
    state: AnimeScreenModel.State.Success,
    sourcePreferences: SourcePreferences,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    showNextEpisodeAirTime: Boolean,
    alwaysUseExternalPlayer: Boolean,
    showFileSize: Boolean,
    autoExpandDescription: Boolean,
    showSeasonsSection: Boolean,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,
    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueWatching: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,
    onCoverClicked: () -> Unit,
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditIntervalClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    changeAnimeSkipIntro: (() -> Unit)?,
    onSettingsClicked: (() -> Unit)?,
    onEditInfoClicked: () -> Unit,
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiFillermarkClicked: (List<Episode>, fillermarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,
    onEpisodeSwipe: (EpisodeList.Item, LibraryPreferences.EpisodeSwipeAction) -> Unit,
    onEpisodeSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onAllEpisodeSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onLocalScoreClicked: () -> Unit,
    onToggleDiscoveryExpansion: () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val episodes = remember(state) { state.processedEpisodes }
    val listItem = remember(state) { state.episodeListItems }

    val showSuggestions = sourcePreferences.relatedAnimeShowSource().collectAsState().value

    val isAnySelected by remember {
        derivedStateOf { episodes.fastAny { it.selected } }
    }
    val insetPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
    var topBarHeight by remember { mutableIntStateOf(0) }
    val episodeListState = rememberLazyListState()
    val infoScrollState = rememberScrollState()

    val isFirstItemVisible by remember {
        derivedStateOf { episodeListState.firstVisibleItemIndex == 0 }
    }

    val internalOnBackPressed = {
        if (isAnySelected) {
            onAllEpisodeSelected(false)
        } else {
            onBackClicked()
        }
    }
    BackHandler(onBack = internalOnBackPressed)

    val vibrantColors by CoverColorObserver.vibrantColors.collectAsState()
    val vibrantColor = vibrantColors[state.anime.id] ?: state.anime.asAnimeCover().vibrantCoverColor

    DynamicTachiyomiTheme(colorSeed = vibrantColor) {
        val backgroundColor = MaterialTheme.colorScheme.background
        val isLight = backgroundColor.luminance() > 0.5f
        val context = LocalContext.current

        LaunchedEffect(backgroundColor) {
            val activity = context as? ComponentActivity ?: return@LaunchedEffect
            val lightStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK)
            val darkStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            activity.enableEdgeToEdge(
                statusBarStyle = if (isLight) lightStyle else darkStyle,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Scaffold(
                hazeEnabled = false,
                floatingActionButton = {
                    val isFABVisible = remember(episodes) {
                        episodes.fastAny { !it.episode.seen } && !isAnySelected
                    }
                    AnimatedVisibility(
                        visible = isFABVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        val isWatching = remember(state.episodes) {
                            state.episodes.fastAny { it.episode.seen }
                        }
                        ExtendedFloatingActionButton(
                            text = {
                                Text(text = stringResource(if (isWatching) MR.strings.action_resume else MR.strings.action_start))
                            },
                            icon = {
                                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                            },
                            onClick = onContinueWatching,
                            expanded = episodeListState.shouldExpandFAB(),
                        )
                    }
                },
                topBar = {
                    val selectedEpisodeCount = remember(episodes) {
                        episodes.count { it.selected }
                    }
                    val isFirstItemScrolled by remember {
                        derivedStateOf { episodeListState.firstVisibleItemScrollOffset > 0 }
                    }
                    val animatedTitleAlpha by animateFloatAsState(
                        targetValue = if (!isFirstItemVisible) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(200),
                        label = "Top Bar Title",
                    )
                    val animatedBgAlpha by animateFloatAsState(
                        targetValue = if (!isFirstItemVisible || isFirstItemScrolled) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(200),
                        label = "Top Bar Background",
                    )
                    AnimeToolbar(
                        modifier = Modifier.onSizeChanged { topBarHeight = it.height },
                        title = state.anime.title,
                        titleAlphaProvider = { if (isAnySelected) 1f else animatedTitleAlpha },
                        backgroundAlphaProvider = { animatedBgAlpha },
                        hasFilters = state.filterActive,
                        onBackClicked = internalOnBackPressed,
                        onClickFilter = onFilterButtonClicked,
                        onClickShare = onShareClicked,
                        onClickDownload = onDownloadActionClicked,
                        onClickEditCategory = onEditCategoryClicked,
                        onClickRefresh = onRefresh,
                        onClickMigrate = onMigrateClicked,
                        onClickSettings = onSettingsClicked,
                        changeAnimeSkipIntro = changeAnimeSkipIntro,
                        onClickEditInfo = onEditInfoClicked.takeIf { state.anime.favorite },
                        actionModeCounter = selectedEpisodeCount,
                        onSelectAll = { onAllEpisodeSelected(true) },
                        onInvertSelection = { onInvertSelection() },
                    )
                },
                bottomBar = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.BottomEnd,
                    ) {
                        val selectedEpisodes = remember(episodes) {
                            episodes.filter { it.selected }
                        }
                        SharedAnimeBottomActionMenu(
                            selected = selectedEpisodes,
                            onEpisodeClicked = onEpisodeClicked,
                            onMultiBookmarkClicked = onMultiBookmarkClicked,
                            onMultiFillermarkClicked = onMultiFillermarkClicked,
                            onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
                            onMarkPreviousAsSeenClicked = onMarkPreviousAsSeenClicked,
                            onDownloadEpisode = onDownloadEpisode,
                            onMultiDeleteClicked = onMultiDeleteClicked,
                            onContinueWatching = onContinueWatching,
                            fillFraction = 0.5f,
                            alwaysUseExternalPlayer = alwaysUseExternalPlayer,
                        )
                    }
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            ) { contentPadding ->
                PullRefresh(
                    refreshing = state.isRefreshingData,
                    onRefresh = onRefresh,
                    enabled = !isAnySelected,
                    indicatorPadding = PaddingValues(
                        start = insetPadding.calculateStartPadding(layoutDirection),
                        top = with(density) { topBarHeight.toDp() },
                        end = insetPadding.calculateEndPadding(layoutDirection),
                    ),
                ) {
                    TwoPanelBox(
                        modifier = Modifier.padding(
                            start = contentPadding.calculateStartPadding(layoutDirection),
                            end = contentPadding.calculateEndPadding(layoutDirection),
                        ),
                        startContent = {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(infoScrollState)
                                    .padding(bottom = contentPadding.calculateBottomPadding()),
                            ) {
                                AnimeInfoBox(
                                    isTabletUi = true,
                                    appBarPadding = contentPadding.calculateTopPadding(),
                                    anime = state.anime,
                                    totalScore = state.totalScore,
                                    sourceName = remember { state.source.getNameForAnimeInfo() },
                                    isStubSource = remember { state.source is StubSource },
                                    onCoverClick = onCoverClicked,
                                    doSearch = onSearch,
                                )
                                if (showSeasonsSection) {
                                    val navigator = LocalNavigator.currentOrThrow
                                    AnimeSeasonSection(
                                        seasons = state.seasons,
                                        onSeasonClick = { navigator.push(eu.kanade.tachiyomi.ui.anime.AnimeScreen(it)) },
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    )
                                }
                                val isWatching = remember(state.episodes) {
                                    state.episodes.fastAny { it.episode.seen }
                                }
                                AnimeActionRow(
                                    favorite = state.anime.favorite,
                                    trackingCount = state.trackingCount,
                                    nextUpdate = nextUpdate,
                                    isUserIntervalMode = state.anime.fetchInterval < 0,
                                    onAddToLibraryClicked = onAddToLibraryClicked,
                                    onWebViewClicked = onWebViewClicked,
                                    onWebViewLongClicked = onWebViewLongClicked,
                                    onTrackingClicked = onTrackingClicked,
                                    onEditIntervalClicked = onEditIntervalClicked,
                                    onEditCategory = onEditCategoryClicked,
                                    onContinueWatching = onContinueWatching,
                                    isWatching = isWatching,
                                    localScore = state.totalScore,
                                    onLocalScoreClicked = onLocalScoreClicked,
                                    mainTrackItem = remember(state.trackItems) { state.trackItems.find { it.tracker.id == 999L } ?: state.trackItems.firstOrNull() },
                                )

                                if (showSuggestions && state.suggestionSections.isNotEmpty()) {
                                    val navigator = LocalNavigator.currentOrThrow
                                    Surface(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = MaterialTheme.shapes.medium,
                                    ) {
                                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                            DiscoveryHeader(expanded = state.discoveryExpanded, onToggle = onToggleDiscoveryExpansion)
                                            
                                            if (!state.discoveryExpanded) {
                                                val combinedItems = remember(state.suggestionSections) {
                                                    state.suggestionSections.flatMap { it.items.take(3) }.distinctBy { it.id }.take(15)
                                                }
                                                androidx.compose.foundation.lazy.LazyRow(
                                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    itemsIndexed(
                                                        items = combinedItems,
                                                        key = { index: Int, anime: tachiyomi.domain.anime.model.Anime -> "suggestion-combined-large-${anime.id}-$index" },
                                                    ) { _: Int, anime: tachiyomi.domain.anime.model.Anime ->
                                                        SuggestionItem(anime = anime, onClick = { navigator.push(eu.kanade.tachiyomi.ui.anime.AnimeScreen(anime.id)) })
                                                    }
                                                }
                                            } else {
                                                state.suggestionSections.forEach { section ->
                                                    Column(modifier = Modifier.padding(top = 4.dp)) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            val icon = when (section.type) {
                                                                SuggestionSection.Type.Franchise -> androidx.compose.material.icons.Icons.Filled.AutoAwesome
                                                                SuggestionSection.Type.Similarity -> androidx.compose.material.icons.Icons.Outlined.Compare
                                                                SuggestionSection.Type.Author -> androidx.compose.material.icons.Icons.Outlined.Person
                                                                SuggestionSection.Type.Source -> androidx.compose.material.icons.Icons.Outlined.Language
                                                                SuggestionSection.Type.Tag -> androidx.compose.material.icons.Icons.Outlined.Label
                                                                SuggestionSection.Type.Community -> androidx.compose.material.icons.Icons.Outlined.NewReleases
                                                            }
                                                            val label = when (section.type) {
                                                                SuggestionSection.Type.Franchise -> stringResource(KMR.strings.related_mangas_website_suggestions)
                                                                SuggestionSection.Type.Similarity -> stringResource(SYMR.strings.relation_similar)
                                                                SuggestionSection.Type.Author -> section.title
                                                                SuggestionSection.Type.Source -> section.title
                                                                SuggestionSection.Type.Tag -> stringResource(SYMR.strings.az_recommends)
                                                                SuggestionSection.Type.Community -> stringResource(MR.strings.latest)
                                                            }
                                                            Icon(
                                                                imageVector = icon,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp),
                                                                tint = MaterialTheme.colorScheme.secondary
                                                            )
                                                            Text(
                                                                text = label,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.secondary
                                                            )
                                                        }
                                                        androidx.compose.foundation.lazy.LazyRow(
                                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        ) {
                                                            itemsIndexed(
                                                                items = section.items,
                                                                key = { index: Int, anime: tachiyomi.domain.anime.model.Anime -> "suggestion-large-${section.type}-${anime.id}-$index" },
                                                            ) { _: Int, anime: tachiyomi.domain.anime.model.Anime ->
                                                                SuggestionItem(anime = anime, onClick = { navigator.push(eu.kanade.tachiyomi.ui.anime.AnimeScreen(anime.id)) })
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                ExpandableAnimeDescription(
                                    defaultExpandState = autoExpandDescription,
                                    description = state.anime.description,
                                    tagsProvider = { state.anime.genre },
                                    onTagSearch = onTagSearch,
                                    onCopyTagToClipboard = onCopyTagToClipboard,
                                )
                            }
                        },
                                    
                        endContent = {
                            VerticalFastScroller(
                                listState = episodeListState,
                                topContentPadding = contentPadding.calculateTopPadding(),
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxHeight(),
                                    state = episodeListState,
                                    contentPadding = PaddingValues(
                                        top = contentPadding.calculateTopPadding(),
                                        bottom = contentPadding.calculateBottomPadding(),
                                    ),
                                ) {
                                    item(key = AnimeScreenItem.EPISODE_HEADER, contentType = AnimeScreenItem.EPISODE_HEADER) {
                                        val missingEpisodeCount = remember(episodes) {
                                            episodes.map { it.episode.episodeNumber }.missingEpisodesCount()
                                        }
                                        EpisodeHeader(
                                            enabled = !isAnySelected,
                                            episodeCount = episodes.size,
                                            missingEpisodeCount = missingEpisodeCount,
                                            onClick = onFilterButtonClicked,
                                        )
                                    }
                                    if (state.airingTime > 0L) {
                                        item(key = AnimeScreenItem.AIRING_TIME, contentType = AnimeScreenItem.AIRING_TIME) {
                                            var timer by remember { mutableLongStateOf(state.airingTime) }
                                            LaunchedEffect(key1 = timer) {
                                                if (timer > 0L) {
                                                    delay(1000L)
                                                    timer -= 1000L
                                                }
                                            }
                                            if (timer > 0L && showNextEpisodeAirTime && state.anime.status.toInt() != SAnime.COMPLETED) {
                                                NextEpisodeAiringListItem(
                                                    title = stringResource(
                                                        MR.strings.display_mode_episode,
                                                        formatEpisodeNumber(state.airingEpisodeNumber),
                                                    ),
                                                    date = formatTime(state.airingTime, useDayFormat = true),
                                                )
                                            }
                                        }
                                    }
                                    sharedEpisodeItems(
                                        anime = state.anime,
                                        source = state.source,
                                        showFileSize = showFileSize,
                                        episodes = listItem,
                                        isAnyEpisodeSelected = episodes.fastAny { it.selected },
                                        episodeSwipeStartAction = episodeSwipeStartAction,
                                        episodeSwipeEndAction = episodeSwipeEndAction,
                                        onEpisodeClicked = onEpisodeClicked,
                                        onDownloadEpisode = onDownloadEpisode,
                                        onEpisodeSelected = onEpisodeSelected,
                                        onEpisodeSwipe = onEpisodeSwipe,
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeItemWrapper(
    item: EpisodeList,
    anime: Anime,
    source: Source,
    showFileSize: Boolean,
    isAnyEpisodeSelected: Boolean,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onEpisodeSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onEpisodeSwipe: (EpisodeList.Item, LibraryPreferences.EpisodeSwipeAction) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    when (item) {
        is EpisodeList.MissingCount -> {
            MissingEpisodeCountListItem(count = item.count)
        }
        is EpisodeList.Item -> {
            var fileSizeAsync: Long? by remember { mutableStateOf(item.fileSize) }
            val isEpisodeDownloaded = item.downloadState == Download.State.DOWNLOADED
            if (isEpisodeDownloaded && showFileSize && fileSizeAsync == null) {
                LaunchedEffect(item, Unit) {
                    fileSizeAsync = withIOContext {
                        downloadProvider.getEpisodeFileSize(
                            item.episode.name,
                            item.episode.url,
                            item.episode.scanlator,
                            anime.ogTitle,
                            source,
                        )
                    }
                    item.fileSize = fileSizeAsync
                }
            }
            AnimeEpisodeListItem(
                title = if (anime.displayMode == Anime.EPISODE_DISPLAY_NUMBER) {
                    stringResource(
                        MR.strings.display_mode_episode,
                        formatEpisodeNumber(item.episode.episodeNumber),
                    )
                } else {
                    item.episode.name
                },
                date = relativeDateTimeText(item.episode.dateUpload),
                watchProgress = item.episode.lastSecondSeen
                    .takeIf { !item.episode.seen && it > 0L }
                    ?.let {
                        stringResource(
                            MR.strings.episode_progress,
                            formatTime(it),
                            formatTime(item.episode.totalSeconds),
                        )
                    },
                scanlator = item.episode.scanlator.takeIf { !it.isNullOrBlank() },
                seen = item.episode.seen,
                bookmark = item.episode.bookmark,
                fillermark = item.episode.fillermark,
                selected = item.selected,
                downloadIndicatorEnabled = !isAnyEpisodeSelected && !anime.isLocal(),
                downloadStateProvider = { item.downloadState },
                downloadProgressProvider = { item.downloadProgress },
                episodeSwipeStartAction = episodeSwipeStartAction,
                episodeSwipeEndAction = episodeSwipeEndAction,
                onLongClick = {
                    onEpisodeSelected(item, !item.selected, true, true)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onClick = {
                    onEpisodeItemClick(
                        episodeItem = item,
                        isAnyEpisodeSelected = isAnyEpisodeSelected,
                        onToggleSelection = { onEpisodeSelected(item, !item.selected, true, false) },
                        onEpisodeClicked = onEpisodeClicked,
                    )
                },
                onDownloadClick = if (onDownloadEpisode != null) {
                    { onDownloadEpisode(listOf(item), it) }
                } else {
                    null
                },
                onEpisodeSwipe = {
                    onEpisodeSwipe(item, it)
                },
                fileSize = fileSizeAsync,
            )
        }
    }
}

@Composable
private fun SharedAnimeBottomActionMenu(
    selected: List<EpisodeList.Item>,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiFillermarkClicked: (List<Episode>, fillermarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onDownloadEpisode: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onMultiDeleteClicked: (List<Episode>) -> Unit,
    onContinueWatching: () -> Unit,
    fillFraction: Float,
    alwaysUseExternalPlayer: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimeBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = modifier.fillMaxWidth(fillFraction),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected.fastMap { it.episode }, true)
        }.takeIf { selected.fastAny { !it.episode.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected.fastMap { it.episode }, false)
        }.takeIf { selected.fastAll { it.episode.bookmark } },
        onFillermarkClicked = {
            onMultiFillermarkClicked.invoke(selected.fastMap { it.episode }, true)
        }.takeIf { selected.fastAny { !it.episode.fillermark } },
        onRemoveFillermarkClicked = {
            onMultiFillermarkClicked.invoke(selected.fastMap { it.episode }, false)
        }.takeIf { selected.fastAll { it.episode.fillermark } },
        onMarkAsSeenClicked = {
            onMultiMarkAsSeenClicked(selected.fastMap { it.episode }, true)
        }.takeIf { selected.fastAny { !it.episode.seen } },
        onMarkAsUnseenClicked = {
            onMultiMarkAsSeenClicked(selected.fastMap { it.episode }, false)
        }.takeIf { selected.fastAny { it.episode.seen || it.episode.lastSecondSeen > 0L } },
        onMarkPreviousAsSeenClicked = {
            onMarkPreviousAsSeenClicked(selected[0].episode)
        }.takeIf { selected.size == 1 },
        onDownloadClicked = {
            onDownloadEpisode!!(selected.toList(), EpisodeDownloadAction.START)
        }.takeIf {
            onDownloadEpisode != null && selected.fastAny { it.downloadState != Download.State.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected.fastMap { it.episode })
        }.takeIf {
            onDownloadEpisode != null && selected.fastAny { it.downloadState == Download.State.DOWNLOADED }
        },
        onExternalClicked = {
            onEpisodeClicked(selected.fastMap { it.episode }.first(), true)
        }.takeIf { !alwaysUseExternalPlayer && selected.size == 1 },
        onInternalClicked = {
            onEpisodeClicked(selected.fastMap { it.episode }.first(), true)
        }.takeIf { alwaysUseExternalPlayer && selected.size == 1 }
    )
}

private fun onEpisodeItemClick(
    episodeItem: EpisodeList.Item,
    isAnyEpisodeSelected: Boolean,
    onToggleSelection: (Boolean) -> Unit,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
) {
    when {
        episodeItem.selected -> onToggleSelection(false)
        isAnyEpisodeSelected -> onToggleSelection(true)
        else -> onEpisodeClicked(episodeItem.episode, false)
    }
}

private fun formatTime(milliseconds: Long, useDayFormat: Boolean = false): String {
    return if (useDayFormat) {
        String.format(
            "Airing in %02dd %02dh %02dm %02ds",
            TimeUnit.MILLISECONDS.toDays(milliseconds),
            TimeUnit.MILLISECONDS.toHours(milliseconds) -
                TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(milliseconds)),
            TimeUnit.MILLISECONDS.toMinutes(milliseconds) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds)),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    } else if (milliseconds > 3600000L) {
        String.format(
            "%d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(milliseconds),
            TimeUnit.MILLISECONDS.toMinutes(milliseconds) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds)),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    } else {
        String.format(
            "%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    }
}

private val downloadProvider: DownloadProvider by injectLazy()

@Composable
private fun SuggestionItem(
    anime: Anime,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.width(112.dp)) {
        val scoreText = remember(anime.score) {
            if (anime.score != null && anime.score!! > 0) {
                String.format("%.1f", anime.score)
            } else null
        }
        eu.kanade.presentation.library.components.AnimeComfortableGridItem(
            title = anime.title,
            coverData = remember(anime.id) {
                tachiyomi.domain.anime.model.AnimeCover(
                    animeId = anime.id,
                    sourceId = anime.source,
                    isAnimeFavorite = anime.favorite,
                    ogUrl = anime.thumbnailUrl,
                    lastModified = anime.coverLastModified,
                )
            },
            coverBadgeStart = {
                eu.kanade.presentation.browse.components.InLibraryBadge(enabled = anime.favorite)
            },
            coverBadgeEnd = {
                if (scoreText != null) {
                    tachiyomi.presentation.core.components.Badge(
                        text = scoreText,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            },
            onClick = onClick,
            onLongClick = {},
        )
    }
}

@Composable
private fun DiscoveryHeader(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Recommended for you",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        TextButton(
            onClick = onToggle,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.height(28.dp).alignByBaseline()
        ) {
            Text(
                text = if (expanded) "Collapse" else stringResource(tachiyomi.i18n.MR.strings.label_more),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun LazyListScope.sharedEpisodeItems(
    anime: Anime,
    source: Source,
    showFileSize: Boolean,
    episodes: List<EpisodeList>,
    isAnyEpisodeSelected: Boolean,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onEpisodeSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onEpisodeSwipe: (EpisodeList.Item, LibraryPreferences.EpisodeSwipeAction) -> Unit,
) {
    itemsIndexed(
        items = episodes,
        key = { index, item ->
            when (item) {
                is EpisodeList.Item -> "ep-${item.episode.id}-$index"
                is EpisodeList.MissingCount -> "ms-${item.id}-$index"
            }
        },
    ) { _, item ->
        EpisodeItemWrapper(
            item = item,
            anime = anime,
            source = source,
            showFileSize = showFileSize,
            isAnyEpisodeSelected = isAnyEpisodeSelected,
            episodeSwipeStartAction = episodeSwipeStartAction,
            episodeSwipeEndAction = episodeSwipeEndAction,
            onEpisodeClicked = onEpisodeClicked,
            onDownloadEpisode = onDownloadEpisode,
            onEpisodeSelected = onEpisodeSelected,
            onEpisodeSwipe = onEpisodeSwipe,
        )
    }
}
