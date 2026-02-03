package eu.kanade.presentation.more.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.anime.components.MarkdownRender
import eu.kanade.presentation.more.stats.components.StatsItem
import eu.kanade.presentation.more.stats.components.StatsOverviewItem
import eu.kanade.presentation.more.stats.data.StatsData
import tachiyomi.presentation.core.util.secondaryItemAlpha
import eu.kanade.presentation.util.toDurationString
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.ElevatedCard
import coil3.compose.AsyncImage
import eu.kanade.domain.ai.AiPreferences
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun StatsScreenContent(
    state: StatsScreenState.SuccessAnime,
    paddingValues: PaddingValues,
) {
    val statListState = rememberLazyListState()
    LazyColumn(
        state = statListState,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
    ) {
        item {
            ProfileHeaderSection(state)
        }

        if (state.aiAnalysis != null) {
            item {
                AiIntelligenceSection(state.aiAnalysis)
            }
        }

        item {
            GenreRadarSection(state.genreAffinity)
        }

        item {
            OverviewGridSection(state)
        }

        item {
            StatusBreakdownSection(state.statuses)
        }

        item {
            ScoreDistributionSection(state.scores)
        }

        item {
            GenreAffinitySection(state.genreAffinity)
        }

        item {
            ExtensionUsageSection(state.extensions)
        }

        item {
            WatchHabitsSection(state.watchHabits)
        }
    }
}

@Composable
private fun StatsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Text(
        modifier = Modifier.padding(horizontal = MaterialTheme.padding.extraLarge),
        text = title,
        style = MaterialTheme.typography.titleSmall,
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.padding.medium)) {
            content()
        }
    }
}

@Composable
private fun GenreRadarSection(genreAffinity: StatsData.GenreAffinity) {
    if (genreAffinity.genreScores.size < 3) return
    
    StatsSectionCard(title = "Neural Affinity Map") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(MaterialTheme.padding.medium),
            contentAlignment = Alignment.Center
        ) {
            val labels = genreAffinity.genreScores.take(6).map { it.first }
            val values = genreAffinity.genreScores.take(6).map { it.second.toFloat() }
            val max = values.maxOrNull() ?: 1f
            
            RadarChart(
                labels = labels,
                data = values.map { it / max },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun RadarChart(
    labels: List<String>,
    data: List<Float>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * 0.7f
        val angleStep = (2 * Math.PI / labels.size).toFloat()

        // Draw Web Grid
        for (i in 1..4) {
            val currentRadius = radius * (i / 4f)
            val path = Path()
            for (j in labels.indices) {
                val angle = j * angleStep - Math.PI.toFloat() / 2
                val x = center.x + currentRadius * cos(angle)
                val y = center.y + currentRadius * sin(angle)
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, gridColor, style = Stroke(width = 1.dp.toPx()))
        }

        // Draw Axes and Labels
        labels.forEachIndexed { i, label ->
            val angle = i * angleStep - Math.PI.toFloat() / 2
            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)
            drawLine(gridColor, center, Offset(x, y), strokeWidth = 1.dp.toPx())

            // Draw Label
            val labelRadius = radius + 20.dp.toPx()
            val lx = center.x + labelRadius * cos(angle)
            val ly = center.y + labelRadius * sin(angle)
            
            drawContext.canvas.nativeCanvas.drawText(
                label.take(10),
                lx,
                ly,
                android.graphics.Paint().apply {
                    color = textColor.toArgb()
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }

        // Draw Data Path
        val dataPath = Path()
        data.forEachIndexed { i, value ->
            val angle = i * angleStep - Math.PI.toFloat() / 2
            val currentRadius = radius * value
            val x = center.x + currentRadius * cos(angle)
            val y = center.y + currentRadius * sin(angle)
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        
        drawPath(dataPath, primaryColor.copy(alpha = 0.3f))
        drawPath(dataPath, primaryColor, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun ProfileHeaderSection(state: StatsScreenState.SuccessAnime) {
    val aiPreferences = remember { Injekt.get<AiPreferences>() }
    val displayName by aiPreferences.displayName().collectAsState()
    val profilePhotoUri by aiPreferences.profilePhotoUri().collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface,
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (profilePhotoUri.isNotEmpty()) {
                    AsyncImage(
                        model = profilePhotoUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.LocalLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${state.overview.libraryAnimeCount} Titles in DNA",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.secondaryItemAlpha()
            )
        }
    }
}

@Composable
private fun AiIntelligenceSection(analysis: String) {
    var expanded by remember { mutableStateOf(false) }
    StatsSectionCard(
        title = "Neural Intelligence Report",
        modifier = Modifier.clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.medium)
                .animateContentSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (expanded) "Full Report" else "Tap to read intelligence insight",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                MarkdownRender(content = analysis)
            }
        }
    }
}

@Composable
private fun OverviewGridSection(state: StatsScreenState.SuccessAnime) {
    val context = LocalContext.current
    val watchTime = state.overview.totalSeenDuration
        .toDuration(DurationUnit.MILLISECONDS)
        .toDurationString(context, fallback = "0m")

    StatsSectionCard(title = "Core Metrics") {
        Column(modifier = Modifier.padding(MaterialTheme.padding.medium)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(Icons.Outlined.Schedule, "Watch Time", watchTime)
                MetricItem(Icons.Outlined.Star, "Mean Score", "%.2f".format(state.trackers.meanScore))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).alpha(0.5f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(Icons.Outlined.History, "Episodes", state.episodes.readEpisodeCount.toString())
                MetricItem(Icons.Outlined.Extension, "Sources", state.trackers.trackerCount.toString())
            }
        }
    }
}

@Composable
private fun MetricItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.secondaryItemAlpha())
        }
    }
}

