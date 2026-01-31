package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    // SY -->
    isPanorama: Boolean = false,
    showLanguageIcon: Boolean = false,
    showSourceIcon: Boolean = false,
    // SY <--
) {
    val selectedIds = remember(selection) { selection.map { it.id }.toSet() }

    LazyLibraryGrid(
        modifier = Modifier.fillMaxSize(),
        columns = columns,
        contentPadding = contentPadding,
    ) {
        globalSearchItem(searchQuery, onGlobalSearchClicked)

        items(
            items = items,
            contentType = { "anime_library_comfortable_grid_item" },
        ) { libraryItem ->
            val anime = libraryItem.libraryAnime.anime
            val commonProps = AnimeComfortableGridItemProps(
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
                languageIconBadge = {
                    if (showLanguageIcon) LanguageIconBadge(sourceLanguage = libraryItem.sourceLanguage)
                },
                sourceIconBadge = {
                    if (showSourceIcon) SourceIconBadge(source = libraryItem.source)
                },
                onLongClick = { onLongClick(libraryItem.libraryAnime) },
                onClick = { onClick(libraryItem.libraryAnime) },
                onClickContinueWatching = if (onClickContinueWatching != null && libraryItem.unseenCount > 0) {
                    { onClickContinueWatching(libraryItem.libraryAnime) }
                } else {
                    null
                },
            )

            if (isPanorama) {
                AnimePanoramaGridItem(
                    isSelected = commonProps.isSelected,
                    title = commonProps.title,
                    coverData = commonProps.coverData,
                    coverBadgeStart = { commonProps.coverBadgeStart() },
                    coverBadgeEnd = { commonProps.coverBadgeEnd() },
                    languageIconBadge = { commonProps.languageIconBadge(this) },
                    sourceIconBadge = { commonProps.sourceIconBadge(this) },
                    onLongClick = commonProps.onLongClick,
                    onClick = commonProps.onClick,
                    onClickContinueWatching = commonProps.onClickContinueWatching,
                )
            } else {
                AnimeComfortableGridItem(
                    isSelected = commonProps.isSelected,
                    title = commonProps.title,
                    coverData = commonProps.coverData,
                    coverBadgeStart = { commonProps.coverBadgeStart() },
                    coverBadgeEnd = { commonProps.coverBadgeEnd() },
                    languageIconBadge = { commonProps.languageIconBadge(this) },
                    sourceIconBadge = { commonProps.sourceIconBadge(this) },
                    onLongClick = commonProps.onLongClick,
                    onClick = commonProps.onClick,
                    onClickContinueWatching = commonProps.onClickContinueWatching,
                )
            }
        }
    }
}

private data class AnimeComfortableGridItemProps(
    val isSelected: Boolean,
    val title: String,
    val coverData: AnimeCover,
    val coverBadgeStart: @Composable RowScope.() -> Unit,
    val coverBadgeEnd: @Composable RowScope.() -> Unit,
    val languageIconBadge: @Composable BoxScope.() -> Unit,
    val sourceIconBadge: @Composable BoxScope.() -> Unit,
    val onLongClick: () -> Unit,
    val onClick: () -> Unit,
    val onClickContinueWatching: (() -> Unit)?,
)
