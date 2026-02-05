package eu.kanade.tachiyomi.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class FeedManageScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { FeedManageScreenModel() }
        val state by screenModel.state.collectAsState()

        var deleteDialogItem by remember { mutableStateOf<FeedSavedSearch?>(null) }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = "Manage Feed",
                    navigateUp = { navigator.pop() },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            if (state.items.isEmpty()) {
                EmptyScreen(
                    stringRes = MR.strings.information_empty_category, // reuse for now
                    modifier = Modifier.padding(paddingValues),
                )
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues,
            ) {
                itemsIndexed(
                    items = state.items,
                    key = { _, item -> "feed-${item.feed.id}" },
                ) { index, item ->
                    FeedManageItem(
                        title = item.title,
                        canMoveUp = index != 0,
                        canMoveDown = index != state.items.lastIndex,
                        onMoveUp = { screenModel.moveUp(item.feed) },
                        onMoveDown = { screenModel.moveDown(item.feed) },
                        onDelete = { deleteDialogItem = item.feed },
                        modifier = Modifier.animateItem(),
                    )
                    if (index != state.items.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }

        if (deleteDialogItem != null) {
            val feed = deleteDialogItem!!
            AlertDialog(
                onDismissRequest = { deleteDialogItem = null },
                title = { Text(text = "Delete feed item?") },
                text = { Text(text = "Are you sure you want to remove this from your feed?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            screenModel.delete(feed)
                            deleteDialogItem = null
                        },
                    ) {
                        Text(text = stringResource(MR.strings.action_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteDialogItem = null }) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun FeedManageItem(
    title: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
            ) {
                Icon(imageVector = Icons.Outlined.ArrowDropUp, contentDescription = null)
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
            ) {
                Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                )
            }
        }
    }
}
