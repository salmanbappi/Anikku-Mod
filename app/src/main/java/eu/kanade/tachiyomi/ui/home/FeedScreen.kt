package eu.kanade.tachiyomi.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.anime.components.AnimeCover
import eu.kanade.presentation.components.AppBar
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun FeedScreen(
    screenModel: FeedScreenModel,
    onAnimeClick: (Anime) -> Unit,
) {
    val state by screenModel.state.collectAsState()

    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(SYMR.strings.feed),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(paddingValues))
            state.items.isEmpty() -> EmptyScreen(
                stringRes = SYMR.strings.feed_tab_empty,
                modifier = Modifier.padding(paddingValues),
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = paddingValues.calculateTopPadding() + 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.items) { item ->
                        FeedIsland(
                            title = item.savedSearch?.name ?: item.source.name,
                            animeList = item.animeList,
                            onAnimeClick = onAnimeClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedIsland(
    title: String,
    animeList: List<Anime>,
    onAnimeClick: (Anime) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(animeList) { anime ->
                    FeedCard(
                        anime = anime,
                        onClick = { onAnimeClick(anime) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedCard(
    anime: Anime,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(100.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AnimeCover.Book(
            data = anime,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = anime.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(32.dp)
        )
    }
}
