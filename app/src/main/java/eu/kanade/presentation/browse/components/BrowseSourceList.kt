package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.library.components.AnimeListItem
import eu.kanade.presentation.library.components.CommonAnimeItemDefaults
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.AnimeCover
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseSourceList(
    animeList: LazyPagingItems<Anime>,
    entries: Int,
    topBarHeight: Int,
    contentPadding: PaddingValues,
    onAnimeClick: (Anime) -> Unit,
    onAnimeLongClick: (Anime) -> Unit,
    selection: List<Anime>,
    favoriteIds: Set<Long> = emptySet(),
    onBatchIncrement: (Int) -> Unit = {},
) {
    var containerHeight by remember { mutableIntStateOf(0) }
    val selectionIds = remember(selection) { selection.map { it.id }.toSet() }
    LazyColumn(
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
        modifier = if (entries > 0) {
            Modifier.onGloballyPositioned { layoutCoordinates ->
                containerHeight = layoutCoordinates.size.height - topBarHeight
            }
        } else {
            Modifier
        },
    ) {
        item {
            if (animeList.loadState.prepend is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }

        items(
            count = animeList.itemCount,
            key = { index -> 
                val anime = animeList.peek(index)
                if (anime != null) "anime-${anime.id}" else "placeholder_$index"
            },
            contentType = { "anime" },
        ) { index ->
            val anime = animeList[index] ?: return@items
            onBatchIncrement(index)
            val isFavorite = remember(anime.id, favoriteIds) { anime.id in favoriteIds }
            BrowseSourceListItem(
                anime = anime,
                isFavorite = isFavorite,
                isSelected = anime.id in selectionIds,
                onClick = { onAnimeClick(anime) },
                onLongClick = { onAnimeLongClick(anime) },
                entries = entries,
                containerHeight = containerHeight,
            )
        }

        item {
            if (animeList.loadState.refresh is LoadState.Loading || animeList.loadState.append is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
internal fun BrowseSourceListItem(
    anime: Anime,
    isFavorite: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
    entries: Int,
    containerHeight: Int,
) {
    AnimeListItem(
        title = anime.title,
        isSelected = isSelected,
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
        badge = {
            InLibraryBadge(enabled = isFavorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
        entries = entries,
        containerHeight = containerHeight,
    )
}
