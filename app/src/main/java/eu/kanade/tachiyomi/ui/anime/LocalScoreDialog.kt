package eu.kanade.tachiyomi.ui.anime

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.domain.anime.model.Anime
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.WheelTextPicker
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun LocalScoreDialog(
    anime: Anime,
    onDismissRequest: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    val scores = List(11) { it.toString() }.toImmutableList()
    var selectedScore by remember { mutableStateOf(anime.score?.toInt()?.toString() ?: "0") }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(selectedScore.toDouble())
                onDismissRequest()
            }) {
                Text(stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.score))
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                WheelTextPicker(
                    items = scores,
                    startIndex = scores.indexOf(selectedScore).coerceAtLeast(0),
                    onSelectionChanged = { selectedScore = scores[it] }
                )
            }
        }
    )
}