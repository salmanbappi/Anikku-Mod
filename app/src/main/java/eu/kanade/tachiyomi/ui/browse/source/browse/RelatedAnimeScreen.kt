package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.components.BrowseSourceComfortableGridItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.library.components.CommonAnimeItemDefaults
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.util.plus

class RelatedAnimeScreen(val animeId: Long) : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { RelatedAnimeScreenModel(animeId) }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = state.title,
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            RelatedAnimeContent(
                state = state,
                contentPadding = paddingValues,
                onAnimeClick = { navigator.push(AnimeScreen(it.id)) },
            )
        }
    }

    @Composable
    private fun RelatedAnimeContent(
        state: RelatedAnimeScreenModel.State,
        contentPadding: PaddingValues,
        onAnimeClick: (tachiyomi.domain.anime.model.Anime) -> Unit,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(128.dp),
            contentPadding = contentPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridVerticalSpacer),
            horizontalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridHorizontalSpacer),
        ) {
            state.items.forEach { (keyword, animes) ->
                item(key = "header-$keyword", span = { GridItemSpan(maxLineSpan) }) {
                    ListGroupHeader(
                        text = keyword,
                        modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                    )
                }
                items(animes, key = { "anime-${it.id}" }) { anime ->
                    BrowseSourceComfortableGridItem(
                        anime = anime,
                        onClick = { onAnimeClick(anime) },
                    )
                }
            }
        }
    }
}
