package eu.kanade.presentation.anime.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.BadgeGroup
import eu.kanade.presentation.library.components.AnimeComfortableGridItem
import tachiyomi.domain.anime.model.Anime
import tachiyomi.presentation.core.components.material.padding

@Composable
fun RecommendationsComponent(
    recommendations: Map<String, List<Anime>>,
    onClickAnime: (Anime) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recommendations.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        recommendations.forEach { (title, animes) ->
            if (animes.isEmpty()) return@forEach

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(animes) { anime ->
                        RecommendationItem(
                            anime = anime,
                            onClick = { onClickAnime(anime) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationItem(
    anime: Anime,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AnimeComfortableGridItem(
            anime = anime,
            onClick = onClick,
            onLongClick = onClick,
            isSelected = false,
            titleAlpha = 1f,
        )
    }
}