@Composable
private fun GenreAffinitySection(genreAffinity: StatsData.GenreAffinity) {
    StatsSectionCard(title = "Genre Signature") {
        Column(modifier = Modifier.padding(MaterialTheme.padding.medium)) {
            val maxCount = genreAffinity.genreScores.firstOrNull()?.second ?: 1
            genreAffinity.genreScores.take(5).forEach { (genre, count) ->
                GenreBar(genre, count, maxCount)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GenreBar(genre: String, count: Int, maxCount: Int) {
    val progress = count.toFloat() / maxCount
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = genre, style = MaterialTheme.typography.bodySmall)
            Text(text = count.toString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.secondaryItemAlpha())
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun ExtensionUsageSection(extensions: StatsData.ExtensionUsage) {
    StatsSectionCard(title = "Primary Gateways") {
        Column(modifier = Modifier.padding(MaterialTheme.padding.medium)) {
            extensions.topExtensions.forEachIndexed { index, (name, count) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(24.dp)
                    )
                    Text(text = name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(text = "$count titles", style = MaterialTheme.typography.bodySmall, modifier = Modifier.secondaryItemAlpha())
                }
            }
        }
    }
}

@Composable
private fun WatchHabitsSection(habits: StatsData.WatchHabits) {
    StatsSectionCard(title = "Neural Patterns") {
        Column(modifier = Modifier.padding(MaterialTheme.padding.medium), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HabitItem("Preferred Cycle", habits.preferredWatchTime)
            if (habits.topDayAnime != null) {
                HabitItem("Peak Focus (24h)", habits.topDayAnime)
            }
            if (habits.topMonthAnime != null) {
                HabitItem("Dominant Series (30d)", habits.topMonthAnime)
            }
        }
    }
}

@Composable
private fun HabitItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusBreakdownSection(statuses: StatsData.StatusBreakdown) {
    StatsSectionCard(title = stringResource(MR.strings.label_system_equilibrium)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val data = listOf(
                statuses.completedCount.toFloat(),
                statuses.ongoingCount.toFloat(),
                statuses.droppedCount.toFloat(),
                statuses.onHoldCount.toFloat(),
                statuses.planToWatchCount.toFloat()
            )
            val colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.error,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.outline
            )
            
            PieChart(
                data = data,
                colors = colors,
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.width(24.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusLegendItem(MaterialTheme.colorScheme.primary, "Completed", statuses.completedCount)
                StatusLegendItem(MaterialTheme.colorScheme.secondary, "Ongoing", statuses.ongoingCount)
                StatusLegendItem(MaterialTheme.colorScheme.error, "Dropped", statuses.droppedCount)
                StatusLegendItem(MaterialTheme.colorScheme.tertiary, "On Hold", statuses.onHoldCount)
                StatusLegendItem(MaterialTheme.colorScheme.outline, "Planned", statuses.planToWatchCount)
            }
        }
    }
}

@Composable
private fun StatusLegendItem(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        Text(text = count.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScoreDistributionSection(scores: StatsData.ScoreDistribution) {
    StatsSectionCard(title = stringResource(MR.strings.label_score_resonance)) {
        Column(modifier = Modifier.padding(MaterialTheme.padding.medium)) {
            val maxCount = scores.distribution.values.maxOrNull() ?: 1
            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                for (i in 1..10) {
                    val count = scores.distribution[i] ?: 0
                    val weight = count.toFloat() / maxCount
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = count.toString(), style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(weight.coerceAtLeast(0.05f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(text = i.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(
    data: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = data.sum()
    Canvas(modifier = modifier) {
        var startAngle = -90f
        data.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * 360f
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            startAngle += sweepAngle
        }
        // Hollow center for "Donut" style
        drawCircle(
            color = Color.Black.copy(alpha = 0.1f), // Shadow
            radius = size.minDimension / 2 * 0.6f
        )
    }
}