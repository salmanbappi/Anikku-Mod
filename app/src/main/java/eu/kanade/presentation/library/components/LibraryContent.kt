package eu.kanade.presentation.library.components

import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryAnime
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.presentation.core.components.material.PullRefresh
import kotlin.time.Duration.Companion.seconds

import eu.kanade.domain.ui.ContainerStyle
import eu.kanade.domain.ui.UiPreferences
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun LibraryContent(
    categories: List<Category>,
    searchQuery: String?,
    selection: List<LibraryAnime>,
    contentPadding: PaddingValues,
    currentPage: () -> Int,
    hasActiveFilters: Boolean,
    showPageTabs: Boolean,
    onChangeCurrentPage: (Int) -> Unit,
    onAnimeClicked: (Long) -> Unit,
    onContinueWatchingClicked: ((LibraryAnime) -> Unit)?,
    onToggleSelection: (LibraryAnime) -> Unit,
    onToggleRangeSelection: (LibraryAnime) -> Unit,
    onRefresh: (Category?) -> Boolean,
    onGlobalSearchClicked: () -> Unit,
    getNumberOfAnimeForCategory: (Category) -> Int?,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getAnimeLibraryForPage: (Int) -> List<LibraryItem>,
) {
    val uiPreferences = remember { Injekt.get<UiPreferences>() }
    val containerStyles by uiPreferences.containerStyles().collectAsState()
    val useContainer = remember(containerStyles) { ContainerStyle.LIBRARY in containerStyles }

    Column(
        modifier = Modifier.padding(
            top = contentPadding.calculateTopPadding(),
            start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
            end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
        ),
    ) {
        val pagerState = rememberPagerState(
            initialPage = remember(categories) { currentPage().coerceAtMost(categories.lastIndex.coerceAtLeast(0)) },
        ) { categories.size }

        val scope = rememberCoroutineScope()
        var isRefreshing by remember(pagerState.currentPage) { mutableStateOf(false) }

        LaunchedEffect(categories.size) {
            if (pagerState.currentPage >= categories.size && categories.isNotEmpty()) {
                pagerState.scrollToPage(categories.lastIndex)
            }
        }

        if (showPageTabs && categories.size > 1) {
            LibraryTabs(
                categories = categories,
                pagerState = pagerState,
                getNumberOfItemsForCategory = getNumberOfAnimeForCategory,
            ) { scope.launch { pagerState.animateScrollToPage(it) } }
        }

        val notSelectionMode = selection.isEmpty()
        val onClickAnime = { anime: LibraryAnime ->
            if (notSelectionMode) {
                onAnimeClicked(anime.anime.id)
            } else {
                onToggleSelection(anime)
            }
        }

        PullRefresh(
            modifier = Modifier.fillMaxSize(),
            refreshing = isRefreshing,
            onRefresh = {
                val started = onRefresh(categories[currentPage()])
                if (!started) return@PullRefresh
                scope.launch {
                    // Fake refresh status but hide it after a second as it's a long running task
                    isRefreshing = true
                    delay(1.seconds)
                    isRefreshing = false
                }
            },
            enabled = notSelectionMode,
        ) {
            val pagerContent = @Composable {
                LibraryPager(
                    state = pagerState,
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                    hasActiveFilters = hasActiveFilters,
                    selectedAnime = selection,
                    searchQuery = searchQuery,
                    onGlobalSearchClicked = onGlobalSearchClicked,
                    getDisplayMode = getDisplayMode,
                    getColumnsForOrientation = getColumnsForOrientation,
                    getLibraryForPage = getAnimeLibraryForPage,
                    onClickAnime = onClickAnime,
                    onLongClickAnime = onToggleRangeSelection,
                    onClickContinueWatching = onContinueWatchingClicked,
                )
            }

            if (useContainer) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 2.dp
                ) {
                    pagerContent()
                }
            } else {
                pagerContent()
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            onChangeCurrentPage(pagerState.currentPage)
        }
    }
}
