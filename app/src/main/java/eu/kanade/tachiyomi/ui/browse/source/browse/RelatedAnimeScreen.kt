package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.components.BrowseSourceComfortableGridItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.library.components.CommonAnimeItemDefaults
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RelatedAnimeScreen(val animeId: Long) : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { RelatedAnimeScreenModel(animeId) }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val showHome by sourcePreferences.relatedAnimeShowHome().collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = state.title,
                    navigateUp = navigator::pop,
                    actions = {
                        if (showHome) {
                            IconButton(onClick = { navigator.popUntil { it is HomeScreen } }) {
                                Icon(
                                    imageVector = Icons.Outlined.Home,
                                    contentDescription = stringResource(KMR.strings.action_bar_home),
                                )
                            }
                        }
                    },
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
