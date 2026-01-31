package eu.kanade.tachiyomi.ui.library.errors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.anime.components.BaseAnimeListItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

class LibraryUpdateErrorsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { LibraryUpdateErrorsScreenModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(SYMR.strings.library_errors),
                    navigateUp = navigator::pop,
                    actions = {
                        AppBarActions(
                            persistentListOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_delete),
                                    icon = Icons.Outlined.Delete,
                                    onClick = screenModel::deleteErrors,
                                ),
                            ),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            if (state.isEmpty()) {
                EmptyScreen(
                    modifier = Modifier.padding(paddingValues),
                    stringRes = SYMR.strings.info_empty_library_update_errors,
                )
            } else {
                ScrollbarLazyColumn(
                    contentPadding = paddingValues,
                ) {
                    items(state) { item ->
                        LibraryUpdateErrorItem(
                            item = item,
                            onClick = { navigator.push(AnimeScreen(item.error.animeId)) },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LibraryUpdateErrorItem(
        item: LibraryUpdateErrorUiItem,
        onClick: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BaseAnimeListItem(
                anime = item.error.animeCover.let { cover ->
                    // Construct a dummy anime for the cover component
                    // In a real scenario, you'd fetch the actual anime object
                    // but for errors, the title and cover might be enough.
                    tachiyomi.domain.anime.model.Anime(
                        id = item.error.animeId,
                        source = item.error.animeSource,
                        favorite = true,
                        lastUpdate = 0,
                        nextUpdate = 0,
                        fetchInterval = 0,
                        dateAdded = 0,
                        viewerFlags = 0,
                        chapterFlags = 0,
                        coverLastModified = item.error.animeCover.lastModified ?: 0,
                        url = "",
                        ogTitle = item.error.animeTitle,
                        ogArtist = null,
                        ogAuthor = null,
                        ogDescription = null,
                        ogGenre = null,
                        ogStatus = 0,
                        thumbnailUrl = item.error.animeCover.ogUrl,
                        initialized = true,
                    )
                },
                onClickItem = onClick,
                content = {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                    ) {
                        Text(
                            text = item.error.animeTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
            )
        }
    }
}
