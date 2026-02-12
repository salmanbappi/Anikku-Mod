package eu.kanade.presentation.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.category.components.CategoryFloatingActionButton
import eu.kanade.presentation.category.components.CategoryListItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.tachiyomi.ui.category.CategoryScreenState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import sh.calvin.reorderable.*
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun CategoryScreen(
    state: CategoryScreenState.Success,
    onClickCreate: () -> Unit,
    onClickSortAlphabetically: () -> Unit,
    onClickRename: (Category) -> Unit,
    onClickDelete: (Category) -> Unit,
    onReorder: (List<Category>) -> Unit,
    onClickHide: (Category) -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.action_edit_categories),
                navigateUp = navigateUp,
                actions = {
                    AppBarActions(
                        persistentListOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_sort),
                                icon = Icons.Outlined.SortByAlpha,
                                onClick = onClickSortAlphabetically,
                            ),
                        ),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            CategoryFloatingActionButton(
                lazyListState = lazyListState,
                onCreate = onClickCreate,
            )
        },
    ) { paddingValues ->
        if (state.isEmpty) {
            EmptyScreen(
                stringRes = MR.strings.information_empty_category,
                modifier = Modifier.padding(paddingValues),
            )
            return@Scaffold
        }

        var categories by remember(state.categories) { mutableStateOf(state.categories) }
        val haptic = LocalHapticFeedback.current

        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            categories = categories.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }.toImmutableList()
            onReorder(categories)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .reorderable(reorderableState),
            state = lazyListState,
            contentPadding = paddingValues + topSmallPaddingValues + PaddingValues(
                horizontal = MaterialTheme.padding.medium,
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            items(
                items = categories,
                key = { category -> "category-${category.id}" },
            ) { category ->
                ReorderableItem(
                    state = reorderableState,
                    key = "category-${category.id}",
                ) { isDragging ->
                    CategoryListItem(
                        modifier = Modifier.animateItem(),
                        category = category,
                        onRename = { onClickRename(category) },
                        onDelete = { onClickDelete(category) },
                        onHide = { onClickHide(category) },
                        dragHandle = {
                            Icon(
                                imageVector = Icons.Outlined.DragHandle,
                                contentDescription = null,
                                modifier = Modifier.draggableHandle()
                            )
                        },
                    )
                }
            }
        }
    }
}
