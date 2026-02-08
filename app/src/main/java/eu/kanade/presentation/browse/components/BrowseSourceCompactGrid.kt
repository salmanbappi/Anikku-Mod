package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.library.components.AnimeCompactGridItem
import eu.kanade.presentation.library.components.CommonAnimeItemDefaults
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.AnimeCover
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseSourceCompactGrid(
    animeList: LazyPagingItems<Anime>,
    columns: GridCells,
    contentPadding: PaddingValues,
    onAnimeClick: (Anime) -> Unit,
    onAnimeLongClick: (Anime) -> Unit,
    selection: List<Anime>,
    favoriteIds: Set<Long> = emptySet(),
    onBatchIncrement: (Int) -> Unit = {},
) {
    val selectionIds = remember(selection) { selection.map { it.id }.toSet() }
    LazyVerticalGrid(
        columns = columns,
        contentPadding = contentPadding + PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridHorizontalSpacer),
    ) {
        if (animeList.loadState.prepend is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }

        items(
            count = animeList.itemCount,
            key = { index -> 
                val anime = animeList.peek(index)
                if (anime != null) "anime-${anime.id}-$index" else "placeholder_$index"
            },
            contentType = { "anime" },
        ) { index ->
            val anime = animeList[index] ?: return@items
            onBatchIncrement(index)
            val isFavorite = remember(anime.id, favoriteIds) { anime.id in favoriteIds }
            BrowseSourceCompactGridItem(
                anime = anime,
                isFavorite = isFavorite,
                isSelected = anime.id in selectionIds,
                onClick = { onAnimeClick(anime) },
                onLongClick = { onAnimeLongClick(anime) },
            )
        }

        if (animeList.loadState.refresh is LoadState.Loading || animeList.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
internal fun BrowseSourceCompactGridItem(
    anime: Anime,
    isFavorite: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
) {
    AnimeCompactGridItem(
        title = anime.title,
        coverData = remember(anime.id, isFavorite) {
            AnimeCover(
                animeId = anime.id,
                sourceId = anime.source,
                isAnimeFavorite = isFavorite,
                ogUrl = anime.thumbnailUrl,
                lastModified = anime.coverLastModified,
            )
        },
        coverAlpha = if (isFavorite) CommonAnimeItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = {
            InLibraryBadge(enabled = isFavorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
        isSelected = isSelected,
    )
}
