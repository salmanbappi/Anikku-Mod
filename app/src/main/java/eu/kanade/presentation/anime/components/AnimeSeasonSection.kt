package eu.kanade.presentation.anime.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.library.components.AnimeComfortableGridItem
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.domain.anime.model.Season
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun AnimeSeasonSection(
    seasons: ImmutableList<Season>,
    onSeasonClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (seasons.size <= 1) return

    // Intuitive Sorting: Seasons/Movies first (positive/0/-2), then OVAs/ONAs/Specials
    val sortedSeasons = remember(seasons) {
        seasons.sortedWith(
            compareBy<Season> { 
                when {
                    it.seasonNumber >= 0 -> 0 // Normal seasons
                    it.seasonNumber == -2.0 -> 1 // Movies
                    it.seasonNumber == -3.0 -> 2 // OVA
                    it.seasonNumber == -4.0 -> 3 // ONA
                    else -> 4 // Specials
                }
            }.thenBy { it.seasonNumber }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LibraryBooks,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Series Seasons",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Quickly switch between seasons of this series",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = sortedSeasons,
                    key = { it.anime.id }
                ) { season ->
                    SeasonItem(
                        season = season,
                        onClick = { onSeasonClick(season.anime.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonItem(
    season: Season,
    onClick: () -> Unit,
) {
    val seasonLabel = remember(season.seasonNumber, season.anime.title) {
        val title = season.anime.title.lowercase()
        val isPart = title.contains("part") || title.contains("cour")
        
        when (season.seasonNumber) {
            -2.0 -> "Movie"
            -3.0 -> "OVA"
            -4.0 -> "ONA"
            -5.0 -> "Special"
            else -> {
                if (season.seasonNumber > 0) {
                    val num = if (season.seasonNumber % 1.0 == 0.0) season.seasonNumber.toInt().toString() else season.seasonNumber.toString()
                    // If it's 1.0, only show "Season 1" or "Part 1" if the title actually contains a season marker
                    // otherwise it's just the base title entry.
                    if (season.seasonNumber == 1.0 && !title.contains("season") && !title.contains("part") && !title.contains("cour") && !title.contains(" s1")) {
                         "Main"
                    } else if (isPart) {
                        "Part $num"
                    } else {
                        "Season $num"
                    }
                } else {
                    "Special"
                }
            }
        }
    }

    Column(
        modifier = Modifier.width(104.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AnimeComfortableGridItem(
            title = seasonLabel,
            coverData = remember(season.anime.id) {
                tachiyomi.domain.anime.model.AnimeCover(
                    animeId = season.anime.id,
                    sourceId = season.anime.source,
                    isAnimeFavorite = season.anime.favorite,
                    ogUrl = season.anime.thumbnailUrl,
                    lastModified = season.anime.coverLastModified,
                )
            },
            coverBadgeStart = {
                if (season.isPrimary) {
                    Badge(
                        text = "Current",
                        color = MaterialTheme.colorScheme.secondary,
                        textColor = MaterialTheme.colorScheme.onSecondary
                    )
                }
            },
            onClick = onClick,
            onLongClick = {},
        )
    }
}
