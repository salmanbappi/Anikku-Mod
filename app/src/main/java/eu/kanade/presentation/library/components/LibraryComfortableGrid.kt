package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastAny
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.anime.model.AnimeCover
import tachiyomi.domain.library.model.LibraryAnime

@Composable
internal fun LibraryComfortableGrid(
    items: List<LibraryItem>,
    columns: Int,
    contentPadding: PaddingValues,
    selection: List<LibraryAnime>,
    onClick: (LibraryAnime) -> Unit,
    onLongClick: (LibraryAnime) -> Unit,
    onClickContinueWatching: ((LibraryAnime) -> Unit)?,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
) {
    val selectedIds = remember(selection) { selection.map { it.id }.toSet() }

    LazyLibraryGrid(
        modifier = Modifier.fillMaxSize(),
        columns = columns,
        contentPadding = contentPadding,
    ) {
        globalSearchItem(searchQuery, onGlobalSearchClicked)

        itemsIndexed(
            items = items,
            key = { index: Int, it: eu.kanade.tachiyomi.ui.library.LibraryItem -> "anime-${it.libraryAnime.anime.id}-$index" },
            contentType = { _: Int, _: eu.kanade.tachiyomi.ui.library.LibraryItem -> "anime_library_comfortable_grid_item" },
        ) { _: Int, libraryItem: eu.kanade.tachiyomi.ui.library.LibraryItem ->
            val anime = libraryItem.libraryAnime.anime
            AnimeComfortableGridItem(
                isSelected = libraryItem.libraryAnime.id in selectedIds,
                title = anime.title,
                coverData = AnimeCover(
                    animeId = anime.id,
                    sourceId = anime.source,
                    isAnimeFavorite = anime.favorite,
                    ogUrl = anime.thumbnailUrl,
                    lastModified = anime.coverLastModified,
                ),
                coverBadgeStart = {
                    DownloadsBadge(count = libraryItem.downloadCount)
                    UnviewedBadge(count = libraryItem.unseenCount)
                },
                coverBadgeEnd = {
                    LanguageBadge(
                        isLocal = libraryItem.isLocal,
                        sourceLanguage = libraryItem.sourceLanguage,
                    )
                },
                onLongClick = { onLongClick(libraryItem.libraryAnime) },
                onClick = { onClick(libraryItem.libraryAnime) },
                onClickContinueWatching = if (onClickContinueWatching != null && libraryItem.unseenCount > 0) {
                    { onClickContinueWatching(libraryItem.libraryAnime) }
                } else {
                    null
                },
            )
        }
    }
}
