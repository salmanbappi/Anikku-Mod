package eu.kanade.presentation.anime.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun AiGlossaryDialog(
    onDismissRequest: () -> Unit,
    onFetch: (String) -> Unit,
    glossaryInfo: String?,
    isFetching: Boolean,
) {
    var query by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    if (query.isNotBlank()) {
                        hasSearched = true
                        onFetch(query)
                    }
                },
                enabled = query.isNotBlank() && !isFetching,
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = "AI Guide")
        },
        text = {
            Column {
                Text(
                    text = "Ask AI about cultural terms, character relationships, or story context.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.secondaryItemAlpha(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(text = "What do you want to know?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isFetching,
                )
                
                if (hasSearched) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .height(200.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (isFetching) {
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        } else if (glossaryInfo != null) {
                            Text(
                                text = glossaryInfo,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text(
                                text = "AI couldn't find any information for this query.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.secondaryItemAlpha(),
                            )
                        }
                    }
                }
            }
        },
    )
}