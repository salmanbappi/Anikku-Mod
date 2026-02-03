package eu.kanade.tachiyomi.ui.anime

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.presentation.track.TrackScoreSelector
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.domain.anime.model.Anime
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun LocalScoreDialog(
    anime: Anime,
    onDismissRequest: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    val scores = List(11) { it.toString() }.toImmutableList()
    var selectedScore by remember { mutableStateOf(anime.score?.toInt()?.toString() ?: "0") }
    
    TrackScoreSelector(
        selection = selectedScore,
        onSelectionChange = { selectedScore = it },
        selections = scores,
        onConfirm = {
            onConfirm(selectedScore.toDouble())
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
    )
}
