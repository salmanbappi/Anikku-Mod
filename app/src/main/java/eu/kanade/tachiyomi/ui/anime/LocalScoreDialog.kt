package eu.kanade.tachiyomi.ui.anime

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    
    TrackScoreSelector(
        selection = anime.score?.toInt()?.toString() ?: "0",
        onSelectionChange = {
            onConfirm(it.toDouble())
            onDismissRequest()
        },
        selections = scores,
        onConfirm = { },
        onDismissRequest = onDismissRequest,
    )
}
