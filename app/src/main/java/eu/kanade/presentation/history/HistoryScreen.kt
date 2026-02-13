package eu.kanade.presentation.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.history.components.HistoryItem
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import java.time.LocalDate

import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp

@Composable
fun HistoryScreen(
    state: HistoryScreenModel.State,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String?) -> Unit,
    onClickCover: (animeId: Long) -> Unit,
    onClickResume: (animeId: Long, episodeId: Long) -> Unit,
    onDialogChange: (HistoryScreenModel.Dialog?) -> Unit,
    navigateUp: (() -> Unit)?,
    searchQuery: String? = null,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            SearchToolbar(
                titleContent = { AppBarTitle(stringResource(MR.strings.history)) },
                searchQuery = state.searchQuery,
                onChangeSearchQuery = onSearchQueryChange,
                actions = {
                    AppBarActions(
                        // KMK -->
                        persistentListOf<AppBar.AppBarAction>().builder()
                            .apply {
                                add(
                                    // KMK <--
                                    AppBar.Action(
                                        title = stringResource(MR.strings.pref_clear_history),
                                        icon = Icons.Outlined.DeleteSweep,
                                        onClick = {
                                            onDialogChange(HistoryScreenModel.Dialog.DeleteAll)
                                        },
                                    ),
                                    // KMK -->
                                )
                            }
                            .build(),
                        // KMK <--
                    )
                },
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        state.list.let {
            if (it == null) {
                LoadingScreen(Modifier.padding(contentPadding))
            } else if (it.isEmpty()) {
                val msg = if (!searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.information_no_recent_anime
                }
                EmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                HistoryScreenContent(
                    history = it,
                    contentPadding = contentPadding,
                    onClickCover = { history -> onClickCover(history.animeId) },
                    onClickResume = { history -> onClickResume(history.animeId, history.episodeId) },
                    onClickDelete = { item -> onDialogChange(HistoryScreenModel.Dialog.Delete(item)) },
                )
            }
        }
    }
}

@Composable
private fun HistoryScreenContent(
    history: List<HistoryUiModel>,
    contentPadding: PaddingValues,
    onClickCover: (HistoryWithRelations) -> Unit,
    onClickResume: (HistoryWithRelations) -> Unit,
    onClickDelete: (HistoryWithRelations) -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = history.filterIsInstance<HistoryUiModel.Item>(),
            key = { "history-${it.item.id}" },
        ) { model ->
            HistoryItem(
                modifier = Modifier.animateItemFastScroll(),
                history = model.item,
                onClickCover = { onClickCover(model.item) },
                onClickResume = { onClickResume(model.item) },
                onClickDelete = { onClickDelete(model.item) },
            )
        }
    }
}

sealed interface HistoryUiModel {
    data class Header(val date: LocalDate) : HistoryUiModel
    data class Item(val item: HistoryWithRelations) : HistoryUiModel
}

@PreviewLightDark
@Composable
internal fun HistoryScreenPreviews(
    @PreviewParameter(HistoryScreenModelStateProvider::class)
    historyState: HistoryScreenModel.State,
) {
    TachiyomiPreviewTheme {
        HistoryScreen(
            state = historyState,
            snackbarHostState = SnackbarHostState(),
            searchQuery = null,
            onSearchQueryChange = {},
            onClickCover = {},
            onClickResume = { _, _ -> run {} },
            onDialogChange = {},
            navigateUp = {},
        )
    }
}
