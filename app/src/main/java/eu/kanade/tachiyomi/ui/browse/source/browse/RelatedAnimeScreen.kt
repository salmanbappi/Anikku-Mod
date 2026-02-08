package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.Scaffold
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
        val showHome by sourcePreferences.relatedAnimeShowHome().changes()
            .collectAsState(sourcePreferences.relatedAnimeShowHome().get())

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = "Discover Related",
                    subtitle = state.title,
                    navigateUp = navigator::pop,
                    actions = {
                        if (showHome) {
                            IconButton(onClick = { navigator.popUntil { it is HomeScreen } }) {
                                Icon(
                                    imageVector = Icons.Outlined.Home,
                                    contentDescription = stringResource(MR.strings.label_library),
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
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val columns = remember(configuration.screenWidthDp) {
            when {
                configuration.screenWidthDp >= 1200 -> GridCells.Fixed(6)
                configuration.screenWidthDp >= 840 -> GridCells.Fixed(5)
                configuration.screenWidthDp >= 600 -> GridCells.Fixed(4)
                configuration.screenWidthDp >= 480 -> GridCells.Fixed(3)
                else -> GridCells.Fixed(2)
            }
        }

        LazyVerticalGrid(
            columns = columns,
            contentPadding = contentPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridVerticalSpacer),
            horizontalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridHorizontalSpacer),
            modifier = Modifier.fillMaxSize()
        ) {
            state.items.forEach { (keyword, animes) ->
                item(key = "header-$keyword", span = { GridItemSpan(maxLineSpan) }) {
                    ListGroupHeader(
                        text = keyword.uppercase(),
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    )
                }
                itemsIndexed(
                    items = animes,
                    key = { index: Int, it: tachiyomi.domain.anime.model.Anime -> "anime-$keyword-${it.id}-$index" },
                ) { _: Int, anime: tachiyomi.domain.anime.model.Anime ->
                    BrowseSourceComfortableGridItem(
                        anime = anime,
                        isFavorite = anime.favorite,
                        onClick = { onAnimeClick(anime) },
                    )
                }
            }
        }
    }
}
