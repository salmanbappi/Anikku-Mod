package eu.kanade.presentation.anime

import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import androidx.palette.graphics.Palette
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.BitmapImage
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import eu.kanade.presentation.anime.components.AnimeActionRow
import eu.kanade.presentation.anime.components.AnimeBottomActionMenu
import eu.kanade.presentation.anime.components.AnimeEpisodeListItem
import eu.kanade.presentation.anime.components.AnimeInfoBox
import eu.kanade.presentation.anime.components.AnimeToolbar
import eu.kanade.presentation.anime.components.EpisodeDownloadAction
import eu.kanade.presentation.anime.components.EpisodeHeader
import eu.kanade.presentation.anime.components.ExpandableAnimeDescription
import eu.kanade.presentation.anime.components.MissingEpisodeCountListItem
import eu.kanade.presentation.anime.components.NextEpisodeAiringListItem
import eu.kanade.presentation.anime.components.RecommendationsComponent
import eu.kanade.presentation.components.relativeDateTimeText
import eu.kanade.presentation.library.components.AnimeComfortableGridItem
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.presentation.util.formatEpisodeNumber
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.getNameForAnimeInfo
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.anime.AnimeScreenModel
import eu.kanade.tachiyomi.ui.anime.EpisodeList
import eu.kanade.tachiyomi.ui.browse.extension.details.SourcePreferencesScreen
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.coroutines.delay
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.VerticalFastScroller
import tachiyomi.presentation.core.components.material.ExtendedFloatingActionButton
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import androidx.compose.material3.SnackbarHost
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.shouldExpandFAB
import java.time.Instant

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
    onBackClicked: () -> Unit,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: (List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onTagSearch: (String) -> Unit,
    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueWatching: () -> Unit,
    onSearch: (String, Boolean) -> Unit,
    onCoverClicked: () -> Unit,
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: (DownloadAction) -> Unit,
    onEditCategoryClicked: () -> Unit,
    onEditFetchIntervalClicked: () -> Unit,
    onMigrateClicked: () -> Unit,
    changeAnimeSkipIntro: () -> Unit,
    onMultiBookmarkClicked: (List<Episode>, Boolean) -> Unit,
    onMultiFillermarkClicked: (List<Episode>, Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Episode>, Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,
    onEpisodeSwipe: (EpisodeList.Item, LibraryPreferences.EpisodeSwipeAction) -> Unit,
    onEpisodeSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onAllEpisodeSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    // SY -->
    onEditInfoClicked: () -> Unit,
    onMergeClicked: () -> Unit,
    onOpenFolder: () -> Unit,
    onClearAnime: () -> Unit,
    onSourceSettings: () -> Unit,
    onCastClicked: () -> Unit,
    onRecommendationClicked: (Anime) -> Unit,
    // SY <--
    onSettingsClicked: (() -> Unit)? = null,
) {
    val episodeListState = rememberLazyListState()
    val episodes = remember(state.episodes) { state.episodes }
    val isAnySelected = remember(episodes) { episodes.fastAny { it.selected } }
    val selectedEpisodeCount = remember(episodes) { episodes.count { it.selected } }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        bottomBar = {
            val selectedEpisodes = remember(episodes) {
                episodes.filter { it.selected }.map { 
                    EpisodeList.Item(it.episode, it.downloadState, it.downloadProgress, null, it.selected)
                }
            }
            AnimeBottomActionMenu(
                visible = isAnySelected,
                onBookmarkClicked = { onMultiBookmarkClicked(selectedEpisodes.map { it.episode }, true) }.takeIf { selectedEpisodes.fastAny { !it.episode.bookmark } },
                onRemoveBookmarkClicked = { onMultiBookmarkClicked(selectedEpisodes.map { it.episode }, false) }.takeIf { selectedEpisodes.fastAll { it.episode.bookmark } },
                onFillermarkClicked = { onMultiFillermarkClicked(selectedEpisodes.map { it.episode }, true) }.takeIf { selectedEpisodes.fastAny { !it.episode.fillermark } },
                onRemoveFillermarkClicked = { onMultiFillermarkClicked(selectedEpisodes.map { it.episode }, false) }.takeIf { selectedEpisodes.fastAll { it.episode.fillermark } },
                onMarkAsSeenClicked = { onMultiMarkAsSeenClicked(selectedEpisodes.map { it.episode }, true) }.takeIf { selectedEpisodes.fastAny { !it.episode.seen } },
                onMarkAsUnseenClicked = { onMultiMarkAsSeenClicked(selectedEpisodes.map { it.episode }, false) }.takeIf { selectedEpisodes.fastAny { it.episode.seen } },
                onMarkPreviousAsSeenClicked = { onMarkPreviousAsSeenClicked(selectedEpisodes.first().episode) }.takeIf { selectedEpisodes.size == 1 },
                onDownloadClicked = { onDownloadEpisode(selectedEpisodes, EpisodeDownloadAction.START) }.takeIf { selectedEpisodes.fastAny { it.downloadState != Download.State.DOWNLOADED } },
                onDeleteClicked = { onMultiDeleteClicked(selectedEpisodes.map { it.episode }) },
                onExternalClicked = { onEpisodeClicked(selectedEpisodes.first().episode, true) }.takeIf { selectedEpisodes.size == 1 },
                onInternalClicked = { onEpisodeClicked(selectedEpisodes.first().episode, false) }.takeIf { selectedEpisodes.size == 1 },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            val isFABVisible = remember(episodes) {
                episodes.fastAny { !it.episode.seen } && !isAnySelected
            }
            AnimatedVisibility(
                visible = isFABVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    text = {
                        val isWatching = remember(state.episodes) {
                            state.episodes.fastAny { it.episode.seen }
                        }
                        Text(
                            text = stringResource(
                                if (isWatching) MR.strings.action_resume else MR.strings.action_start,
                            ),
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                        )
                    },
                    onClick = onContinueWatching,
                    expanded = episodeListState.shouldExpandFAB(),
                )
            }
        },
    ) { contentPadding ->
        val topPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

        Box {
            PullRefresh(
                refreshing = state.isRefreshing,
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
                        item(
                            key = "info_box",
                        ) {
                            AnimeInfoBox(
                                isTabletUi = false,
                                appBarPadding = topPadding,
                                anime = state.anime,
                                sourceName = remember { state.source.getNameForAnimeInfo() },
                                isStubSource = state.source is StubSource,
                                onCoverClick = onCoverClicked,
                                doSearch = onSearch,
                            )
                        }

                        item(
                            key = "action_row",
                        ) {
                            AnimeActionRow(
                                favorite = state.anime.favorite,
                                trackingCount = state.trackingCount,
                                nextUpdate = state.anime.expectedNextUpdate,
                                isUserIntervalMode = state.anime.fetchInterval > 0,
                                onAddToLibraryClicked = onAddToLibraryClicked,
                                onWebViewClicked = onWebViewClicked,
                                onWebViewLongClicked = onWebViewLongClicked,
                                onTrackingClicked = onTrackingClicked,
                                onEditIntervalClicked = onEditFetchIntervalClicked,
                                onEditCategory = onEditCategoryClicked,
                            )
                        }

                        item(
                            key = "description_with_tag",
                        ) {
                            val context = LocalContext.current
                            ExpandableAnimeDescription(
                                defaultExpandState = state.isFromSource,
                                description = state.anime.description,
                                tagsProvider = { state.anime.genre },
                                onTagSearch = onTagSearch,
                                onCopyTagToClipboard = { tag ->
                                    context.copyToClipboard(tag, tag)
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (state.recommendations.isNotEmpty()) {
                            item(key = "recommendations") {
                                RecommendationsComponent(
                                    recommendations = state.recommendations,
                                    onClickAnime = onRecommendationClicked,
                                )
                            }
                        }

                        item(
                            key = "episode_header",
                        ) {
                            val missingEpisodeCount = remember(state.episodeListItems) {
                                state.episodeListItems.filterIsInstance<EpisodeList.MissingCount>().sumOf { it.count }
                            }
                            EpisodeHeader(
                                enabled = !isAnySelected,
                                episodeCount = episodes.size,
                                missingEpisodeCount = missingEpisodeCount,
                                onClick = onFilterButtonClicked,
                            )
                        }

                        if (state.airingTime > 0L) {
                            item(
                                key = "airing_info",
                            ) {
                                NextEpisodeAiringListItem(
                                    title = stringResource(
                                        SYMR.strings.airing_expected_days,
                                        formatTime(state.airingTime, useDayFormat = true),
                                    ),
                                    date = formatTime(state.airingTime, useDayFormat = true),
                                )
                            }
                        }

                        sharedEpisodeItems(
                            this,
                            anime = state.anime,
                            source = state.source,
                            showFileSize = showFileSize,
                            episodes = state.episodeListItems,
                            isAnyEpisodeSelected = isAnySelected,
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

            AnimeToolbar(
                modifier = Modifier.align(Alignment.TopCenter),
                title = state.anime.title,
                titleAlphaProvider = { 
                    if (episodeListState.firstVisibleItemIndex > 0) 1f else 0f 
                },
                hasFilters = state.filterActive,
                onBackClicked = onBackClicked,
                onClickFilter = onFilterButtonClicked,
                onClickShare = onShareClicked,
                onClickDownload = onDownloadActionClicked,
                onClickEditCategory = onEditCategoryClicked,
                onClickRefresh = onRefresh,
                onClickMigrate = onMigrateClicked,
                // SY -->
                onClickMerge = onMergeClicked.takeIf { state.anime.favorite },
                onClickEditInfo = onEditInfoClicked.takeIf { state.anime.favorite },
                onClickOpenFolder = onOpenFolder,
                onClickClearAnime = onClearAnime,
                onClickSourceSettings = onSourceSettings,
                onClickCast = onCastClicked,
                // SY <--
                onClickSettings = onSettingsClicked,
                changeAnimeSkipIntro = changeAnimeSkipIntro,
                actionModeCounter = selectedEpisodeCount,
                onSelectAll = { onAllEpisodeSelected(true) },
                onInvertSelection = { onInvertSelection() },
                backgroundAlphaProvider = {
                    val firstItemVisible = episodeListState.firstVisibleItemIndex > 0
                    if (firstItemVisible) 1f else 0f
                }
            )
        }
    }
}

private fun formatTime(timestamp: Long, useDayFormat: Boolean): String {
    return java.text.DateFormat.getDateTimeInstance().format(java.util.Date(timestamp * 1000L))
}

private fun sharedEpisodeItems(
    scope: LazyListScope,
    anime: Anime,
    source: Source,
    showFileSize: Boolean,
    episodes: List<EpisodeList>,
    isAnyEpisodeSelected: Boolean,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    onEpisodeClicked: (Episode, Boolean) -> Unit,
    onDownloadEpisode: (List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit,
    onEpisodeSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onEpisodeSwipe: (EpisodeList.Item, LibraryPreferences.EpisodeSwipeAction) -> Unit,
) {
    scope.items(
        items = episodes,
        key = { it.id_key },
        contentType = {
            when (it) {
                is EpisodeList.Item -> "episode"
                is EpisodeList.MissingCount -> "missing_count"
            }
        },
    ) { item ->
        when (item) {
            is EpisodeList.Item -> {
                AnimeEpisodeListItem(
                    title = item.episode.name,
                    date = item.episode.dateUpload.takeIf { it > 0 }?.let { formatTime(it / 1000, false) },
                    watchProgress = null,
                    scanlator = item.episode.scanlator,
                    seen = item.episode.seen,
                    bookmark = item.episode.bookmark,
                    fillermark = item.episode.fillermark,
                    selected = item.selected,
                    downloadIndicatorEnabled = true,
                    downloadStateProvider = { item.downloadState },
                    downloadProgressProvider = { item.downloadProgress },
                    onEpisodeSwipe = { onEpisodeSwipe(item, it) },
                    episodeSwipeStartAction = episodeSwipeStartAction,
                    episodeSwipeEndAction = episodeSwipeEndAction,
                    onLongClick = { onEpisodeSelected(item, !item.selected, true, false) },
                    onClick = { onEpisodeClicked(item.episode, false) },
                    onDownloadClick = { onDownloadEpisode(listOf(item), it) },
                    fileSize = null, // Default value if not available
                )
            }
            is EpisodeList.MissingCount -> {
                MissingEpisodeCountListItem(count = item.count)
            }
        }
    }
}

private val EpisodeList.id_key: Any
    get() = when (this) {
        is EpisodeList.Item -> episode.id
        is EpisodeList.MissingCount -> id
    }
